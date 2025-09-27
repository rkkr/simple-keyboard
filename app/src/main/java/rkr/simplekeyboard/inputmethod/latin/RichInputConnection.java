/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2024 wittmane
 * Copyright (C) 2019 Emmanuel
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

package rkr.simplekeyboard.inputmethod.latin;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.settings.SpacingAndPunctuations;
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils;

/**
 * Enrichment class for InputConnection to simplify interaction and add functionality.
 *
 * This class serves as a wrapper to be able to simply add hooks to any calls to the underlying
 * InputConnection. It also keeps track of a number of things to avoid having to call upon IPC
 * all the time to find out what text is in the buffer, when we need it to determine caps mode
 * for example.
 */
public final class RichInputConnection {
    private static final String TAG = "RichInputConnection";
    private static final int INVALID_CURSOR_POSITION = -1;

    /**
     * This variable contains an expected value for the selection start position. This is where the
     * cursor or selection start may end up after all the keyboard-triggered updates have passed. We
     * keep this to compare it to the actual selection start to guess whether the move was caused by
     * a keyboard command or not.
     * It's not really the selection start position: the selection start may not be there yet, and
     * in some cases, it may never arrive there.
     */
    private int mExpectedSelStart = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * The expected selection end.  Only differs from mExpectedSelStart if a non-empty selection is
     * expected.  The same caveats as mExpectedSelStart apply.
     */
    private int mExpectedSelEnd = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * This contains the committed text immediately preceding the cursor and the composing
     * text, if any. It is refreshed when the cursor moves by calling upon the TextView.
     */
    private String mTextBeforeCursor = "";
    private String mTextAfterCursor = "";
    private String mTextSelection = "";

    private final LatinIME mLatinIME;
    private InputConnection mIC;
    private int mNestLevel;
    private final ExecutorService mBackgroundThread;

    public RichInputConnection(final LatinIME latinIME) {
        mLatinIME = latinIME;
        mIC = null;
        mNestLevel = 0;
        mBackgroundThread = Executors.newSingleThreadExecutor();
    }

    public boolean isConnected() {
        return mIC != null;
    }

    public void beginBatchEdit() {
        if (++mNestLevel == 1) {
            mIC = mLatinIME.getCurrentInputConnection();
            if (isConnected()) {
                mIC.beginBatchEdit();
            }
        } else {
            Log.e(TAG, "Nest level too deep : " + mNestLevel);
        }
    }

    public void endBatchEdit() {
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!"); // TODO: exception instead
        if (--mNestLevel == 0 && isConnected()) {
            mIC.endBatchEdit();
        }
    }

    public void updateSelection(final int newSelStart, final int newSelEnd) {
        mExpectedSelStart = newSelStart;
        mExpectedSelEnd = newSelEnd;
    }

    @TargetApi(Build.VERSION_CODES.S)
    private void setTextAroundCursor(final SurroundingText textAroundCursor) {
        if (null == textAroundCursor) {
            Log.e(TAG, "Unable get text around cursor.");
            mTextBeforeCursor = "";
            mTextAfterCursor = "";
            mTextSelection = "";
            return;
        }
        final CharSequence text = textAroundCursor.getText();
        mTextBeforeCursor = text.subSequence(0, textAroundCursor.getSelectionStart()).toString();
        mTextSelection = text.subSequence(textAroundCursor.getSelectionStart(), textAroundCursor.getSelectionEnd()).toString();
        mTextAfterCursor = text.subSequence(textAroundCursor.getSelectionEnd(), text.length()).toString();
    }

    /**
     * Reload the cached text from the EditorInfo.
     */
    public void reloadTextCache(final EditorInfo editorInfo, final boolean restarting) {
        mIC = mLatinIME.getCurrentInputConnection();

        if (mExpectedSelStart != INVALID_CURSOR_POSITION && mExpectedSelEnd != INVALID_CURSOR_POSITION
            && !restarting) {
            // Updated by onUpdateSelection, don't override as editorInfo might be invalid
            // If restarting, onStartInputView was called instead of onUpdateSelection
            return;
        }
        updateSelection(editorInfo.initialSelStart, editorInfo.initialSelEnd);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final SurroundingText textAroundCursor = editorInfo
                    .getInitialSurroundingText(Constants.EDITOR_CONTENTS_CACHE_SIZE, Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
            setTextAroundCursor(textAroundCursor);
            mLatinIME.mHandler.postUpdateShiftState();
        } else {
            reloadTextCache();
        }
    }

    /**
     * Reload the cached text from the InputConnection.
     */
    public void reloadTextCache() {
        mIC = mLatinIME.getCurrentInputConnection();
        if (!isConnected()) {
            return;
        }
        // To check if selection changed before text was retrieved
        final int expectedSelStart = mExpectedSelStart;
        final int expectedSelEnd = mExpectedSelEnd;

        mBackgroundThread.execute(() -> {
            if (!isConnected()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                final SurroundingText textAroundCursor =
                        mIC.getSurroundingText(Constants.EDITOR_CONTENTS_CACHE_SIZE, Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                if (expectedSelStart != mExpectedSelStart || expectedSelEnd != mExpectedSelEnd) {
                    Log.w(TAG, "Selection range modified before thread completion.");
                    return;
                }
                setTextAroundCursor(textAroundCursor);

                // All callbacks that need text before cursor are here
                mLatinIME.mHandler.postUpdateShiftState();
            } else {
                final CharSequence textBeforeCursor = mIC.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                if (expectedSelStart != mExpectedSelStart) {
                    Log.w(TAG, "Selection start modified before thread completion.");
                    return;
                }
                if (null == textBeforeCursor) {
                    Log.e(TAG, "Unable get text before cursor.");
                    mTextBeforeCursor = "";
                    return;
                } else {
                    mTextBeforeCursor = textBeforeCursor.toString();
                }

                // All callbacks that need text before cursor are here
                mLatinIME.mHandler.postUpdateShiftState();

                final CharSequence textAfterCursor = mIC.getTextAfterCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                if (expectedSelEnd != mExpectedSelEnd) {
                    Log.w(TAG, "Selection end modified before thread completion.");
                    return;
                }
                if (null == textAfterCursor) {
                    Log.e(TAG, "Unable get text after cursor.");
                    mTextAfterCursor = "";
                } else {
                    mTextAfterCursor = textAfterCursor.toString();
                }
                if (hasSelection()) {
                    final CharSequence textSelection = mIC.getSelectedText(0);
                    if (expectedSelStart != mExpectedSelStart || expectedSelEnd != mExpectedSelEnd) {
                        Log.w(TAG, "Selection range modified before thread completion.");
                        return;
                    }
                    if (null == textSelection) {
                        Log.e(TAG, "Unable get text selection.");
                        mTextSelection = "";
                    } else {
                        mTextSelection = textSelection.toString();
                    }
                } else {
                    mTextSelection = "";
                }
            }
        });
    }

    public void clearCaches() {
        Log.i(TAG, "Clearing text caches.");
        mExpectedSelStart = INVALID_CURSOR_POSITION;
        mExpectedSelEnd = INVALID_CURSOR_POSITION;
        mTextBeforeCursor = "";
        mTextSelection = "";
        mTextAfterCursor = "";
    }

    /**
     * Calls {@link InputConnection#commitText(CharSequence, int)}.
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    public void commitText(final CharSequence text, final int newCursorPosition) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();
        mTextBeforeCursor += text;
        // TODO: the following is exceedingly error-prone. Right now when the cursor is in the
        // middle of the composing word mComposingText only holds the part of the composing text
        // that is before the cursor, so this actually works, but it's terribly confusing. Fix this.
        if (hasCursorPosition()) {
            mExpectedSelStart += text.length();
            mExpectedSelEnd = mExpectedSelStart;
        }
        if (isConnected()) {
            mIC.commitText(text, newCursorPosition);
        }
    }

    public CharSequence getSelectedText() {
        return mTextSelection;
    }

    public boolean canDeleteCharacters() {
        return mExpectedSelStart > 0;
    }

    /**
     * Gets the caps modes we should be in after this specific string.
     *
     * This returns a bit set of TextUtils#CAP_MODE_*, masked by the inputType argument.
     * This method also supports faking an additional space after the string passed in argument,
     * to support cases where a space will be added automatically, like in phantom space
     * state for example.
     * Note that for English, we are using American typography rules (which are not specific to
     * American English, it's just the most common set of rules for English).
     *
     * @param inputType a mask of the caps modes to test for.
     * @param spacingAndPunctuations the values of the settings to use for locale and separators.
     * @return the caps modes that should be on as a set of bits
     */
    public int getCursorCapsMode(final int inputType, final SpacingAndPunctuations spacingAndPunctuations) {
        mIC = mLatinIME.getCurrentInputConnection();
        if (!isConnected()) {
            return Constants.TextUtils.CAP_MODE_OFF;
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        // TODO: don't call #toString() here. Instead, all accesses to
        // mCommittedTextBeforeComposingText should be done on the main thread.
        return CapsModeUtils.getCapsMode(mTextBeforeCursor, inputType,
                spacingAndPunctuations);
    }

    public int getCodePointBeforeCursor() {
        final int length = mTextBeforeCursor.length();
        if (length < 1) return Constants.NOT_A_CODE;
        return Character.codePointBefore(mTextBeforeCursor, length);
    }

    public void replaceText(final int startPosition, final int endPosition, CharSequence text) {
        if (mExpectedSelStart != mExpectedSelEnd) {
            Log.e(TAG, "replaceText called with text range selected");
            return;
        }
        if (mExpectedSelStart != startPosition) {
            Log.e(TAG, "replaceText called with range not starting with current cursor position");
            return;
        }

        final int numCharsSelected = endPosition - startPosition;
        final String textAfterCursor = mTextAfterCursor;
        if (textAfterCursor.length() < numCharsSelected) {
            Log.e(TAG, "replaceText called with range longer than current text");
            return;
        }
        mTextAfterCursor = text + textAfterCursor.substring(numCharsSelected);

        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            mIC.replaceText(startPosition, endPosition, text, 0, null);
        } else {
            mIC.deleteSurroundingText(0, numCharsSelected);
            mIC.commitText(text, 0);
        }
    }

    public void deleteTextBeforeCursor(final int numChars) {
        String textBeforeCursor = mTextBeforeCursor;
        if (!textBeforeCursor.isEmpty() && textBeforeCursor.length() >= numChars) {
            mTextBeforeCursor = textBeforeCursor.substring(0, textBeforeCursor.length() - numChars);
        }
        if (mExpectedSelStart >= numChars) {
            mExpectedSelStart -= numChars;
        }

        mIC.deleteSurroundingText(numChars, 0);
    }

    public void deleteSelectedText() {
        if (mExpectedSelStart == mExpectedSelEnd) {
            Log.e(TAG, "deleteSelectedText called with text range not selected");
            return;
        }

        beginBatchEdit();
        final int selectionLength = mExpectedSelEnd - mExpectedSelStart;
        mTextSelection = "";
        setSelection(mExpectedSelStart, mExpectedSelStart);
        mIC.deleteSurroundingText(0, selectionLength);
        endBatchEdit();
    }

    public void performEditorAction(final int actionId) {
        mIC = mLatinIME.getCurrentInputConnection();
        if (isConnected()) {
            mIC.performEditorAction(actionId);
        }
    }

    public void sendKeyEvent(final KeyEvent keyEvent) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            // This method is only called for enter or backspace when speaking to old applications
            // (target SDK <= 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)), or for digits.
            // When talking to new applications we never use this method because it's inherently
            // racy and has unpredictable results, but for backward compatibility we continue
            // sending the key events for only Enter and Backspace because some applications
            // mistakenly catch them to do some stuff.
            switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
                mTextBeforeCursor += "\n";
                if (hasCursorPosition()) {
                    mExpectedSelStart += 1;
                    mExpectedSelEnd = mExpectedSelStart;
                }
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                if (null != keyEvent.getCharacters()) {
                    mTextBeforeCursor += keyEvent.getCharacters();
                    if (hasCursorPosition()) {
                        mExpectedSelStart += keyEvent.getCharacters().length();
                        mExpectedSelEnd = mExpectedSelStart;
                    }
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                break;
            default:
                final String text = StringUtils.newSingleCodePointString(keyEvent.getUnicodeChar());
                mTextBeforeCursor += text;
                if (hasCursorPosition()) {
                    mExpectedSelStart += text.length();
                    mExpectedSelEnd = mExpectedSelStart;
                }
                break;
            }
        }
        if (isConnected()) {
            mIC.sendKeyEvent(keyEvent);
        }
    }

    /**
     * Set the selection of the text editor.
     *
     * Calls through to {@link InputConnection#setSelection(int, int)}.
     *
     * @param start the character index where the selection should start.
     * @param end the character index where the selection should end.
     * valid when setting the selection or when retrieving the text cache at that point, or
     * invalid arguments were passed.
     */
    public void setSelection(int start, int end) {
        if (start < 0 || end < 0 || start > end) {
            return;
        }
        if (mExpectedSelStart == start && mExpectedSelEnd == end) {
            return;
        }

        final int textStart = mExpectedSelStart - mTextBeforeCursor.length();
        final String textRange = mTextBeforeCursor + mTextSelection + mTextAfterCursor;
        if (textRange.length() >= end - textStart && start - textStart >= 0 && textStart >= 0) {
            // Parameters might be partially updated by background thread, skip in such case
            mTextBeforeCursor = textRange.substring(0, start - textStart);
            mTextSelection = textRange.substring(start - textStart, end - textStart);
            mTextAfterCursor = textRange.substring(end - textStart);
        }

        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();

        mExpectedSelStart = start;
        mExpectedSelEnd = end;
        if (isConnected()) {
            mIC.setSelection(start, end);
        }
    }

    public int getExpectedSelectionStart() {
        return mExpectedSelStart;
    }

    public int getExpectedSelectionEnd() {
        return mExpectedSelEnd;
    }

    /**
     * @return whether there is a selection currently active.
     */
    public boolean hasSelection() {
        return mExpectedSelEnd != mExpectedSelStart;
    }

    public boolean hasCursorPosition() {
        return mExpectedSelStart != INVALID_CURSOR_POSITION && mExpectedSelEnd != INVALID_CURSOR_POSITION;
    }

    /**
     * Some chars, such as emoji consist of 2 chars (surrogate pairs). We should treat them as one character.
     * Some chars are joined with ZERO WIDTH JOINER (U+200D), pairs need to be counted
     */
    public int getUnicodeSteps(int chars, boolean rightSidePointer) {
        int steps = 0;
        if (chars < 0) {
            CharSequence charsBeforeCursor = rightSidePointer && hasSelection() ?
                    getSelectedText() :
                    mTextBeforeCursor;
            if (charsBeforeCursor == null || charsBeforeCursor == "") {
                return chars;
            }
            for (int i = charsBeforeCursor.length() - 1; i >= 0 && chars < 0; i--, steps--) {
                if (i > 1 && charsBeforeCursor.charAt(i - 1) == '\u200d') {
                    continue;
                }
                if (charsBeforeCursor.charAt(i) == '\u200d') {
                    continue;
                }
                if (Character.isSurrogate(charsBeforeCursor.charAt(i)) &&
                        !Character.isHighSurrogate(charsBeforeCursor.charAt(i))) {
                    continue;
                }
                chars++;
            }
        } else if (chars > 0) {
            CharSequence charsAfterCursor = !rightSidePointer && hasSelection() ?
                    getSelectedText() :
                    mTextAfterCursor;
            if (charsAfterCursor == null || charsAfterCursor == "") {
                return chars;
            }
            for (int i = 0; i < charsAfterCursor.length() && chars > 0; i++, steps++) {
                if (i < charsAfterCursor.length() - 1 && charsAfterCursor.charAt(i + 1) == '\u200d') {
                    continue;
                }
                if (charsAfterCursor.charAt(i) == '\u200d') {
                    continue;
                }
                if (Character.isHighSurrogate(charsAfterCursor.charAt(i))) {
                    continue;
                }
                chars--;
            }
        }
        return steps;
    }
}
