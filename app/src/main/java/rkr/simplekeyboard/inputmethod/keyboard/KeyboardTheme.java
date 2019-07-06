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

package rkr.simplekeyboard.inputmethod.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.settings.Settings;

public final class KeyboardTheme {
    private static final String TAG = KeyboardTheme.class.getSimpleName();

    static final String KEYBOARD_THEME_KEY = "pref_keyboard_theme_20140509";

    // These should be aligned with Keyboard.themeId and Keyboard.Case.keyboardTheme
    // attributes' values in attrs.xml.
    public static final int THEME_ID_LIGHT_BORDER = 1;
    public static final int THEME_ID_DARK_BORDER = 2;
    public static final int THEME_ID_LIGHT = 3;
    public static final int THEME_ID_DARK = 4;
    public static final int DEFAULT_THEME_ID = THEME_ID_LIGHT;

    /* package private for testing */
    static final KeyboardTheme[] KEYBOARD_THEMES = {
        new KeyboardTheme(THEME_ID_LIGHT, "LXXLight", R.style.KeyboardTheme_LXX_Light),
        new KeyboardTheme(THEME_ID_DARK, "LXXDark", R.style.KeyboardTheme_LXX_Dark),
        new KeyboardTheme(THEME_ID_LIGHT_BORDER, "LXXLightBorder", R.style.KeyboardTheme_LXX_Light_Border),
        new KeyboardTheme(THEME_ID_DARK_BORDER, "LXXDarkBorder", R.style.KeyboardTheme_LXX_Dark_Border),
    };

    public final int mThemeId;
    public final int mStyleId;
    public final String mThemeName;

    // Note: The themeId should be aligned with "themeId" attribute of Keyboard style
    // in values/themes-<style>.xml.
    private KeyboardTheme(final int themeId, final String themeName, final int styleId) {
        mThemeId = themeId;
        mThemeName = themeName;
        mStyleId = styleId;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        return (o instanceof KeyboardTheme) && ((KeyboardTheme)o).mThemeId == mThemeId;
    }

    @Override
    public int hashCode() {
        return mThemeId;
    }

    /* package private for testing */
    static KeyboardTheme searchKeyboardThemeById(final int themeId) {
        // TODO: This search algorithm isn't optimal if there are many themes.
        for (final KeyboardTheme theme : KEYBOARD_THEMES) {
            if (theme.mThemeId == themeId) {
                return theme;
            }
        }
        return null;
    }

    /* package private for testing */
    static KeyboardTheme getDefaultKeyboardTheme() {
        return searchKeyboardThemeById(DEFAULT_THEME_ID);
    }

    public static String getKeyboardThemeName(final int themeId) {
        final KeyboardTheme theme = searchKeyboardThemeById(themeId);
        Log.i("Getting theme ID", Integer.toString(themeId));
        return theme.mThemeName;
    }

    public static void saveKeyboardThemeId(final int themeId, final SharedPreferences prefs) {
        prefs.edit().putString(KEYBOARD_THEME_KEY, Integer.toString(themeId)).apply();
    }

    public static KeyboardTheme getKeyboardTheme(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String themeIdString = prefs.getString(KEYBOARD_THEME_KEY, null);
        if (themeIdString == null) {
            return searchKeyboardThemeById(THEME_ID_LIGHT);
        }
        try {
            final int themeId = Integer.parseInt(themeIdString);
            final KeyboardTheme theme = searchKeyboardThemeById(themeId);
            if (theme != null) {
                return theme;
            }
            Log.w(TAG, "Unknown keyboard theme in preference: " + themeIdString);
        } catch (final NumberFormatException e) {
            Log.w(TAG, "Illegal keyboard theme in preference: " + themeIdString, e);
        }
        // Remove preference that contains unknown or illegal theme id.
        prefs.edit().remove(KEYBOARD_THEME_KEY).remove(Settings.PREF_KEYBOARD_COLOR).apply();
        return getDefaultKeyboardTheme();
    }
}
