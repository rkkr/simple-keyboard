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
import android.inputmethodservice.InputMethodService;
import android.os.AsyncTask;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.InputMethodSubtypeCompatUtils;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.settings.Settings;
import rkr.simplekeyboard.inputmethod.latin.utils.AdditionalSubtypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.LanguageOnSpacebarUtils;
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
    private RichInputMethodSubtype mCurrentRichInputMethodSubtype;

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
        final InputMethodSubtype[] additionalSubtypes = getAdditionalSubtypes();
        mImmService.setAdditionalInputMethodSubtypes(
                getInputMethodIdOfThisIme(), additionalSubtypes);

        // Initialize the current input method subtype.
        refreshSubtypeCaches();
    }

    public InputMethodSubtype[] getAdditionalSubtypes() {
        final SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(mContext);
        final String prefAdditionalSubtypes = Settings.readPrefAdditionalSubtypes(
                prefs, mContext.getResources());
        return AdditionalSubtypeUtils.createAdditionalSubtypesArray(prefAdditionalSubtypes);
    }

    public InputMethodManager getInputMethodManager() {
        checkInitialized();
        return mImmService;
    }

    public boolean isInputMethodOfThisImeEnabled() {
        return mInputMethodInfoCache.isInputMethodOfThisImeEnabled();
    }

    public List<InputMethodSubtype> getMyEnabledInputMethodSubtypeList(
            boolean allowsImplicitlySelectedSubtypes) {
        return getEnabledInputMethodSubtypeList(
                getInputMethodInfoOfThisIme(), allowsImplicitlySelectedSubtypes);
    }

    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (mImmService.switchToNextInputMethod(token, onlyCurrentIme)) {
            return true;
        }
        // Was not able to call {@link InputMethodManager#switchToNextInputMethodIBinder,boolean)}
        // because the current device is running ICS or previous and lacks the API.
        if (switchToNextInputSubtypeInThisIme(token, onlyCurrentIme)) {
            return true;
        }
        return switchToNextInputMethodAndSubtype(token);
    }

    private boolean switchToNextInputSubtypeInThisIme(final IBinder token,
            final boolean onlyCurrentIme) {
        final InputMethodSubtype currentSubtype = mImmService.getCurrentInputMethodSubtype();
        final List<InputMethodSubtype> enabledSubtypes = getMyEnabledInputMethodSubtypeList(
                true /* allowsImplicitlySelectedSubtypes */);
        final int currentIndex = getSubtypeIndexInList(currentSubtype, enabledSubtypes);
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(TAG, "Can't find current subtype in enabled subtypes: subtype="
                    + SubtypeLocaleUtils.getSubtypeNameForLogging(currentSubtype));
            return false;
        }
        final int nextIndex = (currentIndex + 1) % enabledSubtypes.size();
        if (nextIndex <= currentIndex && !onlyCurrentIme) {
            // The current subtype is the last or only enabled one and it needs to switch to
            // next IME.
            return false;
        }
        final InputMethodSubtype nextSubtype = enabledSubtypes.get(nextIndex);
        setInputMethodAndSubtype(token, nextSubtype);
        return true;
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

    public boolean checkIfSubtypeBelongsToThisImeAndEnabled(final InputMethodSubtype subtype) {
        return checkIfSubtypeBelongsToList(subtype,
                getEnabledInputMethodSubtypeList(
                        getInputMethodInfoOfThisIme(),
                        true /* allowsImplicitlySelectedSubtypes */));
    }

    private static boolean checkIfSubtypeBelongsToList(final InputMethodSubtype subtype,
            final List<InputMethodSubtype> subtypes) {
        return getSubtypeIndexInList(subtype, subtypes) != INDEX_NOT_FOUND;
    }

    private static int getSubtypeIndexInList(final InputMethodSubtype subtype,
            final List<InputMethodSubtype> subtypes) {
        final int count = subtypes.size();
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype ims = subtypes.get(index);
            if (ims.equals(subtype)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    public void onSubtypeChanged(final InputMethodSubtype newSubtype) {
        updateCurrentSubtype(newSubtype);
        if (DEBUG) {
            Log.w(TAG, "onSubtypeChanged: " + mCurrentRichInputMethodSubtype.getNameForLogging());
        }
    }

    private static RichInputMethodSubtype sForcedSubtypeForTesting = null;

    public Locale getCurrentSubtypeLocale() {
        if (null != sForcedSubtypeForTesting) {
            return sForcedSubtypeForTesting.getLocale();
        }
        return getCurrentSubtype().getLocale();
    }

    public RichInputMethodSubtype getCurrentSubtype() {
        if (null != sForcedSubtypeForTesting) {
            return sForcedSubtypeForTesting;
        }
        return mCurrentRichInputMethodSubtype;
    }

    public boolean hasMultipleEnabledIMEsOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> enabledImis = mImmService.getEnabledInputMethodList();
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, enabledImis);
    }

    public boolean hasMultipleEnabledSubtypesInThisIme(
            final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> imiList = Collections.singletonList(
                getInputMethodInfoOfThisIme());
        return hasMultipleEnabledSubtypes(shouldIncludeAuxiliarySubtypes, imiList);
    }

    private boolean hasMultipleEnabledSubtypes(final boolean shouldIncludeAuxiliarySubtypes,
            final List<InputMethodInfo> imiList) {
        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) return true;
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

        if (filteredImisCount > 1) {
            return true;
        }
        final List<InputMethodSubtype> subtypes = getMyEnabledInputMethodSubtypeList(true);
        int keyboardCount = 0;
        // imm.getEnabledInputMethodSubtypeList(null, true) will return the current IME's
        // both explicitly and implicitly enabled input method subtype.
        // (The current IME should be LatinIME.)
        for (InputMethodSubtype subtype : subtypes) {
            if (KEYBOARD_MODE.equals(subtype.getMode())) {
                ++keyboardCount;
            }
        }
        return keyboardCount > 1;
    }

    public InputMethodSubtype findSubtypeByLocaleAndKeyboardLayoutSet(final String localeString,
            final String keyboardLayoutSetName) {
        final InputMethodInfo myImi = getInputMethodInfoOfThisIme();
        final int count = myImi.getSubtypeCount();
        for (int i = 0; i < count; i++) {
            final InputMethodSubtype subtype = myImi.getSubtypeAt(i);
            final String layoutName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype);
            if (localeString.equals(subtype.getLocale())
                    && keyboardLayoutSetName.equals(layoutName)) {
                return subtype;
            }
        }
        return null;
    }

    public InputMethodSubtype findSubtypeByLocale(final Locale locale) {
        // Find the best subtype based on a straightforward matching algorithm.
        // TODO: Use LocaleList#getFirstMatch() instead.
        final List<InputMethodSubtype> subtypes =
                getMyEnabledInputMethodSubtypeList(true /* allowsImplicitlySelectedSubtypes */);
        final int count = subtypes.size();
        for (int i = 0; i < count; ++i) {
            final InputMethodSubtype subtype = subtypes.get(i);
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.equals(locale)) {
                return subtype;
            }
        }
        for (int i = 0; i < count; ++i) {
            final InputMethodSubtype subtype = subtypes.get(i);
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage()) &&
                    subtypeLocale.getCountry().equals(locale.getCountry()) &&
                    subtypeLocale.getVariant().equals(locale.getVariant())) {
                return subtype;
            }
        }
        for (int i = 0; i < count; ++i) {
            final InputMethodSubtype subtype = subtypes.get(i);
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage()) &&
                    subtypeLocale.getCountry().equals(locale.getCountry())) {
                return subtype;
            }
        }
        for (int i = 0; i < count; ++i) {
            final InputMethodSubtype subtype = subtypes.get(i);
            final Locale subtypeLocale = InputMethodSubtypeCompatUtils.getLocaleObject(subtype);
            if (subtypeLocale.getLanguage().equals(locale.getLanguage())) {
                return subtype;
            }
        }
        return null;
    }

    public void setInputMethodAndSubtype(final IBinder token, final InputMethodSubtype subtype) {
        mImmService.setInputMethodAndSubtype(
                token, getInputMethodIdOfThisIme(), subtype);
    }

    public void setAdditionalInputMethodSubtypes(final InputMethodSubtype[] subtypes) {
        mImmService.setAdditionalInputMethodSubtypes(
                getInputMethodIdOfThisIme(), subtypes);
        // Clear the cache so that we go read the {@link InputMethodInfo} of this IME and list of
        // subtypes again next time.
        refreshSubtypeCaches();
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
        updateCurrentSubtype(mImmService.getCurrentInputMethodSubtype());
    }

    public boolean shouldOfferSwitchingToNextInputMethod(final IBinder binder) {
        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return hasMultipleEnabledSubtypesInThisIme(false);
        }
        return mImmService.shouldOfferSwitchingToNextInputMethod(binder);
    }

    private void updateCurrentSubtype(final InputMethodSubtype subtype) {
        mCurrentRichInputMethodSubtype = RichInputMethodSubtype.getRichInputMethodSubtype(subtype);
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

    public List<InputMethodSubtype> getSubtypes(final String locale) {
        List<InputMethodSubtype> subtypes = new ArrayList<>();
        switch (locale) {
            case LOCALE_ENGLISH_UNITED_STATES:
                subtypes.add(createSubtype(locale, R.string.subtype_en_US, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_en_US);
                break;
            case LOCALE_ENGLISH_GREAT_BRITAIN:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_en_GB, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_en_GB);
                break;
            case LOCALE_AFRIKAANS:
            case LOCALE_AZERBAIJANI_AZERBAIJAN:
            case LOCALE_ENGLISH_INDIA:
            case LOCALE_INDONESIAN:
            case LOCALE_ICELANDIC:
            case LOCALE_LITHUANIAN:
            case LOCALE_LATVIAN:
            case LOCALE_MALAY_MALAYSIA:
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
            case LOCALE_CATALAN:
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
            case LOCALE_SPANISH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SPANISH));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_SPANISH_UNITED_STATES:
                subtypes.add(createSubtype(locale, R.string.subtype_es_US, LAYOUT_SPANISH));
                addGenericLayouts(subtypes, locale, R.string.subtype_es_US);
                break;
            case LOCALE_GERMAN_SWITZERLAND:
            case LOCALE_FRENCH_SWITZERLAND:
            case LOCALE_ITALIAN_SWITZERLAND:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_SWISS));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_ESTONIAN_ESTONIA:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_CZECH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_DANISH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_GERMAN:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_FINNISH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_FRENCH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_AZERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_FRENCH_CANADA:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_CROATIAN:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_HUNGARIAN:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_ITALIAN:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_NORWEGIAN_BOKMAL:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_DUTCH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_DUTCH_BELGIUM:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_AZERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_POLISH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_SLOVENIAN:
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_SWEDISH:
                //TODO: verify this is always true
                subtypes.add(createSubtype(locale, R.string.subtype_generic, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale, R.string.subtype_generic);
                break;
            case LOCALE_TURKISH:
                //TODO: verify this is always true
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
                subtypes.add(createSubtype(locale, R.string.subtype_bulgarian_bds, LAYOUT_BULGARIAN_BDS));
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
                subtypes.add(createSubtype(locale, R.string.subtype_generic_compact, LAYOUT_HINDI_COMPACT));
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
                subtypes.add(createSubtype(locale, R.string.subtype_generic_traditional, LAYOUT_NEPALI_TRADITIONAL));
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

    private InputMethodSubtype createSubtype(final String locale, final int labelRes, final String keyboardLayoutSet) {
//        final InputMethodSubtype subtype = AdditionalSubtypeUtils.createAdditionalSubtype(locale, keyboardLayoutSet);
        final InputMethodSubtype subtype = createSubtypeInternal(locale, labelRes, keyboardLayoutSet, false);
        return /*new RichInputMethodSubtype*/(subtype);
    }

    private void addGenericLayouts(final List<InputMethodSubtype> subtypes, final String locale, final int labelRes) {
        final int initialSize = subtypes.size();
        final String[] predefinedKeyboardLayoutSets = mContext.getResources().getStringArray(
                R.array.predefined_layouts);
        for (final String predefinedLayout : predefinedKeyboardLayoutSets) {
            boolean alreadyExists = false;
            for (int i = 0; i < initialSize; i++) {
                if (SubtypeLocaleUtils.getKeyboardLayoutSetName(subtypes.get(i)).equals(predefinedLayout)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (alreadyExists) {
                continue;
            }
            subtypes.add(createSubtypeInternal(locale, /*labelRes*/SubtypeLocaleUtils.getSubtypeNameId(locale, predefinedLayout), predefinedLayout, true));
        }
    }

    private static InputMethodSubtype createSubtypeInternal(
            final String localeString, final int labelRes, final String keyboardLayoutSetName, final boolean isAdditional) {
//        final int nameId = SubtypeLocaleUtils.getSubtypeNameId(localeString, keyboardLayoutSetName);
        final int nameId = labelRes;
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
        return builder.build();
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
