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

public class Subtype {
    private static final String TAG = Subtype.class.getSimpleName();

    private static final int NO_RESOURCE = 0;

    private final String mLocale;
    private final String mLayoutSet;
    private final int mLayoutNameRes;
    private final String mLayoutNameStr;
    private final boolean mShowLayoutInName;
    private final Resources mResources;

    public Subtype(final String locale, final String layoutSet, final int layoutNameRes,
                   final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = layoutNameRes;
        mLayoutNameStr = null;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    public Subtype(final String locale, final String layoutSet, final String layoutNameStr,
                   final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = NO_RESOURCE;
        mLayoutNameStr = layoutNameStr;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    public String getLocale() {
        return mLocale;
    }

    public Locale getLocaleObject() {
        return LocaleUtils.constructLocaleFromString(mLocale);
    }

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

    public String getLayoutSet() {
        return mLayoutSet;
    }

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

    /**
     * Get the full display name of the locale in its locale.
     * @return
     */
    public String getLocaleDisplayNameInLocale() {
        return LocaleResourceUtils.getLocaleDisplayName(getLocale());
    }

    /**
     * Get the display name of the language in its locale.
     * @return
     */
    public String getLanguageDisplayNameInLocale() {
        return LocaleResourceUtils.getLanguageDisplayName(getLocale());
    }
}
