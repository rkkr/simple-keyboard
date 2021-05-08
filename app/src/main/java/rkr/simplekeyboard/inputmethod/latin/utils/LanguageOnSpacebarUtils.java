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

package rkr.simplekeyboard.inputmethod.latin.utils;

import java.util.Locale;
import java.util.Set;

import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.MySubtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodSubtype;

/**
 * This class determines that the language name on the spacebar should be displayed in what format.
 */
public final class LanguageOnSpacebarUtils {
    public static final int FORMAT_TYPE_NONE = 0;
    public static final int FORMAT_TYPE_LANGUAGE_ONLY = 1;
    public static final int FORMAT_TYPE_FULL_LOCALE = 2;

    private LanguageOnSpacebarUtils() {
        // This utility class is not publicly instantiable.
    }

    public static int getLanguageOnSpacebarFormatType(final MySubtype subtype) {
        final Locale locale = subtype.getLocaleObject();
        if (locale == null) {
            return FORMAT_TYPE_NONE;
        }
        final String keyboardLanguage = locale.getLanguage();
        final String keyboardLayout = subtype.getLayoutSet();
        int sameLanguageAndLayoutCount = 0;
        final Set<MySubtype> enabledSubtypes =
                RichInputMethodManager.getInstance().getEnabledSubtypesOfThisIme();
        for (final MySubtype enabledSubtype : enabledSubtypes) {
            final String language = enabledSubtype.getLocaleObject().getLanguage();
            if (keyboardLanguage.equals(language)
                    && keyboardLayout.equals(enabledSubtype.getLayoutSet())) {
                sameLanguageAndLayoutCount++;
            }
        }
        // Display full locale name only when there are multiple subtypes that have the same
        // locale and keyboard layout. Otherwise displaying language name is enough.
        return sameLanguageAndLayoutCount > 1 ? FORMAT_TYPE_FULL_LOCALE
                : FORMAT_TYPE_LANGUAGE_ONLY;
    }
}
