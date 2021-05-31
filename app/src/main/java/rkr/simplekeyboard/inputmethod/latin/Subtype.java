/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.res.Resources;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;

/**
 * A keyboard layout for a locale.
 */
public final class Subtype {
    private static final int NO_RESOURCE = 0;

    private final String mLocale;
    private final String mLayoutSet;
    private final int mLayoutNameRes;
    private final String mLayoutNameStr;
    private final boolean mShowLayoutInName;
    private final Resources mResources;

    /**
     * Create a subtype.
     * @param locale the locale for the layout in the format of "ll_cc_variant" where "ll" is a
     *              language code, "cc" is a country code.
     * @param layoutSet the keyboard layout set name.
     * @param layoutNameRes the keyboard layout name resource ID to use for display instead of the
     *                     name of the language.
     * @param showLayoutInName flag to indicate if the display name of the keyboard layout should be
     *                        used in the main display name of the subtype
     *                        (eg: "English (US) (QWERTY)" vs "English (US)").
     * @param resources the resources to use.
     */
    public Subtype(final String locale, final String layoutSet, final int layoutNameRes,
                   final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = layoutNameRes;
        mLayoutNameStr = null;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    /**
     * Create a subtype.
     * @param locale the locale for the layout in the format of "ll_cc_variant" where "ll" is a
     *              language code, "cc" is a country code.
     * @param layoutSet the keyboard layout set name.
     * @param layoutNameStr the keyboard layout name string to use for display instead of the name
     *                     of the language.
     * @param showLayoutInName flag to indicate if the display name of the keyboard layout should be
     *                        used in the main display name of the subtype
     *                        (eg: "English (US) (QWERTY)" vs "English (US)").
     * @param resources the resources to use.
     */
    public Subtype(final String locale, final String layoutSet, final String layoutNameStr,
                   final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = NO_RESOURCE;
        mLayoutNameStr = layoutNameStr;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    /**
     * Get the locale string.
     * @return the locale string.
     */
    public String getLocale() {
        return mLocale;
    }

    /**
     * Get the locale object.
     * @return the locale object.
     */
    public Locale getLocaleObject() {
        return LocaleUtils.constructLocaleFromString(mLocale);
    }

    /**
     * Get the display name for the subtype. This should be something like "English (US)" or
     * "English (US) (QWERTY)".
     * @return the display name.
     */
    public String getName() {
        final String localeDisplayName =
                LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(mLocale);
        if (mShowLayoutInName) {
            if (mLayoutNameRes != NO_RESOURCE) {
                return mResources.getString(R.string.subtype_generic_layout, localeDisplayName,
                        mResources.getString(mLayoutNameRes));
            }
            if (mLayoutNameStr != null) {
                return mResources.getString(R.string.subtype_generic_layout, localeDisplayName,
                        mLayoutNameStr);
            }
        }
        return localeDisplayName;
    }

    /**
     * Get the keyboard layout set name (internal).
     * @return the keyboard layout set name.
     */
    public String getKeyboardLayoutSet() {
        return mLayoutSet;
    }

    /**
     * Get the display name for the keyboard layout. This should be something like "QWERTY".
     * @return the display name for the keyboard layout.
     */
    public String getLayoutDisplayName() {
        final String displayName;
        if (mLayoutNameRes != NO_RESOURCE) {
            displayName = mResources.getString(mLayoutNameRes);
        } else if (mLayoutNameStr != null) {
            displayName = mLayoutNameStr;
        } else {
            displayName = LocaleResourceUtils.getLanguageDisplayNameInSystemLocale(mLocale);
        }
        return displayName;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof Subtype)) {
            return false;
        }
        final Subtype other = (Subtype)o;
        return mLocale.equals(other.mLocale) && mLayoutSet.equals(other.mLayoutSet);
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + mLocale.hashCode();
        hashCode = hashCode * 31 + mLayoutSet.hashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        return "subtype " + mLocale + ":" + mLayoutSet;
    }
}
