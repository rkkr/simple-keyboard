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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import rkr.simplekeyboard.inputmethod.latin.Subtype;

public final class SubtypePreferenceUtils {
    private static final String TAG = SubtypePreferenceUtils.class.getSimpleName();

    private static final Subtype[] EMPTY_SUBTYPE_ARRAY = new Subtype[0];

    private SubtypePreferenceUtils() {
        // This utility class is not publicly instantiable.
    }

    private static final String LOCALE_AND_LAYOUT_SEPARATOR = ":";
    private static final int INDEX_OF_LOCALE = 0;
    private static final int INDEX_OF_KEYBOARD_LAYOUT = 1;
    private static final int LENGTH_WITHOUT_EXTRA_VALUE = (INDEX_OF_KEYBOARD_LAYOUT + 1);
    private static final String PREF_SUBTYPE_SEPARATOR = ";";

    private static String getPrefString(final Subtype subtype) {
        final String localeString = subtype.getLocale();
        final String keyboardLayoutSetName = subtype.getLayoutSet();
        return localeString + LOCALE_AND_LAYOUT_SEPARATOR + keyboardLayoutSetName;
    }

    public static Subtype[] createSubtypesFromPref(final String prefSubtypes,
                                                   final Resources resources) {
        if (TextUtils.isEmpty(prefSubtypes)) {
            return EMPTY_SUBTYPE_ARRAY;
        }
        final String[] prefSubtypeArray = prefSubtypes.split(PREF_SUBTYPE_SEPARATOR);
        final ArrayList<Subtype> subtypesList = new ArrayList<>(prefSubtypeArray.length);
        for (final String prefSubtype : prefSubtypeArray) {
            final String[] elems = prefSubtype.split(LOCALE_AND_LAYOUT_SEPARATOR);
            if (elems.length != LENGTH_WITHOUT_EXTRA_VALUE) {
                Log.w(TAG, "Unknown subtype specified: " + prefSubtype + " in "
                        + prefSubtypes);
                continue;
            }
            final String localeString = elems[INDEX_OF_LOCALE];
            final String keyboardLayoutSetName = elems[INDEX_OF_KEYBOARD_LAYOUT];
            final Subtype subtype =
                    SubtypeUtils.getSubtype(localeString, keyboardLayoutSetName, resources);
            if (subtype == null) {
                continue;
            }
            subtypesList.add(subtype);
        }
        return subtypesList.toArray(new Subtype[subtypesList.size()]);
    }

    public static String createPrefSubtypes(final List<Subtype> subtypes) {
        if (subtypes == null || subtypes.size() == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Subtype subtype : subtypes) {
            if (sb.length() > 0) {
                sb.append(PREF_SUBTYPE_SEPARATOR);
            }
            sb.append(getPrefString(subtype));
        }
        return sb.toString();
    }
}
