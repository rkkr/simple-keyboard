/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rkr.simplekeyboard.inputmethod.latin.common;

import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;

/**
 * A class to help with handling Locales in string form.
 *
 * This file has the same meaning and features (and shares all of its code) with the one with the
 * same name in Latin IME. They need to be kept synchronized; for any update/bugfix to
 * this file, consider also updating/fixing the version in Latin IME.
 */
public final class LocaleUtils {
    private LocaleUtils() {
        // Intentional empty constructor for utility class.
    }

    // Locale match level constants.
    // A higher level of match is guaranteed to have a higher numerical value.
    // Some room is left within constants to add match cases that may arise necessary
    // in the future, for example differentiating between the case where the countries
    // are both present and different, and the case where one of the locales does not
    // specify the countries. This difference is not needed now.

    private static final HashMap<String, Locale> sLocaleCache = new HashMap<>();

    /**
     * Creates a locale from a string specification.
     * @param localeString a string specification of a locale, in a format of "ll_cc_variant" where
     * "ll" is a language code, "cc" is a country code.
     */
    public static Locale constructLocaleFromString(final String localeString) {
        synchronized (sLocaleCache) {
            if (sLocaleCache.containsKey(localeString)) {
                return sLocaleCache.get(localeString);
            }
            final String[] elements = localeString.split("_", 3);
            final Locale locale;
            if (elements.length == 1) {
                locale = new Locale(elements[0] /* language */);
            } else if (elements.length == 2) {
                locale = new Locale(elements[0] /* language */, elements[1] /* country */);
            } else { // localeParams.length == 3
                locale = new Locale(elements[0] /* language */, elements[1] /* country */,
                        elements[2] /* variant */);
            }
            sLocaleCache.put(localeString, locale);
            return locale;
        }
    }

    /**
     * Creates a string specification for a locale.
     * @param locale the locale
     * @return a string specification of a locale, in a format of "ll_cc_variant" where "ll" is a
     * language code, "cc" is a country code.
     */
    public static String getLocaleString(final Locale locale) {
        if (TextUtils.isEmpty(locale.getCountry())) {
            return locale.getLanguage();
        }
        if (TextUtils.isEmpty(locale.getVariant())) {
            return locale.getLanguage() + "_" + locale.getCountry();
        }
        return locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
    }

    public static Locale findBestLocale(final Locale localeToFind, final Collection<Locale> collection) {
        // Find the best subtype based on a straightforward matching algorithm.
        // TODO: Use LocaleList#getFirstMatch() instead.
        for (final Locale locale : collection) {
            if (locale.equals(localeToFind)) {
                return locale;
            }
        }
        for (final Locale locale : collection) {
            if (locale.getLanguage().equals(localeToFind.getLanguage()) &&
                    locale.getCountry().equals(localeToFind.getCountry()) &&
                    locale.getVariant().equals(localeToFind.getVariant())) {
                return locale;
            }
        }
        for (final Locale locale : collection) {
            if (locale.getLanguage().equals(localeToFind.getLanguage()) &&
                    locale.getCountry().equals(localeToFind.getCountry())) {
                return locale;
            }
        }
        for (final Locale locale : collection) {
            if (locale.getLanguage().equals(localeToFind.getLanguage())) {
                return locale;
            }
        }
        return null;
    }

    public static List<Locale> getSystemLocales() {
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

    /**
     * Comparator for {@link Locale} to order them alphabetically
     * first.
     */
    public static class LocaleComparator implements Comparator<Locale> {
        @Override
        public int compare(Locale a, Locale b) {
            if (a.equals(b)) {
                // ensure that this is consistent with equals
                return 0;
            }
            final String aDisplay =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(getLocaleString(a));
            final String bDisplay =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(getLocaleString(b));
            final int result = aDisplay.compareToIgnoreCase(bDisplay);
            if (result != 0) {
                return result;
            }
            // ensure that non-equal objects are distinguished to be consistent with equals
            return a.hashCode() > b.hashCode() ? 1 : -1;
        }
    }
}
