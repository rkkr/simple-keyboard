/*
 * Copyright (C) 2013 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.compat;

import android.os.Build;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodSubtype;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.latin.MySubtype;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;

public final class InputMethodSubtypeCompatUtils {
    private InputMethodSubtypeCompatUtils() {
        // This utility class is not publicly instantiable.
    }

    public static Locale getLocaleObject(final InputMethodSubtype subtype) {
        // Locale.forLanguageTag() is available only in Android L and later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            final String languageTag = subtype.getLanguageTag();
            if (!TextUtils.isEmpty(languageTag)) {
                return Locale.forLanguageTag(languageTag);
            }
        }
        return LocaleUtils.constructLocaleFromString(subtype.getLocale());
    }

    //TODO: figure out the language tag and special handling for sr_ZZ
    public static Locale getLocaleObject(final MySubtype subtype) {
        return LocaleUtils.constructLocaleFromString(subtype.getLocale());
    }
}
