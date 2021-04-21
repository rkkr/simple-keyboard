/*
 * Copyright (C) 2011 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.inputmethod.InputMethodSubtype;

import java.util.HashMap;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;

import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.KEYBOARD_LAYOUT_SET;
import static rkr.simplekeyboard.inputmethod.latin.common.Constants.Subtype.ExtraValue.UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME;

/**
 * A helper class to deal with subtype locales.
  */
// TODO: consolidate this into RichInputMethodSubtype
public final class SubtypeLocaleUtils {
    static final String TAG = SubtypeLocaleUtils.class.getSimpleName();

    // This reference class {@link R} must be located in the same package as LatinIME.java.
    private static final String RESOURCE_PACKAGE_NAME = R.class.getPackage().getName();

    public static final String QWERTY = "qwerty";
    public static final int UNKNOWN_KEYBOARD_LAYOUT = R.string.subtype_generic;

    private static volatile boolean sInitialized = false;
    private static final Object sInitializeLock = new Object();
    private static Resources sResources;
    // Keyboard layout to its display name map.
    private static final HashMap<String, String> sKeyboardLayoutToDisplayNameMap = new HashMap<>();
    // Keyboard layout to subtype name resource id map.
    private static final HashMap<String, Integer> sKeyboardLayoutToNameIdsMap = new HashMap<>();
    // Exceptional locale whose name should be displayed in Locale.ROOT.
    private static final HashMap<String, Integer> sExceptionalLocaleDisplayedInRootLocale =
            new HashMap<>();
    // Exceptional locale to subtype name resource id map.
    private static final HashMap<String, Integer> sExceptionalLocaleToNameIdsMap = new HashMap<>();
    // Exceptional locale to subtype name with layout resource id map.
    private static final HashMap<String, Integer> sExceptionalLocaleToWithLayoutNameIdsMap =
            new HashMap<>();
    private static final String SUBTYPE_NAME_RESOURCE_PREFIX =
            "string/subtype_";
    private static final String SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX =
            "string/subtype_generic_";
    private static final String SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX =
            "string/subtype_with_layout_";
    private static final String SUBTYPE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX =
            "string/subtype_in_root_locale_";
    // Keyboard layout set name for the subtypes that don't have a keyboardLayoutSet extra value.
    // This is for compatibility to keep the same subtype ids as pre-JellyBean.
    private static final HashMap<String, String> sLocaleAndExtraValueToKeyboardLayoutSetMap =
            new HashMap<>();

    private SubtypeLocaleUtils() {
        // Intentional empty constructor for utility class.
    }

    // Note that this initialization method can be called multiple times.
    public static void init(final Context context) {
        synchronized (sInitializeLock) {
            if (sInitialized == false) {
                initLocked(context);
                sInitialized = true;
            }
        }
    }

    private static void initLocked(final Context context) {
        final Resources res = context.getResources();
        sResources = res;

        final String[] predefinedLayoutSet = res.getStringArray(R.array.predefined_layouts);
        final String[] layoutDisplayNames = res.getStringArray(
                R.array.predefined_layout_display_names);
        for (int i = 0; i < predefinedLayoutSet.length; i++) {
            final String layoutName = predefinedLayoutSet[i];
            sKeyboardLayoutToDisplayNameMap.put(layoutName, layoutDisplayNames[i]);
            final String resourceName = SUBTYPE_NAME_RESOURCE_GENERIC_PREFIX + layoutName;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sKeyboardLayoutToNameIdsMap.put(layoutName, resId);
        }

        final String[] exceptionalLocaleInRootLocale = res.getStringArray(
                R.array.subtype_locale_displayed_in_root_locale);
        for (int i = 0; i < exceptionalLocaleInRootLocale.length; i++) {
            final String localeString = exceptionalLocaleInRootLocale[i];
            final String resourceName = SUBTYPE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX + localeString;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleDisplayedInRootLocale.put(localeString, resId);
        }

        final String[] exceptionalLocales = res.getStringArray(
                R.array.subtype_locale_exception_keys);
        for (int i = 0; i < exceptionalLocales.length; i++) {
            final String localeString = exceptionalLocales[i];
            final String resourceName = SUBTYPE_NAME_RESOURCE_PREFIX + localeString;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleToNameIdsMap.put(localeString, resId);
            final String resourceNameWithLayout =
                    SUBTYPE_NAME_RESOURCE_WITH_LAYOUT_PREFIX + localeString;
            final int resIdWithLayout = res.getIdentifier(
                    resourceNameWithLayout, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleToWithLayoutNameIdsMap.put(localeString, resIdWithLayout);
        }

        final String[] keyboardLayoutSetMap = res.getStringArray(
                R.array.locale_and_extra_value_to_keyboard_layout_set_map);
        for (int i = 0; i + 1 < keyboardLayoutSetMap.length; i += 2) {
            final String key = keyboardLayoutSetMap[i];
            final String keyboardLayoutSet = keyboardLayoutSetMap[i + 1];
            sLocaleAndExtraValueToKeyboardLayoutSetMap.put(key, keyboardLayoutSet);
        }
    }

    public static boolean isExceptionalLocale(final String localeString) {
        return sExceptionalLocaleToNameIdsMap.containsKey(localeString);
    }

    public static int getSubtypeNameId(final String localeString, final String keyboardLayoutName) {
        if (isExceptionalLocale(localeString)) {
            return sExceptionalLocaleToWithLayoutNameIdsMap.get(localeString);
        }
        final Integer nameId = sKeyboardLayoutToNameIdsMap.get(keyboardLayoutName);
        return nameId == null ? UNKNOWN_KEYBOARD_LAYOUT : nameId;
    }

    public static Locale getDisplayLocaleOfSubtypeLocale(final String localeString) {
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            return Locale.ROOT;
        }
        return LocaleUtils.constructLocaleFromString(localeString);
    }

    public static String getSubtypeLocaleDisplayNameInSystemLocale(
            final String localeString) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale);
    }

    public static String getSubtypeLocaleDisplayName(final String localeString) {
        final Locale displayLocale = getDisplayLocaleOfSubtypeLocale(localeString);
        return getSubtypeLocaleDisplayNameInternal(localeString, displayLocale);
    }

    public static String getSubtypeLanguageDisplayName(final String localeString) {
        final Locale displayLocale = getDisplayLocaleOfSubtypeLocale(localeString);
        final String languageString;
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            languageString = localeString;
        } else {
            languageString = LocaleUtils.constructLocaleFromString(localeString).getLanguage();
        }
        return getSubtypeLocaleDisplayNameInternal(languageString, displayLocale);
    }

    private static String getSubtypeLocaleDisplayNameInternal(final String localeString,
            final Locale displayLocale) {
        final Integer exceptionalNameResId;
        if (displayLocale.equals(Locale.ROOT)
                && sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            exceptionalNameResId = sExceptionalLocaleDisplayedInRootLocale.get(localeString);
        } else if (sExceptionalLocaleToNameIdsMap.containsKey(localeString)) {
            exceptionalNameResId = sExceptionalLocaleToNameIdsMap.get(localeString);
        } else {
            exceptionalNameResId = null;
        }

        final String displayName;
        if (exceptionalNameResId != null) {
            final RunInLocale<String> getExceptionalName = new RunInLocale<String>() {
                @Override
                protected String job(final Resources res) {
                    return res.getString(exceptionalNameResId);
                }
            };
            displayName = getExceptionalName.runInLocale(sResources, displayLocale);
        } else {
            displayName = LocaleUtils.constructLocaleFromString(localeString)
                    .getDisplayName(displayLocale);
        }
        return StringUtils.capitalizeFirstCodePoint(displayName, displayLocale);
    }

    // InputMethodSubtype's display name in its locale.
    //        isAdditionalSubtype (T=true, F=false)
    // locale layout  |  display name
    // ------ ------- - ----------------------
    //  en_US qwerty  F  English (US)            exception
    //  en_GB qwerty  F  English (UK)            exception
    //  es_US spanish F  Español (EE.UU.)        exception
    //  fr    azerty  F  Français
    //  fr_CA qwerty  F  Français (Canada)
    //  fr_CH swiss   F  Français (Suisse)
    //  de    qwertz  F  Deutsch
    //  de_CH swiss   T  Deutsch (Schweiz)
    //  zz    qwerty  F  Alphabet (QWERTY)       in system locale
    //  fr    qwertz  T  Français (QWERTZ)
    //  de    qwerty  T  Deutsch (QWERTY)
    //  en_US azerty  T  English (US) (AZERTY)   exception
    //  zz    azerty  T  Alphabet (AZERTY)       in system locale

    private static String getReplacementString(final InputMethodSubtype subtype,
            final Locale displayLocale) {
        if (subtype.containsExtraValueKey(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME)) {
            return subtype.getExtraValueOf(UNTRANSLATABLE_STRING_IN_SUBTYPE_NAME);
        }
        return getSubtypeLocaleDisplayNameInternal(subtype.getLocale(), displayLocale);
    }

    public static String getSubtypeDisplayNameInSystemLocale(
            final InputMethodSubtype subtype) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        return getSubtypeDisplayNameInternal(subtype, displayLocale);
    }

    public static String getSubtypeNameForLogging(final InputMethodSubtype subtype) {
        if (subtype == null) {
            return "<null subtype>";
        }
        return getSubtypeLocale(subtype) + "/" + getKeyboardLayoutSetName(subtype);
    }

    private static String getSubtypeDisplayNameInternal(final InputMethodSubtype subtype,
            final Locale displayLocale) {
        final String replacementString = getReplacementString(subtype, displayLocale);
        // TODO: rework this for multi-lingual subtypes
        final int nameResId = subtype.getNameResId();
        final RunInLocale<String> getSubtypeName = new RunInLocale<String>() {
            @Override
            protected String job(final Resources res) {
                try {
                    return res.getString(nameResId, replacementString);
                } catch (Resources.NotFoundException e) {
                    // TODO: Remove this catch when InputMethodManager.getCurrentInputMethodSubtype
                    // is fixed.
                    Log.w(TAG, "Unknown subtype: mode=" + subtype.getMode()
                            + " nameResId=" + subtype.getNameResId()
                            + " locale=" + subtype.getLocale()
                            + " extra=" + subtype.getExtraValue()
                            + "\n" + DebugLogUtils.getStackTrace());
                    return "";
                }
            }
        };
        return StringUtils.capitalizeFirstCodePoint(
                getSubtypeName.runInLocale(sResources, displayLocale), displayLocale);
    }

    public static Locale getSubtypeLocale(final InputMethodSubtype subtype) {
        final String localeString = subtype.getLocale();
        return LocaleUtils.constructLocaleFromString(localeString);
    }

    public static String getKeyboardLayoutSetDisplayName(
            final InputMethodSubtype subtype) {
        final String layoutName = getKeyboardLayoutSetName(subtype);
        return getKeyboardLayoutSetDisplayName(layoutName);
    }

    public static String getKeyboardLayoutSetDisplayName(final String layoutName) {
        return sKeyboardLayoutToDisplayNameMap.get(layoutName);
    }

    public static String getKeyboardLayoutSetName(final InputMethodSubtype subtype) {
        String keyboardLayoutSet = subtype.getExtraValueOf(KEYBOARD_LAYOUT_SET);
        if (keyboardLayoutSet == null) {
            // This subtype doesn't have a keyboardLayoutSet extra value, so lookup its keyboard
            // layout set in sLocaleAndExtraValueToKeyboardLayoutSetMap to keep it compatible with
            // pre-JellyBean.
            keyboardLayoutSet = sLocaleAndExtraValueToKeyboardLayoutSetMap.get(subtype.getLocale());
        }
        // TODO: Remove this null check when InputMethodManager.getCurrentInputMethodSubtype is
        // fixed.
        if (keyboardLayoutSet == null) {
            android.util.Log.w(TAG, "KeyboardLayoutSet not found, use QWERTY: " +
                    "locale=" + subtype.getLocale() + " extraValue=" + subtype.getExtraValue());
            return QWERTY;
        }
        return keyboardLayoutSet;
    }
}
