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

import java.util.HashMap;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;

/**
 * A helper class to deal with displaying locales.
  */
public final class LocaleResourceUtils {
    // This reference class {@link R} must be located in the same package as LatinIME.java.
    private static final String RESOURCE_PACKAGE_NAME = R.class.getPackage().getName();

    private static volatile boolean sInitialized = false;
    private static final Object sInitializeLock = new Object();
    private static Resources sResources;
    // Exceptional locale whose name should be displayed in Locale.ROOT.
    private static final HashMap<String, Integer> sExceptionalLocaleDisplayedInRootLocale =
            new HashMap<>();
    // Exceptional locale to locale name resource id map.
    private static final HashMap<String, Integer> sExceptionalLocaleToNameIdsMap = new HashMap<>();
    private static final String LOCALE_NAME_RESOURCE_PREFIX =
            "string/locale_name_";
    private static final String LOCALE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX =
            "string/locale_name_in_root_locale_";

    private LocaleResourceUtils() {
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

        final String[] exceptionalLocaleInRootLocale = res.getStringArray(
                R.array.locale_displayed_in_root_locale);
        for (int i = 0; i < exceptionalLocaleInRootLocale.length; i++) {
            final String localeString = exceptionalLocaleInRootLocale[i];
            final String resourceName = LOCALE_NAME_RESOURCE_IN_ROOT_LOCALE_PREFIX + localeString;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleDisplayedInRootLocale.put(localeString, resId);
        }

        final String[] exceptionalLocales = res.getStringArray(R.array.locale_exception_keys);
        for (int i = 0; i < exceptionalLocales.length; i++) {
            final String localeString = exceptionalLocales[i];
            final String resourceName = LOCALE_NAME_RESOURCE_PREFIX + localeString;
            final int resId = res.getIdentifier(resourceName, null, RESOURCE_PACKAGE_NAME);
            sExceptionalLocaleToNameIdsMap.put(localeString, resId);
        }
    }

    private static Locale getDisplayLocale(final String localeString) {
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            return Locale.ROOT;
        }
        return LocaleUtils.constructLocaleFromString(localeString);
    }

    /**
     * Get the full display name of the locale in the system's locale.
     * For example in an English system, en_US: "English (US)", fr_CA: "French (Canada)"
     * @param localeString the locale to display.
     * @return the full display name of the locale.
     */
    public static String getLocaleDisplayNameInSystemLocale(
            final String localeString) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        return getLocaleDisplayNameInternal(localeString, displayLocale);
    }

    /**
     * Get the full display name of the locale in its locale.
     * For example, en_US: "English (US)", fr_CA: "Français (Canada)"
     * @param localeString the locale to display.
     * @return the full display name of the locale.
     */
    public static String getLocaleDisplayNameInLocale(final String localeString) {
        final Locale displayLocale = getDisplayLocale(localeString);
        return getLocaleDisplayNameInternal(localeString, displayLocale);
    }

    /**
     * Get the display name of the language in the system's locale.
     * For example in an English system, en_US: "English", fr_CA: "French"
     * @param localeString the locale to display.
     * @return the display name of the language.
     */
    public static String getLanguageDisplayNameInSystemLocale(
            final String localeString) {
        final Locale displayLocale = sResources.getConfiguration().locale;
        final String languageString;
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            languageString = localeString;
        } else {
            languageString = LocaleUtils.constructLocaleFromString(localeString).getLanguage();
        }
        return getLocaleDisplayNameInternal(languageString, displayLocale);
    }

    /**
     * Get the display name of the language in its locale.
     * For example, en_US: "English", fr_CA: "Français"
     * @param localeString the locale to display.
     * @return the display name of the language.
     */
    public static String getLanguageDisplayNameInLocale(final String localeString) {
        final Locale displayLocale = getDisplayLocale(localeString);
        final String languageString;
        if (sExceptionalLocaleDisplayedInRootLocale.containsKey(localeString)) {
            languageString = localeString;
        } else {
            languageString = LocaleUtils.constructLocaleFromString(localeString).getLanguage();
        }
        return getLocaleDisplayNameInternal(languageString, displayLocale);
    }

    private static String getLocaleDisplayNameInternal(final String localeString,
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
            displayName = sResources.getString(exceptionalNameResId);
        } else {
            displayName = LocaleUtils.constructLocaleFromString(localeString)
                    .getDisplayName(displayLocale);
        }
        return StringUtils.capitalizeFirstCodePoint(displayName, displayLocale);
    }
}
