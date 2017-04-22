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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.inputmethod.EditorInfo;

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
    public final long mDoubleSpacePeriodTimeout;
    // From configuration:
    public final Locale mLocale;
    public final boolean mHasHardwareKeyboard;
    public final int mDisplayOrientation;
    // From preferences, in the same order as xml/prefs.xml:
    public final boolean mAutoCap;
    public final boolean mVibrateOn;
    public final boolean mSoundOn;
    public final boolean mKeyPreviewPopupOn;
    public final boolean mShowsVoiceInputKey = false;
    public final boolean mIncludesOtherImesInLanguageSwitchList;
    public final boolean mShowsLanguageSwitchKey;
    public final boolean mUseDoubleSpacePeriod;
    public final int mKeyLongpressTimeout;
    public final boolean mShouldShowLxxSuggestionUi;
    // Use split layout for keyboard.
    public final boolean mIsSplitKeyboardEnabled;

    // From the input box
    @NonNull
    public final InputAttributes mInputAttributes;

    // Deduced settings
    public final int mKeypressVibrationDuration;
    public final float mKeypressSoundVolume;
    public final int mKeyPreviewPopupDismissDelay;

    // Debug settings
    public final boolean mIsInternal;
    public final boolean mHasKeyboardResize;
    public final float mKeyboardHeightScale;
    public final int mKeyPreviewShowUpDuration;
    public final int mKeyPreviewDismissDuration;
    public final float mKeyPreviewShowUpStartXScale;
    public final float mKeyPreviewShowUpStartYScale;
    public final float mKeyPreviewDismissEndXScale;
    public final float mKeyPreviewDismissEndYScale;

    public SettingsValues(final Context context, final SharedPreferences prefs, final Resources res,
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
        mIncludesOtherImesInLanguageSwitchList = !Settings.ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS || prefs.getBoolean(Settings.PREF_INCLUDE_OTHER_IMES_IN_LANGUAGE_SWITCH_LIST, false) /* forcibly */;
        mShowsLanguageSwitchKey = !Settings.ENABLE_SHOW_LANGUAGE_SWITCH_KEY_SETTINGS || Settings.readShowsLanguageSwitchKey(prefs) /* forcibly */;
        mUseDoubleSpacePeriod = prefs.getBoolean(Settings.PREF_KEY_USE_DOUBLE_SPACE_PERIOD, true)
                && inputAttributes.mIsGeneralTextInput;
        mDoubleSpacePeriodTimeout = res.getInteger(R.integer.config_double_space_period_timeout);
        mHasHardwareKeyboard = Settings.readHasHardwareKeyboard(res.getConfiguration());
        mIsSplitKeyboardEnabled = prefs.getBoolean(Settings.PREF_ENABLE_SPLIT_KEYBOARD, false);

        mShouldShowLxxSuggestionUi = Settings.SHOULD_SHOW_LXX_SUGGESTION_UI;
        // Compute other readable settings
        mKeyLongpressTimeout = Settings.readKeyLongpressTimeout(prefs, res);
        mKeypressVibrationDuration = Settings.readKeypressVibrationDuration(prefs, res);
        mKeypressSoundVolume = Settings.readKeypressSoundVolume(prefs, res);
        mKeyPreviewPopupDismissDelay = Settings.readKeyPreviewPopupDismissDelay(prefs, res);
        mIsInternal = Settings.isInternal(prefs);
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
    }

    public boolean isWordSeparator(final int code) {
        return mSpacingAndPunctuations.isWordSeparator(code);
    }

    public boolean isWordConnector(final int code) {
        return mSpacingAndPunctuations.isWordConnector(code);
    }

    public boolean isWordCodePoint(final int code) {
        return Character.isLetter(code) || isWordConnector(code)
                || Character.COMBINING_SPACING_MARK == Character.getType(code);
    }

    public boolean isUsuallyPrecededBySpace(final int code) {
        return mSpacingAndPunctuations.isUsuallyPrecededBySpace(code);
    }

    public boolean isUsuallyFollowedBySpace(final int code) {
        return mSpacingAndPunctuations.isUsuallyFollowedBySpace(code);
    }

    public boolean shouldInsertSpacesAutomatically() {
        return mInputAttributes.mShouldInsertSpacesAutomatically;
    }

    public boolean isLanguageSwitchKeyEnabled() {
        if (!mShowsLanguageSwitchKey) {
            return false;
        }
        final RichInputMethodManager imm = RichInputMethodManager.getInstance();
        if (mIncludesOtherImesInLanguageSwitchList) {
            return imm.hasMultipleEnabledIMEsOrSubtypes(false /* include aux subtypes */);
        }
        return imm.hasMultipleEnabledSubtypesInThisIme(false /* include aux subtypes */);
    }

    public boolean isSameInputType(final EditorInfo editorInfo) {
        return mInputAttributes.isSameInputType(editorInfo);
    }

    public boolean hasSameOrientation(final Configuration configuration) {
        return mDisplayOrientation == configuration.orientation;
    }

    public String dump() {
        final StringBuilder sb = new StringBuilder("Current settings :");
        sb.append("\n   mSpacingAndPunctuations = ");
        sb.append("" + mSpacingAndPunctuations.dump());
        sb.append("\n   mAutoCap = ");
        sb.append("" + mAutoCap);
        sb.append("\n   mVibrateOn = ");
        sb.append("" + mVibrateOn);
        sb.append("\n   mSoundOn = ");
        sb.append("" + mSoundOn);
        sb.append("\n   mKeyPreviewPopupOn = ");
        sb.append("" + mKeyPreviewPopupOn);
        sb.append("\n   mShowsVoiceInputKey = ");
        sb.append("" + mShowsVoiceInputKey);
        sb.append("\n   mIncludesOtherImesInLanguageSwitchList = ");
        sb.append("" + mIncludesOtherImesInLanguageSwitchList);
        sb.append("\n   mShowsLanguageSwitchKey = ");
        sb.append("" + mShowsLanguageSwitchKey);
        sb.append("\n   mUseDoubleSpacePeriod = ");
        sb.append("" + mUseDoubleSpacePeriod);
        sb.append("\n   mKeyLongpressTimeout = ");
        sb.append("" + mKeyLongpressTimeout);
        sb.append("\n   mLocale = ");
        sb.append("" + mLocale);
        sb.append("\n   mInputAttributes = ");
        sb.append("" + mInputAttributes);
        sb.append("\n   mKeypressVibrationDuration = ");
        sb.append("" + mKeypressVibrationDuration);
        sb.append("\n   mKeypressSoundVolume = ");
        sb.append("" + mKeypressSoundVolume);
        sb.append("\n   mKeyPreviewPopupDismissDelay = ");
        sb.append("" + mKeyPreviewPopupDismissDelay);
        sb.append("\n   mAutoCorrectionEnabledPerUserSettings = ");
        sb.append("\n   mDisplayOrientation = ");
        sb.append("" + mDisplayOrientation);
        sb.append("\n   mIsInternal = ");
        sb.append("" + mIsInternal);
        sb.append("\n   mKeyPreviewShowUpDuration = ");
        sb.append("" + mKeyPreviewShowUpDuration);
        sb.append("\n   mKeyPreviewDismissDuration = ");
        sb.append("" + mKeyPreviewDismissDuration);
        sb.append("\n   mKeyPreviewShowUpStartScaleX = ");
        sb.append("" + mKeyPreviewShowUpStartXScale);
        sb.append("\n   mKeyPreviewShowUpStartScaleY = ");
        sb.append("" + mKeyPreviewShowUpStartYScale);
        sb.append("\n   mKeyPreviewDismissEndScaleX = ");
        sb.append("" + mKeyPreviewDismissEndXScale);
        sb.append("\n   mKeyPreviewDismissEndScaleY = ");
        sb.append("" + mKeyPreviewDismissEndYScale);
        return sb.toString();
    }
}
