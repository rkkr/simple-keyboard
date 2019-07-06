/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.InputAttributes;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils;

/**
 * When you call the constructor of this class, you may want to change the current system locale by
 * using {@link rkr.simplekeyboard.inputmethod.latin.utils.RunInLocale}.
 */
// Non-final for testing via mock library.
public class SettingsValues {
    public static final float DEFAULT_SIZE_SCALE = 1.0f; // 100%

    // From resources:
    public final SpacingAndPunctuations mSpacingAndPunctuations;
    // From configuration:
    public final Locale mLocale;
    public final boolean mHasHardwareKeyboard;
    public final int mDisplayOrientation;
    // From preferences, in the same order as xml/prefs.xml:
    public final boolean mAutoCap;
    public final boolean mVibrateOn;
    public final boolean mSoundOn;
    public final boolean mKeyPreviewPopupOn;
    public final boolean mShowsLanguageSwitchKey;
    public final int mKeyLongpressTimeout;
    //public final boolean mShouldShowLxxSuggestionUi;
    public final boolean mHideSpecialChars;
    public final boolean mShowNumberRow;
    public final boolean mSpaceSwipeEnabled;

    // From the input box
    @NonNull
    public final InputAttributes mInputAttributes;

    // Deduced settings
    public final int mKeypressVibrationDuration;
    public final float mKeypressSoundVolume;
    public final int mKeyPreviewPopupDismissDelay;

    // Debug settings
    public final boolean mHasKeyboardResize;
    public final float mKeyboardHeightScale;
    public final int mKeyPreviewShowUpDuration;
    public final int mKeyPreviewDismissDuration;
    public final float mKeyPreviewShowUpStartXScale;
    public final float mKeyPreviewShowUpStartYScale;
    public final float mKeyPreviewDismissEndXScale;
    public final float mKeyPreviewDismissEndYScale;

    public SettingsValues(final SharedPreferences prefs, final Resources res,
            @NonNull final InputAttributes inputAttributes) {
        mLocale = res.getConfiguration().locale;
        // Get the resources
        mSpacingAndPunctuations = new SpacingAndPunctuations(res);

        // Store the input attributes
        mInputAttributes = inputAttributes;

        // Get the settings preferences
        mAutoCap = prefs.getBoolean(Settings.PREF_AUTO_CAP, true);
        mVibrateOn = Settings.readVibrationEnabled(prefs, res);
        mSoundOn = Settings.readKeypressSoundEnabled(prefs, res);
        mKeyPreviewPopupOn = Settings.readKeyPreviewPopupEnabled(prefs, res);
        mShowsLanguageSwitchKey = Settings.readShowsLanguageSwitchKey(prefs);
        mHasHardwareKeyboard = Settings.readHasHardwareKeyboard(res.getConfiguration());

        // Compute other readable settings
        mKeyLongpressTimeout = Settings.readKeyLongpressTimeout(prefs, res);
        mKeypressVibrationDuration = Settings.readKeypressVibrationDuration(prefs, res);
        mKeypressSoundVolume = Settings.readKeypressSoundVolume(prefs, res);
        mKeyPreviewPopupDismissDelay = Settings.readKeyPreviewPopupDismissDelay(prefs, res);
        mHasKeyboardResize = true;
        mKeyboardHeightScale = Settings.readKeyboardHeight(prefs, DEFAULT_SIZE_SCALE);
        mKeyPreviewShowUpDuration = res.getInteger(R.integer.config_key_preview_show_up_duration);
        mKeyPreviewDismissDuration = res.getInteger(R.integer.config_key_preview_dismiss_duration);
        final float defaultKeyPreviewShowUpStartScale = ResourceUtils.getFloatFromFraction(
                res, R.fraction.config_key_preview_show_up_start_scale);
        final float defaultKeyPreviewDismissEndScale = ResourceUtils.getFloatFromFraction(
                res, R.fraction.config_key_preview_dismiss_end_scale);
        mKeyPreviewShowUpStartXScale = defaultKeyPreviewShowUpStartScale;
        mKeyPreviewShowUpStartYScale = defaultKeyPreviewShowUpStartScale;
        mKeyPreviewDismissEndXScale = defaultKeyPreviewDismissEndScale;
        mKeyPreviewDismissEndYScale = defaultKeyPreviewDismissEndScale;
        mDisplayOrientation = res.getConfiguration().orientation;
        mHideSpecialChars = Settings.readHideSpecialChars(prefs);
        mShowNumberRow = Settings.readShowNumberRow(prefs);
        mSpaceSwipeEnabled = Settings.readSpaceSwipeEnabled(prefs);
    }

    public boolean isWordSeparator(final int code) {
        return mSpacingAndPunctuations.isWordSeparator(code);
    }

    public boolean isLanguageSwitchKeyEnabled() {
        if (!mShowsLanguageSwitchKey) {
            return false;
        }
        final RichInputMethodManager imm = RichInputMethodManager.getInstance();
        return imm.hasMultipleEnabledIMEsOrSubtypes(false /* include aux subtypes */);
    }

    public boolean isSameInputType(final EditorInfo editorInfo) {
        return mInputAttributes.isSameInputType(editorInfo);
    }

    public boolean hasSameOrientation(final Configuration configuration) {
        return mDisplayOrientation == configuration.orientation;
    }
}
