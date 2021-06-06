/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.text.InputType;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import java.util.Arrays;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.compat.EditorInfoCompatUtils;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils;

/**
 * Unique identifier for each keyboard type.
 */
public final class KeyboardId {
    public static final int MODE_TEXT = 0;
    public static final int MODE_URL = 1;
    public static final int MODE_EMAIL = 2;
    public static final int MODE_IM = 3;
    public static final int MODE_PHONE = 4;
    public static final int MODE_NUMBER = 5;
    public static final int MODE_DATE = 6;
    public static final int MODE_TIME = 7;
    public static final int MODE_DATETIME = 8;

    public static final int ELEMENT_ALPHABET = 0;
    public static final int ELEMENT_ALPHABET_MANUAL_SHIFTED = 1;
    public static final int ELEMENT_ALPHABET_AUTOMATIC_SHIFTED = 2;
    public static final int ELEMENT_ALPHABET_SHIFT_LOCKED = 3;
    public static final int ELEMENT_SYMBOLS = 5;
    public static final int ELEMENT_SYMBOLS_SHIFTED = 6;
    public static final int ELEMENT_PHONE = 7;
    public static final int ELEMENT_PHONE_SYMBOLS = 8;
    public static final int ELEMENT_NUMBER = 9;

    public final Subtype mSubtype;
    public final int mThemeId;
    public final int mWidth;
    public final int mHeight;
    public final int mMode;
    public final int mElementId;
    public final EditorInfo mEditorInfo;
    public final boolean mLanguageSwitchKeyEnabled;
    public final String mCustomActionLabel;
    public final boolean mShowMoreKeys;
    public final boolean mShowNumberRow;

    private final int mHashCode;

    public KeyboardId(final int elementId, final KeyboardLayoutSet.Params params) {
        mSubtype = params.mSubtype;
        mThemeId = params.mKeyboardThemeId;
        mWidth = params.mKeyboardWidth;
        mHeight = params.mKeyboardHeight;
        mMode = params.mMode;
        mElementId = elementId;
        mEditorInfo = params.mEditorInfo;
        mLanguageSwitchKeyEnabled = params.mLanguageSwitchKeyEnabled;
        mCustomActionLabel = (mEditorInfo.actionLabel != null)
                ? mEditorInfo.actionLabel.toString() : null;
        mShowMoreKeys = params.mShowMoreKeys;
        mShowNumberRow = params.mShowNumberRow;

        mHashCode = computeHashCode(this);
    }

    private static int computeHashCode(final KeyboardId id) {
        return Arrays.hashCode(new Object[] {
                id.mElementId,
                id.mMode,
                id.mWidth,
                id.mHeight,
                id.passwordInput(),
                id.mLanguageSwitchKeyEnabled,
                id.isMultiLine(),
                id.imeAction(),
                id.mCustomActionLabel,
                id.navigateNext(),
                id.navigatePrevious(),
                id.mSubtype,
                id.mThemeId
        });
    }

    private boolean equals(final KeyboardId other) {
        if (other == this)
            return true;
        return other.mElementId == mElementId
                && other.mMode == mMode
                && other.mWidth == mWidth
                && other.mHeight == mHeight
                && other.passwordInput() == passwordInput()
                && other.mLanguageSwitchKeyEnabled == mLanguageSwitchKeyEnabled
                && other.isMultiLine() == isMultiLine()
                && other.imeAction() == imeAction()
                && TextUtils.equals(other.mCustomActionLabel, mCustomActionLabel)
                && other.navigateNext() == navigateNext()
                && other.navigatePrevious() == navigatePrevious()
                && other.mSubtype.equals(mSubtype)
                && other.mThemeId == mThemeId;
    }

    private static boolean isAlphabetKeyboard(final int elementId) {
        return elementId < ELEMENT_SYMBOLS;
    }

    public boolean isAlphabetKeyboard() {
        return isAlphabetKeyboard(mElementId);
    }

    public boolean navigateNext() {
        return (mEditorInfo.imeOptions & EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0
                || imeAction() == EditorInfo.IME_ACTION_NEXT;
    }

    public boolean navigatePrevious() {
        return (mEditorInfo.imeOptions & EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS) != 0
                || imeAction() == EditorInfo.IME_ACTION_PREVIOUS;
    }

    public boolean passwordInput() {
        final int inputType = mEditorInfo.inputType;
        return InputTypeUtils.isPasswordInputType(inputType)
                || InputTypeUtils.isVisiblePasswordInputType(inputType);
    }

    public boolean isMultiLine() {
        return (mEditorInfo.inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0;
    }

    public int imeAction() {
        return InputTypeUtils.getImeOptionsActionIdFromEditorInfo(mEditorInfo);
    }

    public Locale getLocale() {
        return mSubtype.getLocaleObject();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof KeyboardId && equals((KeyboardId) other);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "[%s %s:%s %dx%d %s %s%s%s%s%s%s %s]",
                elementIdToName(mElementId),
                mSubtype.getLocale(),
                mSubtype.getKeyboardLayoutSet(),
                mWidth, mHeight,
                modeName(mMode),
                actionName(imeAction()),
                (navigateNext() ? " navigateNext" : ""),
                (navigatePrevious() ? " navigatePrevious" : ""),
                (passwordInput() ? " passwordInput" : ""),
                (mLanguageSwitchKeyEnabled ? " languageSwitchKeyEnabled" : ""),
                (isMultiLine() ? " isMultiLine" : ""),
                KeyboardTheme.getKeyboardThemeName(mThemeId)
        );
    }

    public static boolean equivalentEditorInfoForKeyboard(final EditorInfo a, final EditorInfo b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.inputType == b.inputType
                && a.imeOptions == b.imeOptions
                && TextUtils.equals(a.privateImeOptions, b.privateImeOptions);
    }

    public static String elementIdToName(final int elementId) {
        switch (elementId) {
        case ELEMENT_ALPHABET: return "alphabet";
        case ELEMENT_ALPHABET_MANUAL_SHIFTED: return "alphabetManualShifted";
        case ELEMENT_ALPHABET_AUTOMATIC_SHIFTED: return "alphabetAutomaticShifted";
        case ELEMENT_ALPHABET_SHIFT_LOCKED: return "alphabetShiftLocked";
        case ELEMENT_SYMBOLS: return "symbols";
        case ELEMENT_SYMBOLS_SHIFTED: return "symbolsShifted";
        case ELEMENT_PHONE: return "phone";
        case ELEMENT_PHONE_SYMBOLS: return "phoneSymbols";
        case ELEMENT_NUMBER: return "number";
        default: return null;
        }
    }

    public static String modeName(final int mode) {
        switch (mode) {
        case MODE_TEXT: return "text";
        case MODE_URL: return "url";
        case MODE_EMAIL: return "email";
        case MODE_IM: return "im";
        case MODE_PHONE: return "phone";
        case MODE_NUMBER: return "number";
        case MODE_DATE: return "date";
        case MODE_TIME: return "time";
        case MODE_DATETIME: return "datetime";
        default: return null;
        }
    }

    public static String actionName(final int actionId) {
        return (actionId == InputTypeUtils.IME_ACTION_CUSTOM_LABEL) ? "actionCustomLabel"
                : EditorInfoCompatUtils.imeActionName(actionId);
    }
}
