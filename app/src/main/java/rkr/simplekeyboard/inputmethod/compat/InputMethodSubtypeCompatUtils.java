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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodSubtype;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.annotations.UsedForTesting;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodSubtype;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;

public final class InputMethodSubtypeCompatUtils {
    // Note that InputMethodSubtype(int nameId, int iconId, String locale, String mode,
    // String extraValue, boolean isAuxiliary, boolean overridesImplicitlyEnabledSubtype, int id)
    // has been introduced in API level 17 (Build.VERSION_CODE.JELLY_BEAN_MR1).
    private static final Constructor<?> CONSTRUCTOR_INPUT_METHOD_SUBTYPE =
            CompatUtils.getConstructor(InputMethodSubtype.class,
                    int.class, int.class, String.class, String.class, String.class, boolean.class,
                    boolean.class, int.class);

    // Note that {@link InputMethodSubtype#isAsciiCapable()} has been introduced in API level 19
    // (Build.VERSION_CODE.KITKAT).
    private static final Method METHOD_isAsciiCapable = CompatUtils.getMethod(
            InputMethodSubtype.class, "isAsciiCapable");

    private InputMethodSubtypeCompatUtils() {
        // This utility class is not publicly instantiable.
    }

    @NonNull
    public static InputMethodSubtype newInputMethodSubtype(int nameId, int iconId, String locale,
            String mode, String extraValue, boolean isAuxiliary,
            boolean overridesImplicitlyEnabledSubtype, int id) {
        return (InputMethodSubtype) CompatUtils.newInstance(CONSTRUCTOR_INPUT_METHOD_SUBTYPE,
                nameId, iconId, locale, mode, extraValue, isAuxiliary,
                overridesImplicitlyEnabledSubtype, id);
    }

    public static boolean isAsciiCapable(final RichInputMethodSubtype subtype) {
        return isAsciiCapable(subtype.getRawSubtype());
    }

    public static boolean isAsciiCapable(final InputMethodSubtype subtype) {
        return isAsciiCapableWithAPI(subtype)
                || subtype.containsExtraValueKey(Constants.Subtype.ExtraValue.ASCII_CAPABLE);
    }

    // Note that InputMethodSubtype.getLanguageTag() is expected to be available in Android N+.
    private static final Method GET_LANGUAGE_TAG =
            CompatUtils.getMethod(InputMethodSubtype.class, "getLanguageTag");

    public static Locale getLocaleObject(final InputMethodSubtype subtype) {
        // Locale.forLanguageTag() is available only in Android L and later.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final String languageTag = (String) CompatUtils.invoke(subtype, null, GET_LANGUAGE_TAG);
            if (!TextUtils.isEmpty(languageTag)) {
                return Locale.forLanguageTag(languageTag);
            }
        }
        return LocaleUtils.constructLocaleFromString(subtype.getLocale());
    }

    @UsedForTesting
    public static boolean isAsciiCapableWithAPI(final InputMethodSubtype subtype) {
        return (Boolean)CompatUtils.invoke(subtype, false, METHOD_isAsciiCapable);
    }
}
