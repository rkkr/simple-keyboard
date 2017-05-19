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
import android.os.Bundle;
import android.preference.SwitchPreference;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.define.ProductionFlags;

/**
 * "Appearance" settings sub screen.
 */
public final class AppearanceSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_appearance);
        if (!ProductionFlags.IS_SPLIT_KEYBOARD_SUPPORTED ||
                Constants.isPhone(Settings.readScreenMetrics(getResources()))) {
            removePreference(Settings.PREF_ENABLE_SPLIT_KEYBOARD);
        }

        refreshEnablingsOfCustomColorSettings();
        setupKeyboardColorSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        CustomInputStyleSettingsFragment.updateCustomInputStylesSummary(
                findPreference(Settings.PREF_CUSTOM_INPUT_STYLES));
        ThemeSettingsFragment.updateKeyboardThemeSummary(findPreference(Settings.SCREEN_THEME));
    }

    private void refreshEnablingsOfCustomColorSettings() {
        final SharedPreferences prefs = getSharedPreferences();
        setPreferenceEnabled(Settings.PREF_KEYBOARD_COLOR,
                Settings.readCustomColorEnabledEnabled(prefs));
        int themeId = KeyboardTheme.getKeyboardTheme(this.getActivity().getApplicationContext()).mThemeId;
        setPreferenceEnabled(Settings.PREF_KEYBOARD_COLOR_ENABLED,
                themeId == KeyboardTheme.THEME_ID_LXX_DARK || themeId == KeyboardTheme.THEME_ID_LXX_LIGHT);
        if (themeId == KeyboardTheme.THEME_ID_ICS || themeId == KeyboardTheme.THEME_ID_KLP && Settings.readCustomColorEnabledEnabled(prefs))
            ((SwitchPreference)findPreference(Settings.PREF_KEYBOARD_COLOR_ENABLED)).setChecked(false);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        refreshEnablingsOfCustomColorSettings();
    }

    private void setupKeyboardColorSettings() {
        final ColorDialogPreference pref = (ColorDialogPreference)findPreference(
                Settings.PREF_KEYBOARD_COLOR);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new ColorDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public int readValue(final String key) {
                return Settings.readKeyboardColor(prefs);
            }

            @Override
            public String getValueText(final int value) {
                String temp = Integer.toHexString(value);
                for (; temp.length() < 8; temp = "0" + temp);
                return temp.substring(2).toUpperCase();
            }
        });
    }
}
