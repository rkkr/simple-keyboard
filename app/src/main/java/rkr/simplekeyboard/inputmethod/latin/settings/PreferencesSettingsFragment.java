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

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardLayoutSet;

/**
 * "Preferences" settings sub screen.
 *
 * This settings sub screen handles the following input preferences.
 * - Auto-capitalization
 * - Show separate number row
 * - Hide special characters
 * - Hide language switch key
 * - Switch to other keyboards
 * - Space swipe cursor move
 * - Delete swipe
 */
public final class PreferencesSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_preferences);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            removePreference(Settings.PREF_ENABLE_IME_SWITCH);
        }
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        if (key.equals(Settings.PREF_HIDE_SPECIAL_CHARS) ||
                key.equals(Settings.PREF_SHOW_NUMBER_ROW)) {
            KeyboardLayoutSet.onKeyboardThemeChanged();
        }
    }
}
