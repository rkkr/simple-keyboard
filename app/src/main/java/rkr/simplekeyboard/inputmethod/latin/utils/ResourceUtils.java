/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2024 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
 * Copyright (C) 2019 Micha LaQua
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues;

public final class ResourceUtils {
    public static final float UNDEFINED_RATIO = -1.0f;
    public static final int UNDEFINED_DIMENSION = -1;

    private ResourceUtils() {
        // This utility class is not publicly instantiable.
    }

    public static int getKeyboardHeight(final Context context, final Resources res, final SettingsValues settingsValues) {
        final int defaultKeyboardHeight = getDefaultKeyboardHeight(context, res);
        float scale = settingsValues.mKeyboardHeightScale;
        return (int)(defaultKeyboardHeight * scale);
    }

    public static int getDefaultKeyboardHeight(final Context context, final Resources res) {
        int heightPixels = 0;
        int widthPixels = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final WindowManager wm = context.getSystemService(WindowManager.class);
            if (wm != null) {
                final Rect bounds = wm.getMaximumWindowMetrics().getBounds();
                heightPixels = bounds.height();
                widthPixels = bounds.width();
            }
        }

        if (heightPixels <= 0 || widthPixels <= 0) {
            final DisplayMetrics dm = res.getDisplayMetrics();
            heightPixels = dm.heightPixels;
            widthPixels = dm.widthPixels;
        }

        final float keyboardHeight = res.getDimension(R.dimen.config_default_keyboard_height);
        final float maxKeyboardHeight = res.getFraction(
                R.fraction.config_max_keyboard_height, heightPixels, heightPixels);
        float minKeyboardHeight = res.getFraction(
                R.fraction.config_min_keyboard_height, heightPixels, heightPixels);
        if (minKeyboardHeight < 0.0f) {
            // Specified fraction was negative, so it should be calculated against display
            // width.
            minKeyboardHeight = -res.getFraction(
                    R.fraction.config_min_keyboard_height, widthPixels, widthPixels);
        }

        return clampKeyboardHeight(keyboardHeight, minKeyboardHeight, maxKeyboardHeight);
    }

    static int clampKeyboardHeight(final float keyboardHeight, final float minKeyboardHeight,
                                   final float maxKeyboardHeight) {
        return (int)Math.max(Math.min(keyboardHeight, maxKeyboardHeight),
                Math.min(minKeyboardHeight, maxKeyboardHeight));
    }

    public static int getKeyboardBottomOffset(final Resources res,
            final SettingsValues settingsValues) {
        return res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                ? (int)(settingsValues.mBottomOffsetPortrait * res.getDisplayMetrics().density)
                : 0;
    }

    public static boolean isValidFraction(final float fraction) {
        return fraction >= 0.0f;
    }

    // {@link Resources#getDimensionPixelSize(int)} returns at least one pixel size.
    public static boolean isValidDimensionPixelSize(final int dimension) {
        return dimension > 0;
    }

    public static float getFraction(final TypedArray a, final int index, final float defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null || !isFractionValue(value)) {
            return defValue;
        }
        return a.getFraction(index, 1, 1, defValue);
    }

    public static float getFraction(final TypedArray a, final int index) {
        return getFraction(a, index, UNDEFINED_RATIO);
    }

    public static float getFraction(final TypedArray a, final int index, final float base,
                                    final float defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null || !isFractionValue(value)) {
            return defValue;
        }
        return value.getFraction(base, base);
    }

    public static int getDimensionPixelSize(final TypedArray a, final int index) {
        final TypedValue value = a.peekValue(index);
        if (value == null || !isDimensionValue(value)) {
            return ResourceUtils.UNDEFINED_DIMENSION;
        }
        return a.getDimensionPixelSize(index, ResourceUtils.UNDEFINED_DIMENSION);
    }

    public static float getDimensionOrFraction(final TypedArray a, final int index, final int base,
            final float defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (isFractionValue(value)) {
            return a.getFraction(index, base, base, defValue);
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue);
        }
        return defValue;
    }

    public static float getDimensionOrFraction(final TypedArray a, final int index,
                                               final float base, final float defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (isFractionValue(value)) {
            return value.getFraction(index, base);
        } else if (isDimensionValue(value)) {
            return a.getDimension(index, defValue);
        }
        return defValue;
    }

    public static int getEnumValue(final TypedArray a, final int index, final int defValue) {
        final TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (isIntegerValue(value)) {
            return a.getInt(index, defValue);
        }
        return defValue;
    }

    public static boolean isFractionValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_FRACTION;
    }

    public static boolean isDimensionValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_DIMENSION;
    }

    public static boolean isIntegerValue(final TypedValue v) {
        return v.type >= TypedValue.TYPE_FIRST_INT && v.type <= TypedValue.TYPE_LAST_INT;
    }

    public static boolean isStringValue(final TypedValue v) {
        return v.type == TypedValue.TYPE_STRING;
    }

    public static boolean isBrightColor(int color) {
        if (android.R.color.transparent == color) {
            return true;
        }
        // See http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
        boolean bright = false;
        int[] rgb = {Color.red(color), Color.green(color), Color.blue(color)};
        int brightness = (int) Math.sqrt(rgb[0] * rgb[0] * .241 + rgb[1] * rgb[1] * .691 + rgb[2] * rgb[2] * .068);
        if (brightness >= 210) {
            bright = true;
        }
        return bright;
    }
}
