/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Build;
import android.os.IBinder;
import android.os.LocaleList;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.InputMethodSubtypeCompatUtils;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.settings.Settings;
import rkr.simplekeyboard.inputmethod.latin.utils.AdditionalSubtypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.IS_ADDITIONAL_SUBTYPE;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.KEYBOARD_MODE;

/**
 * Enrichment class for InputMethodManager to simplify interaction and add functionality.
 */
// non final for easy mocking.
public class RichInputMethodManager {
    private static final String TAG = RichInputMethodManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String SUBTYPE_META_DATA_NAME = "android.view.im";
    private static final String XML_NAMESPACE = "http://schemas.android.com/apk/res/android";
    private static final String TAG_SUBTYPE = "subtype";
    private static final String TAG_ICON = "icon";
    private static final String TAG_LABEL = "label";
    private static final String TAG_SUBTYPE_ID = "subtypeId";
    private static final String TAG_LOCALE = "imeSubtypeLocale";
    private static final String TAG_MODE = "imeSubtypeMode";
    private static final String TAG_EXTRA_VALUE = "imeSubtypeExtraValue";
    private static final String TAG_ASCII_CAPABLE = "isAsciiCapable";

    private RichInputMethodManager() {
        // This utility class is not publicly instantiable.
    }

    private static final RichInputMethodManager sInstance = new RichInputMethodManager();

    private Context mContext;
    private InputMethodManager mImmService;
    private InputMethodInfoCache mInputMethodInfoCache;
    private VirtualSubtypeManager mVirtualSubtypeManager;

    private static final int INDEX_NOT_FOUND = -1;

    public static RichInputMethodManager getInstance() {
        sInstance.checkInitialized();
        return sInstance;
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private boolean isInitialized() {
        return mImmService != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new RuntimeException(TAG + " is used before initialization");
        }
    }

    private void initInternal(final Context context) {
        if (isInitialized()) {
            return;
        }
        mImmService = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mContext = context;
        mInputMethodInfoCache = new InputMethodInfoCache(
                mImmService, context.getPackageName());

        // Initialize additional subtypes.
        SubtypeLocaleUtils.init(context);

        // Initialize the current input method subtype.
        refreshSubtypeCaches();

        mVirtualSubtypeManager = new VirtualSubtypeManager(context);
    }

    public InputMethodManager getInputMethodManager() {
        checkInitialized();
        return mImmService;
    }

    public HashSet<MySubtype> getEnabledSubtypesOfThisIme() {
        return mVirtualSubtypeManager.getAll();
    }

    public boolean addSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.addSubtype(subtype);
    }

    public boolean removeSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.removeSubtype(subtype);
    }

    public void resetSubtypeCycleOrder() {
        mVirtualSubtypeManager.resetSubtypeCycleOrder();
    }

    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (onlyCurrentIme) {
            if (!mVirtualSubtypeManager.hasMultiple()) {
                return false;
            }
            return mVirtualSubtypeManager.switchToNextSubtype(true);
        }
        if (mVirtualSubtypeManager.switchToNextSubtype(false)) {
            return true;
        }
        return switchToNextOtherInputMethod(token);
    }

    private boolean switchToNextOtherInputMethod(final IBinder token) {
        if (mImmService.switchToNextInputMethod(token, false)) {
            return true;
        }
        return switchToNextInputMethodAndSubtype(token);
    }

    private boolean switchToNextInputMethodAndSubtype(final IBinder token) {
        final List<InputMethodInfo> enabledImis = mImmService.getEnabledInputMethodList();
        final int currentIndex = getImiIndexInList(getInputMethodInfoOfThisIme(), enabledImis);
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(TAG, "Can't find current IME in enabled IMEs: IME package="
                    + getInputMethodInfoOfThisIme().getPackageName());
            return false;
        }
        final InputMethodInfo nextImi = getNextNonAuxiliaryIme(currentIndex, enabledImis);
        final List<InputMethodSubtype> enabledSubtypes = getEnabledInputMethodSubtypeList(nextImi,
                true /* allowsImplicitlySelectedSubtypes */);
        if (enabledSubtypes.isEmpty()) {
            // The next IME has no subtype.
            mImmService.setInputMethod(token, nextImi.getId());
            return true;
        }
        final InputMethodSubtype firstSubtype = enabledSubtypes.get(0);
        mImmService.setInputMethodAndSubtype(token, nextImi.getId(), firstSubtype);
        return true;
    }

    private static int getImiIndexInList(final InputMethodInfo inputMethodInfo,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int index = 0; index < count; index++) {
            final InputMethodInfo imi = imiList.get(index);
            if (imi.equals(inputMethodInfo)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    // This method mimics {@link InputMethodManager#switchToNextInputMethod(IBinder,boolean)}.
    private static InputMethodInfo getNextNonAuxiliaryIme(final int currentIndex,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int i = 1; i < count; i++) {
            final int nextIndex = (currentIndex + i) % count;
            final InputMethodInfo nextImi = imiList.get(nextIndex);
            if (!isAuxiliaryIme(nextImi)) {
                return nextImi;
            }
        }
        return imiList.get(currentIndex);
    }

    // Copied from {@link InputMethodInfo}. See how auxiliary of IME is determined.
    private static boolean isAuxiliaryIme(final InputMethodInfo imi) {
        final int count = imi.getSubtypeCount();
        if (count == 0) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(index);
            if (!subtype.isAuxiliary()) {
                return false;
            }
        }
        return true;
    }

    public static class InputMethodInfoCache {
        private final InputMethodManager mImm;
        private final String mImePackageName;

        private InputMethodInfo mCachedThisImeInfo;
        private final HashMap<InputMethodInfo, List<InputMethodSubtype>>
                mCachedSubtypeListWithImplicitlySelected;
        private final HashMap<InputMethodInfo, List<InputMethodSubtype>>
                mCachedSubtypeListOnlyExplicitlySelected;

        public InputMethodInfoCache(final InputMethodManager imm, final String imePackageName) {
            mImm = imm;
            mImePackageName = imePackageName;
            mCachedSubtypeListWithImplicitlySelected = new HashMap<>();
            mCachedSubtypeListOnlyExplicitlySelected = new HashMap<>();
        }

        public synchronized boolean isInputMethodOfThisImeEnabled() {
            for (final InputMethodInfo imi : mImm.getEnabledInputMethodList()) {
                if (imi.getPackageName().equals(mImePackageName)) {
                    return true;
                }
            }
            return false;
        }

        public synchronized InputMethodInfo getInputMethodOfThisIme() {
            if (mCachedThisImeInfo != null) {
                return mCachedThisImeInfo;
            }
            for (final InputMethodInfo imi : mImm.getInputMethodList()) {
                if (imi.getPackageName().equals(mImePackageName)) {
                    mCachedThisImeInfo = imi;
                    return imi;
                }
            }
            throw new RuntimeException("Input method id for " + mImePackageName + " not found.");
        }

        public synchronized List<InputMethodSubtype> getEnabledInputMethodSubtypeList(
                final InputMethodInfo imi, final boolean allowsImplicitlySelectedSubtypes) {
            final HashMap<InputMethodInfo, List<InputMethodSubtype>> cache =
                    allowsImplicitlySelectedSubtypes
                    ? mCachedSubtypeListWithImplicitlySelected
                    : mCachedSubtypeListOnlyExplicitlySelected;
            final List<InputMethodSubtype> cachedList = cache.get(imi);
            if (cachedList != null) {
                return cachedList;
            }
            final List<InputMethodSubtype> result = mImm.getEnabledInputMethodSubtypeList(
                    imi, allowsImplicitlySelectedSubtypes);
            cache.put(imi, result);
            return result;
        }

        public synchronized void clear() {
            mCachedThisImeInfo = null;
            mCachedSubtypeListWithImplicitlySelected.clear();
            mCachedSubtypeListOnlyExplicitlySelected.clear();
        }
    }

    public InputMethodInfo getInputMethodInfoOfThisIme() {
        return mInputMethodInfoCache.getInputMethodOfThisIme();
    }

    public String getInputMethodIdOfThisIme() {
        return getInputMethodInfoOfThisIme().getId();
    }

    public Locale getCurrentSubtypeLocale() {
        return getCurrentSubtype().getLocaleObject();
    }

    public MySubtype getCurrentSubtype() {
        return mVirtualSubtypeManager.getCurrentSubtype();
    }

    public boolean hasMultipleEnabledImesOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        if (hasMultipleEnabledSubtypesInThisIme()) {
            return true;
        }
        final List<InputMethodInfo> enabledImis = mImmService.getEnabledInputMethodList();
        return hasMultipleEnabledImes(shouldIncludeAuxiliarySubtypes, enabledImis);
    }

    public boolean hasMultipleEnabledSubtypesInThisIme() {
        return mVirtualSubtypeManager.hasMultiple();
    }

    private boolean hasMultipleEnabledImes(final boolean shouldIncludeAuxiliarySubtypes,
                                           final List<InputMethodInfo> imiList) {
        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) {
                return true;
            }
            final List<InputMethodSubtype> subtypes = getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount;
                continue;
            }

            int auxCount = 0;
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount;
                }
            }
            final int nonAuxCount = subtypes.size() - auxCount;

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount;
            }
        }

        return filteredImisCount > 1;
    }

    public MySubtype findSubtypeByLocale(final Locale locale) {
        // Find the best subtype based on a straightforward matching algorithm.
        // TODO: Use LocaleList#getFirstMatch() instead.
        final HashSet<MySubtype> subtypes = getEnabledSubtypesOfThisIme();
        for (final MySubtype subtype : subtypes) {
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.equals(locale)) {
                return subtype;
            }
        }
        for (final MySubtype subtype : subtypes) {
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage()) &&
                    subtypeLocale.getCountry().equals(locale.getCountry()) &&
                    subtypeLocale.getVariant().equals(locale.getVariant())) {
                return subtype;
            }
        }
        for (final MySubtype subtype : subtypes) {
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage()) &&
                    subtypeLocale.getCountry().equals(locale.getCountry())) {
                return subtype;
            }
        }
        for (final MySubtype subtype : subtypes) {
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage())) {
                return subtype;
            }
        }
        return null;
    }

    private List<InputMethodSubtype> getEnabledInputMethodSubtypeList(final InputMethodInfo imi,
            final boolean allowsImplicitlySelectedSubtypes) {
        return mInputMethodInfoCache.getEnabledInputMethodSubtypeList(
                imi, allowsImplicitlySelectedSubtypes);
    }

    /**
     * Get the list of subtypes that are always available for this IME. This returns the list of
     * subtypes from method.xml.
     * @return the list of default subtypes.
     */
    public List<InputMethodSubtype> getDefaultSubtypesOfThisIme() {
        final InputMethodInfo imi = getInputMethodInfoOfThisIme();
        final int inputMethodResId = imi.getServiceInfo().metaData.getInt(SUBTYPE_META_DATA_NAME);
        final XmlResourceParser parser = mContext.getResources().getXml(inputMethodResId);
        final List<InputMethodSubtype> defaultSubtypes = new ArrayList<>();
        try {
            while (parser.getEventType() != XmlResourceParser.END_DOCUMENT) {
                final int event = parser.next();
                if (event == XmlPullParser.START_TAG) {
                    final String tag = parser.getName();
                    if (TAG_SUBTYPE.equals(tag)) {
                        defaultSubtypes.add(parseSubtype(parser));
                    }
                }
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to parse default subtypes");
            return new ArrayList<>();
        }
        return defaultSubtypes;
    }

    /**
     * Build a subtype from an xml tag.
     * @param parser the XML parser.
     * @return the subtype that the xml defines.
     */
    private static InputMethodSubtype parseSubtype(final XmlResourceParser parser) {
        final InputMethodSubtype.InputMethodSubtypeBuilder builder =
                new InputMethodSubtype.InputMethodSubtypeBuilder();

        final int icon = parser.getAttributeResourceValue(XML_NAMESPACE, TAG_ICON, -1);
        if (icon != -1) {
            builder.setSubtypeIconResId(icon);
        }

        final int label = parser.getAttributeResourceValue(XML_NAMESPACE, TAG_LABEL, -1);
        if (label != -1) {
            builder.setSubtypeNameResId(label);
        }

        final int id = parser.getAttributeIntValue(XML_NAMESPACE, TAG_SUBTYPE_ID, -1);
        if (id != -1) {
            builder.setSubtypeId(id);
        }

        final String locale = parser.getAttributeValue(XML_NAMESPACE, TAG_LOCALE);
        builder.setSubtypeLocale(locale);

        final String mode = parser.getAttributeValue(XML_NAMESPACE, TAG_MODE);
        builder.setSubtypeMode(mode);

        final String extraValue = parser.getAttributeValue(XML_NAMESPACE, TAG_EXTRA_VALUE);
        builder.setSubtypeExtraValue(extraValue);

        final boolean asciiCapable = parser.getAttributeBooleanValue(XML_NAMESPACE,
                TAG_ASCII_CAPABLE, false);
        builder.setIsAsciiCapable(asciiCapable);

        return builder.build();
    }

    public void refreshSubtypeCaches() {
        mInputMethodInfoCache.clear();
    }

    public boolean shouldOfferSwitchingToNextInputMethod(final IBinder binder) {
        //TODO: fix this - this is only used for switching to other IMEs

        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return hasMultipleEnabledSubtypesInThisIme();
        }
        return mImmService.shouldOfferSwitchingToNextInputMethod(binder);
    }

    public boolean setCurrentSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.setCurrentSubtype(subtype);
    }

    public List<Locale> getSystemLocales() {
        ArrayList<Locale> locales = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList localeList = Resources.getSystem().getConfiguration().getLocales();
            for (int i = 0; i < localeList.size(); i++) {
                locales.add(localeList.get(i));
            }
        } else {
            locales.add(Resources.getSystem().getConfiguration().locale);
        }
        return locales;
    }

    private static final String LOCALE_ENGLISH_UNITED_STATES = "en_US";
    private static final String LOCALE_ENGLISH_GREAT_BRITAIN = "en_GB";
    private static final String LOCALE_AFRIKAANS = "af";
    private static final String LOCALE_ARABIC = "ar";
    private static final String LOCALE_AZERBAIJANI_AZERBAIJAN = "az_AZ";
    private static final String LOCALE_BELARUSIAN_BELARUS = "be_BY";
    private static final String LOCALE_BULGARIAN = "bg";
    private static final String LOCALE_BENGALI_BANGLADESH = "bn_BD";
    private static final String LOCALE_BENGALI_INDIA = "bn_IN";
    private static final String LOCALE_CATALAN = "ca";
    private static final String LOCALE_CZECH = "cs";
    private static final String LOCALE_DANISH = "da";
    private static final String LOCALE_GERMAN = "de";
    private static final String LOCALE_GERMAN_SWITZERLAND = "de_CH";
    private static final String LOCALE_GREEK = "el";
    private static final String LOCALE_ENGLISH_INDIA = "en_IN";
    private static final String LOCALE_ESPERANTO = "eo";
    private static final String LOCALE_SPANISH = "es";
    private static final String LOCALE_SPANISH_UNITED_STATES = "es_US";
    private static final String LOCALE_SPANISH_LATIN_AMERICA = "es_419";
    private static final String LOCALE_ESTONIAN_ESTONIA = "et_EE";
    private static final String LOCALE_BASQUE_SPAIN = "eu_ES";
    private static final String LOCALE_PERSIAN = "fa";
    private static final String LOCALE_FINNISH = "fi";
    private static final String LOCALE_FRENCH = "fr";
    private static final String LOCALE_FRENCH_CANADA = "fr_CA";
    private static final String LOCALE_FRENCH_SWITZERLAND = "fr_CH";
    private static final String LOCALE_GALICIAN_SPAIN = "gl_ES";
    private static final String LOCALE_HINDI = "hi";
    //hi_ZZ: Hinglish/qwerty  # This is a preliminary keyboard layout.
    private static final String LOCALE_CROATIAN = "hr";
    private static final String LOCALE_HUNGARIAN = "hu";
    private static final String LOCALE_ARMENIAN_ARMENIA = "hy_AM";
    private static final String LOCALE_INDONESIAN = "in"; // "id" is the official language code of Indonesian. // Java uses the deprecated "in" code instead of the standard "id" code for Indonesian.
    private static final String LOCALE_ICELANDIC = "is";
    private static final String LOCALE_ITALIAN = "it";
    private static final String LOCALE_ITALIAN_SWITZERLAND = "it_CH";
    private static final String LOCALE_HEBREW = "iw"; // "he" is the official language code of Hebrew. // Java uses the deprecated "iw" code instead of the standard "he" code for Hebrew.
    private static final String LOCALE_GEORGIAN_GEORGIA = "ka_GE";
    private static final String LOCALE_KAZAKH = "kk";
    private static final String LOCALE_KHMER_CAMBODIA = "km_KH";
    private static final String LOCALE_KANNADA_INDIA = "kn_IN";
    private static final String LOCALE_KYRGYZ = "ky";
    private static final String LOCALE_LAO_LAOS = "lo_LA";
    private static final String LOCALE_LITHUANIAN = "lt";
    private static final String LOCALE_LATVIAN = "lv";
    private static final String LOCALE_MACEDONIAN = "mk";
    private static final String LOCALE_MALAYALAM_INDIA = "ml_IN";
    private static final String LOCALE_MONGOLIAN_MONGOLIA = "mn_MN";
    private static final String LOCALE_MARATHI_INDIA = "mr_IN";
    private static final String LOCALE_MALAY_MALAYSIA = "ms_MY";
    private static final String LOCALE_NORWEGIAN_BOKMAL = "nb"; // Norwegian Bokmål
    private static final String LOCALE_NEPALI_NEPAL = "ne_NP";
    private static final String LOCALE_DUTCH = "nl";
    private static final String LOCALE_DUTCH_BELGIUM = "nl_BE";
    private static final String LOCALE_POLISH = "pl";
    private static final String LOCALE_PORTUGUESE_BRAZIL = "pt_BR";
    private static final String LOCALE_PORTUGUESE_PORTUGAL = "pt_PT";
    private static final String LOCALE_ROMANIAN = "ro";
    private static final String LOCALE_RUSSIAN = "ru";
    //si_LK: Sinhala (Sri Lanka)/sinhala # This is a preliminary keyboard layout.
    private static final String LOCALE_SLOVAK = "sk";
    private static final String LOCALE_SLOVENIAN = "sl";
    private static final String LOCALE_SERBIAN = "sr";
    private static final String LOCALE_SERBIAN_LATIN = "sr_ZZ";
    private static final String LOCALE_SWEDISH = "sv";
    private static final String LOCALE_SWAHILI = "sw";
    private static final String LOCALE_TAMIL_INDIA = "ta_IN";
    //ta_LK: Tamil (Sri Lanka)/tamil # Disabled in conjunction with si_LK.
    private static final String LOCALE_TAMIL_SINGAPORE = "ta_SG";
    private static final String LOCALE_TELUGU_INDIA = "te_IN";
    private static final String LOCALE_THAI = "th";
    private static final String LOCALE_TAGALOG = "tl";
    private static final String LOCALE_TURKISH = "tr";
    private static final String LOCALE_UKRAINIAN = "uk";
    private static final String LOCALE_URDU = "ur";
    private static final String LOCALE_UZBEK_UZBEKISTAN = "uz_UZ";
    private static final String LOCALE_VIETNAMESE = "vi";
    private static final String LOCALE_ZULU = "zu";
    public static final String[] sSupportedLocales = new String[] {//TODO: this probably should be private
            LOCALE_ENGLISH_UNITED_STATES,
            LOCALE_ENGLISH_GREAT_BRITAIN,
            LOCALE_AFRIKAANS,
            LOCALE_ARABIC,
            LOCALE_AZERBAIJANI_AZERBAIJAN,
            LOCALE_BELARUSIAN_BELARUS,
            LOCALE_BULGARIAN,
            LOCALE_BENGALI_BANGLADESH,
            LOCALE_BENGALI_INDIA,
            LOCALE_CATALAN,
            LOCALE_CZECH,
            LOCALE_DANISH,
            LOCALE_GERMAN,
            LOCALE_GERMAN_SWITZERLAND,
            LOCALE_GREEK,
            LOCALE_ENGLISH_INDIA,
            LOCALE_ESPERANTO,
            LOCALE_SPANISH,
            LOCALE_SPANISH_UNITED_STATES,
            LOCALE_SPANISH_LATIN_AMERICA,
            LOCALE_ESTONIAN_ESTONIA,
            LOCALE_BASQUE_SPAIN,
            LOCALE_PERSIAN,
            LOCALE_FINNISH,
            LOCALE_FRENCH,
            LOCALE_FRENCH_CANADA,
            LOCALE_FRENCH_SWITZERLAND,
            LOCALE_GALICIAN_SPAIN,
            LOCALE_HINDI,
            LOCALE_CROATIAN,
            LOCALE_HUNGARIAN,
            LOCALE_ARMENIAN_ARMENIA,
            LOCALE_INDONESIAN,
            LOCALE_ICELANDIC,
            LOCALE_ITALIAN,
            LOCALE_ITALIAN_SWITZERLAND,
            LOCALE_HEBREW,
            LOCALE_GEORGIAN_GEORGIA,
            LOCALE_KAZAKH,
            LOCALE_KHMER_CAMBODIA,
            LOCALE_KANNADA_INDIA,
            LOCALE_KYRGYZ,
            LOCALE_LAO_LAOS,
            LOCALE_LITHUANIAN,
            LOCALE_LATVIAN,
            LOCALE_MACEDONIAN,
            LOCALE_MALAYALAM_INDIA,
            LOCALE_MONGOLIAN_MONGOLIA,
            LOCALE_MARATHI_INDIA,
            LOCALE_MALAY_MALAYSIA,
            LOCALE_NORWEGIAN_BOKMAL,
            LOCALE_NEPALI_NEPAL,
            LOCALE_DUTCH,
            LOCALE_DUTCH_BELGIUM,
            LOCALE_POLISH,
            LOCALE_PORTUGUESE_BRAZIL,
            LOCALE_PORTUGUESE_PORTUGAL,
            LOCALE_ROMANIAN,
            LOCALE_RUSSIAN,
            LOCALE_SLOVAK,
            LOCALE_SLOVENIAN,
            LOCALE_SERBIAN,
            LOCALE_SERBIAN_LATIN,
            LOCALE_SWEDISH,
            LOCALE_SWAHILI,
            LOCALE_TAMIL_INDIA,
            LOCALE_TAMIL_SINGAPORE,
            LOCALE_TELUGU_INDIA,
            LOCALE_THAI,
            LOCALE_TAGALOG,
            LOCALE_TURKISH,
            LOCALE_UKRAINIAN,
            LOCALE_URDU,
            LOCALE_UZBEK_UZBEKISTAN,
            LOCALE_VIETNAMESE,
            LOCALE_ZULU
    };

    private static final String LAYOUT_QWERTY = "qwerty";
    private static final String LAYOUT_AZERTY = "azerty";
    private static final String LAYOUT_QWERTZ = "qwertz";
    private static final String LAYOUT_ARABIC = "arabic";
    private static final String LAYOUT_ARMENIAN_PHONETIC = "armenian_phonetic";
    private static final String LAYOUT_BENGALI = "bengali";
    private static final String LAYOUT_BENGALI_AKKHOR = "bengali_akkhor";
    private static final String LAYOUT_BULGARIAN = "bulgarian";
    private static final String LAYOUT_BULGARIAN_BDS = "bulgarian_bds";
    private static final String LAYOUT_EAST_SLAVIC = "east_slavic";
    private static final String LAYOUT_FARSI = "farsi";
    private static final String LAYOUT_GEORGIAN = "georgian";
    private static final String LAYOUT_GREEK = "greek";
    private static final String LAYOUT_HEBREW = "hebrew";
    private static final String LAYOUT_HINDI = "hindi";
    private static final String LAYOUT_HINDI_COMPACT = "hindi_compact";
    private static final String LAYOUT_KANNADA = "kannada";
    private static final String LAYOUT_KHMER = "khmer";
    private static final String LAYOUT_LAO = "lao";
    private static final String LAYOUT_MACEDONIAN = "macedonian";
    private static final String LAYOUT_MALAYALAM = "malayalam";
    private static final String LAYOUT_MARATHI = "marathi";
    private static final String LAYOUT_MONGOLIAN = "mongolian";
    private static final String LAYOUT_NEPALI_ROMANIZED = "nepali_romanized";
    private static final String LAYOUT_NEPALI_TRADITIONAL = "nepali_traditional";
    private static final String LAYOUT_NORDIC = "nordic";
    private static final String LAYOUT_SERBIAN = "serbian";
    private static final String LAYOUT_SERBIAN_QWERTZ = "serbian_qwertz";
    private static final String LAYOUT_SPANISH = "spanish";
    private static final String LAYOUT_SWISS = "swiss";
    private static final String LAYOUT_TAMIL = "tamil";
    private static final String LAYOUT_TELUGU = "telugu";
    private static final String LAYOUT_THAI = "thai";
    private static final String LAYOUT_TURKISH_F = "turkish_f";
    private static final String LAYOUT_URDU = "urdu";
    private static final String LAYOUT_UZBEK = "uzbek";

    public List<MySubtype> getSubtypes(final String locale) {
        List<MySubtype> subtypes = new ArrayList<>();
        switch (locale) {
            case LOCALE_ENGLISH_UNITED_STATES:
                subtypes.add(createSubtype(locale, R.string.subtype_en_US, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_en_US);
                break;
            case LOCALE_ENGLISH_GREAT_BRITAIN:
                subtypes.add(createSubtype(locale, R.string.subtype_en_GB, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_en_GB);
                break;
            case LOCALE_AFRIKAANS:
            case LOCALE_AZERBAIJANI_AZERBAIJAN:
            case LOCALE_ENGLISH_INDIA:
            case LOCALE_FRENCH_CANADA:
            case LOCALE_INDONESIAN:
            case LOCALE_ICELANDIC:
            case LOCALE_ITALIAN:
            case LOCALE_LITHUANIAN:
            case LOCALE_LATVIAN:
            case LOCALE_MALAY_MALAYSIA:
            case LOCALE_DUTCH:
            case LOCALE_POLISH:
            case LOCALE_PORTUGUESE_BRAZIL:
            case LOCALE_PORTUGUESE_PORTUGAL:
            case LOCALE_ROMANIAN:
            case LOCALE_SLOVAK:
            case LOCALE_SWAHILI:
            case LOCALE_VIETNAMESE:
            case LOCALE_ZULU:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_CZECH:
            case LOCALE_GERMAN:
            case LOCALE_CROATIAN:
            case LOCALE_HUNGARIAN:
            case LOCALE_SLOVENIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_FRENCH:
            case LOCALE_DUTCH_BELGIUM:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_AZERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_CATALAN:
//            case LOCALE_ESPERANTO:
            case LOCALE_SPANISH:
            case LOCALE_SPANISH_LATIN_AMERICA:
            case LOCALE_BASQUE_SPAIN:
            case LOCALE_GALICIAN_SPAIN:
            case LOCALE_TAGALOG:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SPANISH));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_ESPERANTO:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SPANISH));
                break;
            case LOCALE_SPANISH_UNITED_STATES:
                subtypes.add(createSubtype(locale, R.string.subtype_es_US, LAYOUT_SPANISH));
                addGenericLayouts(subtypes, locale, R.string.subtype_es_US);
                break;
            case LOCALE_DANISH:
            case LOCALE_ESTONIAN_ESTONIA:
            case LOCALE_FINNISH:
            case LOCALE_NORWEGIAN_BOKMAL:
            case LOCALE_SWEDISH:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_GERMAN_SWITZERLAND:
            case LOCALE_FRENCH_SWITZERLAND:
            case LOCALE_ITALIAN_SWITZERLAND:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SWISS));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_TURKISH:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                subtypes.add(createSubtype(locale, R.string.subtype_generic_f, LAYOUT_TURKISH_F));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_UZBEK_UZBEKISTAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_UZBEK));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_ARABIC:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_ARABIC));
                break;
            case LOCALE_BELARUSIAN_BELARUS:
            case LOCALE_KAZAKH:
            case LOCALE_KYRGYZ:
            case LOCALE_RUSSIAN:
            case LOCALE_UKRAINIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_EAST_SLAVIC));
                break;
            case LOCALE_BULGARIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_BULGARIAN));
                subtypes.add(createSubtype(locale, R.string.subtype_bulgarian_bds, LAYOUT_BULGARIAN_BDS, R.string.subtype_bds));
                break;
            case LOCALE_BENGALI_BANGLADESH:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_BENGALI_AKKHOR));
                break;
            case LOCALE_BENGALI_INDIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_BENGALI));
                break;
            case LOCALE_GREEK:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_GREEK));
                break;
            case LOCALE_PERSIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_FARSI));
                break;
            case LOCALE_HINDI:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_HINDI));
                subtypes.add(createSubtype(locale, R.string.subtype_generic_compact, LAYOUT_HINDI_COMPACT, R.string.subtype_compact));
                break;
            case LOCALE_ARMENIAN_ARMENIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_ARMENIAN_PHONETIC));
                break;
            case LOCALE_HEBREW:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_HEBREW));
                break;
            case LOCALE_GEORGIAN_GEORGIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_GEORGIAN));
                break;
            case LOCALE_KHMER_CAMBODIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_KHMER));
                break;
            case LOCALE_KANNADA_INDIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_KANNADA));
                break;
            case LOCALE_LAO_LAOS:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_LAO));
                break;
            case LOCALE_MACEDONIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_MACEDONIAN));
                break;
            case LOCALE_MALAYALAM_INDIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_MALAYALAM));
                break;
            case LOCALE_MONGOLIAN_MONGOLIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_MONGOLIAN));
                break;
            case LOCALE_MARATHI_INDIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_MARATHI));
                break;
            case LOCALE_NEPALI_NEPAL:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NEPALI_ROMANIZED));
                subtypes.add(createSubtype(locale, R.string.subtype_generic_traditional, LAYOUT_NEPALI_TRADITIONAL, R.string.subtype_traditional));
                break;
            case LOCALE_SERBIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SERBIAN));
                break;
            case LOCALE_SERBIAN_LATIN:
                subtypes.add(createSubtype(locale, R.string.subtype_sr_ZZ, LAYOUT_SERBIAN_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_sr_ZZ);
                break;
            case LOCALE_TAMIL_INDIA:
            case LOCALE_TAMIL_SINGAPORE:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_TAMIL));
                break;
            case LOCALE_TELUGU_INDIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_TELUGU));
                break;
            case LOCALE_THAI:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_THAI));
                break;
            case LOCALE_URDU:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_URDU));
                break;
        }
        return subtypes;
    }

    private MySubtype createSubtype(final String locale, final int labelRes, final String keyboardLayoutSet) {
        return createSubtype(locale, labelRes, keyboardLayoutSet, null);
    }

    private MySubtype createSubtype(final String locale, final int labelRes, final String keyboardLayoutSet, final int layoutRes) {
        return createSubtype(locale, labelRes, keyboardLayoutSet, mContext.getResources().getString(layoutRes));
    }

    private MySubtype createSubtype(final String locale, final int labelRes, final String keyboardLayoutSet, final String layoutName) {
//        final InputMethodSubtype subtype = AdditionalSubtypeUtils.createAdditionalSubtype(locale, keyboardLayoutSet);
        final InputMethodSubtype subtype = createSubtypeInternal(locale, labelRes, labelRes, keyboardLayoutSet, false, null);

        final MySubtype mySubtype = createMySubtype(locale, labelRes, keyboardLayoutSet, layoutName);
        compareSubtypes(subtype, mySubtype);

        return mySubtype;
    }
    private MySubtype createMySubtype(final String locale, final int labelRes, final String keyboardLayoutSet, String layoutName) {
        final String localeDisplayName = LocaleUtils.constructLocaleFromString(locale).getDisplayName();
        final Resources res = mContext.getResources();
        final String subtypeName = res.getString(labelRes, localeDisplayName);
        if (layoutName == null) {

            final String[] predefinedLayouts = mContext.getResources().getStringArray(
                    R.array.predefined_layouts);
            final int predefinedLayoutIndex = Arrays.asList(predefinedLayouts).indexOf(keyboardLayoutSet);
            if (predefinedLayoutIndex >= 0) {
                final String[] predefinedLayoutDisplayNames = mContext.getResources().getStringArray(
                        R.array.predefined_layout_display_names);
                if (predefinedLayoutIndex < predefinedLayoutDisplayNames.length) {
                    layoutName = predefinedLayoutDisplayNames[predefinedLayoutIndex];
                } else {
                    //TODO: probably handle this differently - possibly don't bother with this check
                    layoutName = "unknown";
                }
            } else {
                layoutName = LocaleUtils.constructLocaleFromString(locale).getDisplayLanguage();
            }
        }
        return new MySubtype(locale, subtypeName, keyboardLayoutSet, layoutName);
    }
    private MySubtype createMySubtypeAlternate(final String locale, final int labelRes, final String keyboardLayoutSet, final String layoutName) {
        final String localeDisplayName = LocaleUtils.constructLocaleFromString(locale).getDisplayName();
        final Resources res = mContext.getResources();
        final String baseName = res.getString(labelRes, localeDisplayName);
        final String subtypeName = res.getString(R.string.subtype_generic_layout, baseName, layoutName);
        return new MySubtype(locale, subtypeName, keyboardLayoutSet, layoutName);
    }

    private MySubtype createSubtypeInternal2(final String localeString, final int labelRes,
                                             final String keyboardLayoutSetName,
                                             final boolean isAdditional,
                                             final String genericKeyboardLayoutSetDisplayName) {

        Resources res = mContext.getResources();
        final String baseName = res.getString(labelRes, LocaleUtils.constructLocaleFromString(localeString).getDisplayName());
        final String name;
        if (isAdditional) {
            name = res.getString(R.string.subtype_generic_layout, baseName, genericKeyboardLayoutSetDisplayName);
        } else {
            name = baseName;
        }
        return new MySubtype(localeString, name, keyboardLayoutSetName, "dummy");
    }

    private void compareSubtypes(final InputMethodSubtype standardSubtype, final MySubtype mySubtype) {
        final String nameStandard = SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(standardSubtype);
        final String nameMy = mySubtype.getName();
        final boolean nameSame = nameStandard.equals(nameMy);
        final String nameMessage = "name: equal=" + nameSame
                + (nameSame ? " " + nameStandard : " standard=" + nameStandard + ", my=" + nameMy);


        final boolean localeSame = standardSubtype.getLocale().equals(mySubtype.getLocale());
        final String localeMessage = "locale: equal=" + localeSame
                + (localeSame ? " " + standardSubtype.getLocale() : " standard=" + standardSubtype.getLocale() + ", my=" + mySubtype.getLocale());


        final String layoutStandard = SubtypeLocaleUtils.getKeyboardLayoutSetName(standardSubtype);
        final String layoutMy = mySubtype.getLayoutSet();
        final boolean layoutSame = layoutStandard.equals(layoutMy);
        final String layoutMessage = "layout: equal=" + layoutSame
                + (layoutSame ? " " + layoutStandard : " standard=" + layoutStandard + ", my=" + layoutMy);


        final String layoutNameStandard = SubtypeLocaleUtils.getKeyboardLayoutDisplayName(standardSubtype, mContext);
//        final String layoutNameMy = SubtypeLocaleUtils.getKeyboardLayoutDisplayName(mySubtype, mContext);
        final String layoutNameMy = mySubtype.getLayoutDisplayName();
        final boolean layoutNameSame = layoutNameStandard.equals(layoutNameMy);
        final String layoutNameMessage = "layoutName: equal=" + layoutNameSame
                + (layoutNameSame ? " " + layoutNameStandard : " standard=" + layoutNameStandard + ", my=" + layoutNameMy);


        if (nameSame && localeSame && layoutSame && layoutNameSame) {
            Log.w(TAG, nameMessage);
            Log.w(TAG, localeMessage);
            Log.w(TAG, layoutMessage);
            Log.w(TAG, layoutNameMessage);
        } else {
            Log.e(TAG, nameMessage);
            Log.e(TAG, localeMessage);
            Log.e(TAG, layoutMessage);
            Log.e(TAG, layoutNameMessage);
        }
    }

    private void addGenericLayouts(final List<MySubtype> subtypes, final String locale, final int labelRes) {
        final int initialSize = subtypes.size();
        final String[] predefinedKeyboardLayoutSets = mContext.getResources().getStringArray(
                R.array.predefined_layouts);
        final String[] predefinedKeyboardLayoutSetDisplayNames = mContext.getResources().getStringArray(
                R.array.predefined_layout_display_names);
        for (int i = 0; i < predefinedKeyboardLayoutSets.length; i++) {
            final String predefinedLayout = predefinedKeyboardLayoutSets[i];
            boolean alreadyExists = false;
            for (int j = 0; j < initialSize; j++) {
                if (subtypes.get(j).getLayoutSet().equals(predefinedLayout)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (alreadyExists) {
                continue;
            }


            final InputMethodSubtype subtype = createSubtypeInternal(locale, labelRes, SubtypeLocaleUtils.getSubtypeNameId(locale, predefinedLayout), predefinedLayout, true, predefinedKeyboardLayoutSetDisplayNames[i]);

            final MySubtype mySubtype = createMySubtypeAlternate(locale, labelRes, predefinedLayout, predefinedKeyboardLayoutSetDisplayNames[i]);
            subtypes.add(mySubtype);
            compareSubtypes(subtype, mySubtype);
        }
    }

    private InputMethodSubtype createSubtypeInternal(
            final String localeString, final int labelRes, final int labelResGeneric, final String keyboardLayoutSetName, final boolean isAdditional, final String genericKeyboardLayoutSetDisplayName) {
//        final int nameId = SubtypeLocaleUtils.getSubtypeNameId(localeString, keyboardLayoutSetName);
        final int nameId = isAdditional ? labelResGeneric : labelRes;
        final String platformVersionDependentExtraValues = getPlatformVersionDependentExtraValue(
                localeString, keyboardLayoutSetName, isAdditional);
        final int platformVersionIndependentSubtypeId =
                getPlatformVersionIndependentSubtypeId(localeString, keyboardLayoutSetName);
        InputMethodSubtype.InputMethodSubtypeBuilder builder = new InputMethodSubtype.InputMethodSubtypeBuilder();

        builder.setSubtypeNameResId(nameId)
                .setSubtypeIconResId(R.drawable.ic_ime_switcher_dark)
                .setSubtypeLocale(localeString)
                .setSubtypeMode(KEYBOARD_MODE)
                .setSubtypeExtraValue(platformVersionDependentExtraValues)
                .setOverridesImplicitlyEnabledSubtype(false)
                .setIsAuxiliary(false)
                .setSubtypeId(platformVersionIndependentSubtypeId);
        final InputMethodSubtype result = builder.build();


//        compareSubtypes(result, createSubtypeInternal2(localeString, labelRes, keyboardLayoutSetName, isAdditional, genericKeyboardLayoutSetDisplayName));

        return result;

    }
    private static String getPlatformVersionDependentExtraValue(final String localeString, final String keyboardLayoutSetName, final boolean isAdditional) {
        final ArrayList<String> extraValueItems = new ArrayList<>();
        extraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            extraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        if (isAdditional) {
            extraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        }
        return TextUtils.join(",", extraValueItems);
    }
    private static int getPlatformVersionIndependentSubtypeId(final String localeString,
                                                              final String keyboardLayoutSetName) {
        // For compatibility reasons, we concatenate the extra values in the following order.
        // - KeyboardLayoutSet
        // - AsciiCapable
        // - UntranslatableReplacementStringInSubtypeName
        // - EmojiCapable
        // - isAdditionalSubtype
        final ArrayList<String> compatibilityExtraValueItems = new ArrayList<>();
        compatibilityExtraValueItems.add(KEYBOARD_LAYOUT_SET + "=" + keyboardLayoutSetName);
        if (SubtypeLocaleUtils.isExceptionalLocale(localeString)) {
            compatibilityExtraValueItems.add(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME + "=" +
                    SubtypeLocaleUtils.getKeyboardLayoutSetDisplayName(keyboardLayoutSetName));
        }
        compatibilityExtraValueItems.add(IS_ADDITIONAL_SUBTYPE);
        final String compatibilityExtraValues = TextUtils.join(",", compatibilityExtraValueItems);
        return Arrays.hashCode(new Object[] {
                localeString,
                KEYBOARD_MODE,
                compatibilityExtraValues,
                false /* isAuxiliary */,
                false /* overrideImplicitlyEnabledSubtype */ });
    }
}
