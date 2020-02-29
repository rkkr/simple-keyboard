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

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme;
import rkr.simplekeyboard.inputmethod.latin.AudioAndHapticFeedbackManager;
import rkr.simplekeyboard.inputmethod.latin.InputAttributes;
import rkr.simplekeyboard.inputmethod.latin.utils.AdditionalSubtypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.RunInLocale;

public final class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = Settings.class.getSimpleName();
    // Settings screens
    public static final String SCREEN_THEME = "screen_theme";
    // In the same order as xml/prefs.xml
    public static final String PREF_AUTO_CAP = "auto_cap";
    public static final String PREF_VIBRATE_ON = "vibrate_on";
    public static final String PREF_SOUND_ON = "sound_on";
    public static final String PREF_POPUP_ON = "popup_on";
    //public static final boolean SHOULD_SHOW_LXX_SUGGESTION_UI =
    //        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    public static final String PREF_HIDE_LANGUAGE_SWITCH_KEY =
            "pref_hide_language_switch_key";
    public static final String PREF_CUSTOM_INPUT_STYLES = "custom_input_styles";
    public static final String PREF_VIBRATION_DURATION_SETTINGS =
            "pref_vibration_duration_settings";
    public static final String PREF_KEYPRESS_SOUND_VOLUME = "pref_keypress_sound_volume";
    public static final String PREF_KEY_LONGPRESS_TIMEOUT = "pref_key_longpress_timeout";
    public static final String PREF_KEYBOARD_HEIGHT = "pref_keyboard_height";
    public static final String PREF_KEYBOARD_COLOR = "pref_keyboard_color";
    public static final String PREF_HIDE_SPECIAL_CHARS = "pref_hide_special_chars";
    public static final String PREF_SHOW_NUMBER_ROW = "pref_show_number_row";
    public static final String PREF_SPACE_SWIPE = "pref_space_swipe";
    public static final String PREF_DELETE_SWIPE = "pref_delete_swipe";
    public static final String PREF_MATCHING_NAVBAR_COLOR = "pref_matching_navbar_color";

    private static final float UNDEFINED_PREFERENCE_VALUE_FLOAT = -1.0f;
    private static final int UNDEFINED_PREFERENCE_VALUE_INT = -1;

    private Context mContext;
    private Resources mRes;
    private SharedPreferences mPrefs;
    private SettingsValues mSettingsValues;
    private final ReentrantLock mSettingsValuesLock = new ReentrantLock();

    private static final Settings sInstance = new Settings();

    public static Settings getInstance() {
        return sInstance;
    }

    public static void init(final Context context) {
        sInstance.onCreate(context);
    }

    private Settings() {
        // Intentional empty constructor for singleton.
    }

    private void onCreate(final Context context) {
        mContext = context;
        mRes = context.getResources();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    public void onDestroy() {
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
        mSettingsValuesLock.lock();
        try {
            if (mSettingsValues == null) {
                // TODO: Introduce a static function to register this class and ensure that
                // loadSettings must be called before "onSharedPreferenceChanged" is called.
                Log.w(TAG, "onSharedPreferenceChanged called before loadSettings.");
                return;
            }
            loadSettings(mContext, mSettingsValues.mLocale, mSettingsValues.mInputAttributes);
        } finally {
            mSettingsValuesLock.unlock();
        }
    }

    public void loadSettings(final Context context, final Locale locale,
            final InputAttributes inputAttributes) {
        mSettingsValuesLock.lock();
        mContext = context;
        try {
            final SharedPreferences prefs = mPrefs;
            final RunInLocale<SettingsValues> job = new RunInLocale<SettingsValues>() {
                @Override
                protected SettingsValues job(final Resources res) {
                    return new SettingsValues(prefs, res, inputAttributes);
                }
            };
            mSettingsValues = job.runInLocale(mRes, locale);
        } finally {
            mSettingsValuesLock.unlock();
        }
    }

    // TODO: Remove this method and add proxy method to SettingsValues.
    public SettingsValues getCurrent() {
        return mSettingsValues;
    }


    // Accessed from the settings interface, hence public
    public static boolean readKeypressSoundEnabled(final SharedPreferences prefs,
            final Resources res) {
        return prefs.getBoolean(PREF_SOUND_ON,
                res.getBoolean(R.bool.config_default_sound_enabled));
    }

    public static boolean readVibrationEnabled(final SharedPreferences prefs,
            final Resources res) {
        final boolean hasVibrator = AudioAndHapticFeedbackManager.getInstance().hasVibrator();
        return hasVibrator && prefs.getBoolean(PREF_VIBRATE_ON,
                res.getBoolean(R.bool.config_default_vibration_enabled));
    }

    public static boolean readKeyPreviewPopupEnabled(final SharedPreferences prefs,
            final Resources res) {
        final boolean defaultKeyPreviewPopup = res.getBoolean(
                R.bool.config_default_key_preview_popup);
        return prefs.getBoolean(PREF_POPUP_ON, defaultKeyPreviewPopup);
    }

    public static boolean readShowsLanguageSwitchKey(final SharedPreferences prefs) {
        return !prefs.getBoolean(PREF_HIDE_LANGUAGE_SWITCH_KEY, false);
    }

    public static boolean readHideSpecialChars(final SharedPreferences prefs) {
        return prefs.getBoolean(PREF_HIDE_SPECIAL_CHARS, false);
    }

    public static boolean readShowNumberRow(final SharedPreferences prefs) {
        return prefs.getBoolean(PREF_SHOW_NUMBER_ROW, false);
    }

    public static boolean readSpaceSwipeEnabled(final SharedPreferences prefs) {
        return prefs.getBoolean(PREF_SPACE_SWIPE, true);
    }

    public static boolean readDeleteSwipeEnabled(final SharedPreferences prefs) {
        return prefs.getBoolean(PREF_DELETE_SWIPE, true);
    }

    public static String readPrefAdditionalSubtypes(final SharedPreferences prefs,
            final Resources res) {
        final String predefinedPrefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(
                res.getStringArray(R.array.predefined_subtypes));
        return prefs.getString(PREF_CUSTOM_INPUT_STYLES, predefinedPrefSubtypes);
    }

    public static void writePrefAdditionalSubtypes(final SharedPreferences prefs,
            final String prefSubtypes) {
        prefs.edit().putString(PREF_CUSTOM_INPUT_STYLES, prefSubtypes).apply();
    }

    public static float readKeypressSoundVolume(final SharedPreferences prefs,
            final Resources res) {
        final float volume = prefs.getFloat(
                PREF_KEYPRESS_SOUND_VOLUME, UNDEFINED_PREFERENCE_VALUE_FLOAT);
        return (volume != UNDEFINED_PREFERENCE_VALUE_FLOAT) ? volume
                : readDefaultKeypressSoundVolume(res);
    }

    // Default keypress sound volume for unknown devices.
    // The negative value means system default.
    private static final String DEFAULT_KEYPRESS_SOUND_VOLUME = Float.toString(-1.0f);

    public static float readDefaultKeypressSoundVolume(final Resources res) {
        return Float.parseFloat(ResourceUtils.getDeviceOverrideValue(res,
                R.array.keypress_volumes, DEFAULT_KEYPRESS_SOUND_VOLUME));
    }

    public static int readKeyLongpressTimeout(final SharedPreferences prefs,
            final Resources res) {
        final int milliseconds = prefs.getInt(
                PREF_KEY_LONGPRESS_TIMEOUT, UNDEFINED_PREFERENCE_VALUE_INT);
        return (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) ? milliseconds
                : readDefaultKeyLongpressTimeout(res);
    }

    public static int readDefaultKeyLongpressTimeout(final Resources res) {
        return res.getInteger(R.integer.config_default_longpress_key_timeout);
    }

    public static int readKeypressVibrationDuration(final SharedPreferences prefs,
            final Resources res) {
        final int milliseconds = prefs.getInt(
                PREF_VIBRATION_DURATION_SETTINGS, UNDEFINED_PREFERENCE_VALUE_INT);
        return (milliseconds != UNDEFINED_PREFERENCE_VALUE_INT) ? milliseconds
                : readDefaultKeypressVibrationDuration(res);
    }

    // Default keypress vibration duration for unknown devices.
    // The negative value means system default.
    private static final String DEFAULT_KEYPRESS_VIBRATION_DURATION = Integer.toString(-1);

    public static int readDefaultKeypressVibrationDuration(final Resources res) {
        return Integer.parseInt(ResourceUtils.getDeviceOverrideValue(res,
                R.array.keypress_vibration_durations, DEFAULT_KEYPRESS_VIBRATION_DURATION));
    }

    public static float readKeyboardHeight(final SharedPreferences prefs,
            final float defaultValue) {
        return prefs.getFloat(PREF_KEYBOARD_HEIGHT, defaultValue);
    }

    public static int readKeyboardDefaultColor(final Context context) {
        final int[] keyboardThemeColors = context.getResources().getIntArray(R.array.keyboard_theme_colors);
        final int[] keyboardThemeIds = context.getResources().getIntArray(R.array.keyboard_theme_ids);
        final int themeId = KeyboardTheme.getKeyboardTheme(context).mThemeId;
        for (int index = 0; index < keyboardThemeIds.length; index++) {
            if (themeId == keyboardThemeIds[index]) {
                return keyboardThemeColors[index];
            }
        }

        return Color.LTGRAY;
    }

    public static int readKeyboardColor(final SharedPreferences prefs, final Context context) {
        return prefs.getInt(PREF_KEYBOARD_COLOR, readKeyboardDefaultColor(context));
    }

    public static void removeKeyboardColor(final SharedPreferences prefs) {
        prefs.edit().remove(PREF_KEYBOARD_COLOR).apply();
    }

    public static boolean readUseFullscreenMode(final Resources res) {
        return res.getBoolean(R.bool.config_use_fullscreen_mode);
    }

    public static boolean readHasHardwareKeyboard(final Configuration conf) {
        // The standard way of finding out whether we have a hardware keyboard. This code is taken
        // from InputMethodService#onEvaluateInputShown, which canonically determines this.
        // In a nutshell, we have a keyboard if the configuration says the type of hardware keyboard
        // is NOKEYS and if it's not hidden (e.g. folded inside the device).
        return conf.keyboard != Configuration.KEYBOARD_NOKEYS
                && conf.hardKeyboardHidden != Configuration.HARDKEYBOARDHIDDEN_YES;
    }

    public static boolean readUseMatchingNavbarColor(final SharedPreferences prefs) {
        return prefs.getBoolean(PREF_MATCHING_NAVBAR_COLOR, false);
    }
}
