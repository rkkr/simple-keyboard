/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.SurroundingText;

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
    private final StringBuilder mTextBeforeCursor = new StringBuilder();
    private final StringBuilder mTextAfterCursor = new StringBuilder();

    private final LatinIME mLatinIME;
    private InputConnection mIC;
    private int mNestLevel;

    public RichInputConnection(final LatinIME latinIME) {
        mLatinIME = latinIME;
        mIC = null;
        mNestLevel = 0;
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

    /**
     * Reset the cached text and retrieve it again from the editor.
     *
     * This should be called when the cursor moved. It's possible that we can't connect to
     * the application when doing this; notably, this happens sometimes during rotation, probably
     * because of a race condition in the framework. In this case, we just can't retrieve the
     * data, so we empty the cache and note that we don't know the new cursor position, and we
     * return false so that the caller knows about this and can retry later.
     *
     * @param newSelStart the new position of the selection start, as received from the system.
     * @param newSelEnd the new position of the selection end, as received from the system.
     */
    public void resetCachesUponCursorMove(final int newSelStart,
            final int newSelEnd) {
        mExpectedSelStart = newSelStart;
        mExpectedSelEnd = newSelEnd;
        reloadTextCache();
    }

    /**
     * Reload the cached text from the InputConnection.
     */
    private void reloadTextCache() {
        // Call upon the inputconnection directly since our own method is using the cache, and
        // we want to refresh it.
        mIC = mLatinIME.getCurrentInputConnection();
        if (!isConnected()) {
            return;
        }
        Executors.newSingleThreadExecutor().execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                final SurroundingText textAroundCursor =
                        mIC.getSurroundingText(Constants.EDITOR_CONTENTS_CACHE_SIZE, Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                mTextBeforeCursor.setLength(0);
                mTextAfterCursor.setLength(0);
                if (null == textAroundCursor) {
                    mExpectedSelStart = INVALID_CURSOR_POSITION;
                    mExpectedSelEnd = INVALID_CURSOR_POSITION;
                    Log.e(TAG, "Unable get text before cursor.");
                    return;
                }
                mTextBeforeCursor.append(
                        textAroundCursor.getText().subSequence(0, textAroundCursor.getSelectionStart()));
                mTextAfterCursor.append(
                        textAroundCursor.getText().subSequence(textAroundCursor.getSelectionStart(), textAroundCursor.getText().length()));

                // All callbacks that need text before cursor are here
                mLatinIME.mHandler.postUpdateShiftState();
            } else {
                final CharSequence textBeforeCursor = mIC.getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                mTextBeforeCursor.setLength(0);
                if (null == textBeforeCursor) {
                    mExpectedSelStart = INVALID_CURSOR_POSITION;
                    mExpectedSelEnd = INVALID_CURSOR_POSITION;
                    Log.e(TAG, "Unable get text before cursor.");
                    return;
                }
                mTextBeforeCursor.append(textBeforeCursor);

                // All callbacks that need text before cursor are here
                mLatinIME.mHandler.postUpdateShiftState();

                final CharSequence textAfterCursor = mIC.getTextAfterCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
                mTextAfterCursor.setLength(0);
                if (null == textAfterCursor) {
                    Log.e(TAG, "Unable to get text after cursor.");
                    return;
                }
                mTextAfterCursor.append(textAfterCursor);
            }
        });
    }

    /**
     * Calls {@link InputConnection#commitText(CharSequence, int)}.
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    public void commitText(final CharSequence text, final int newCursorPosition) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();
        mTextBeforeCursor.append(text);
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

    public CharSequence getSelectedText(final int flags) {
        return isConnected() ?  mIC.getSelectedText(flags) : null;
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
        if (TextUtils.isEmpty(mTextBeforeCursor) && 0 != mExpectedSelStart) {
            Log.w(TAG, "Unable to get Caps mode as mTextBeforeCursor is empty.");
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        // TODO: don't call #toString() here. Instead, all accesses to
        // mCommittedTextBeforeComposingText should be done on the main thread.
        return CapsModeUtils.getCapsMode(mTextBeforeCursor.toString(), inputType,
                spacingAndPunctuations);
    }

    public int getCodePointBeforeCursor() {
        final int length = mTextBeforeCursor.length();
        if (length < 1) return Constants.NOT_A_CODE;
        return Character.codePointBefore(mTextBeforeCursor, length);
    }

    public CharSequence getTextBeforeCursor(final int n) {
        // If we have enough characters to satisfy the request, or if we have all characters in
        // the text field, then we can return the cached version right away.
        // However, if we don't have an expected cursor position, then we should always
        // go fetch the cache again (as it happens, INVALID_CURSOR_POSITION < 0, so we need to
        // test for this explicitly)
        if (INVALID_CURSOR_POSITION != mExpectedSelStart) {
            final StringBuilder s = new StringBuilder(mTextBeforeCursor);
            // We call #toString() here to create a temporary object.
            // In some situations, this method is called on a worker thread, and it's possible
            // the main thread touches the contents of mComposingText while this worker thread
            // is suspended, because mComposingText is a StringBuilder. This may lead to crashes,
            // so we call #toString() on it. That will result in the return value being strictly
            // speaking wrong, but since this is used for basing bigram probability off, and
            // it's only going to matter for one getSuggestions call, it's fine in the practice.
            if (s.length() > n) {
                s.delete(0, s.length() - n);
            }
            return s;
        }
        return "";
    }

    public CharSequence getTextAfterCursor(final int n) {
        // If we have enough characters to satisfy the request, or if we have all characters in
        // the text field, then we can return the cached version right away.
        // However, if we don't have an expected cursor position, then we should always
        // go fetch the cache again (as it happens, INVALID_CURSOR_POSITION < 0, so we need to
        // test for this explicitly)
        if (INVALID_CURSOR_POSITION != mExpectedSelStart) {
            final StringBuilder s = new StringBuilder(mTextAfterCursor);
            // We call #toString() here to create a temporary object.
            // In some situations, this method is called on a worker thread, and it's possible
            // the main thread touches the contents of mComposingText while this worker thread
            // is suspended, because mComposingText is a StringBuilder. This may lead to crashes,
            // so we call #toString() on it. That will result in the return value being strictly
            // speaking wrong, but since this is used for basing bigram probability off, and
            // it's only going to matter for one getSuggestions call, it's fine in the practice.
            if (s.length() > n) {
                s.setLength(n);
            }
            return s;
        }
        return "";
    }

    public void replaceText(final int startPosition, final int endPosition, CharSequence text) {
        RichInputMethodManager.getInstance().resetSubtypeCycleOrder();
        mIC.setComposingRegion(startPosition, endPosition);
        mIC.setComposingText(text, startPosition);
        mIC.finishComposingText();
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
                mTextBeforeCursor.append("\n");
                if (hasCursorPosition()) {
                    mExpectedSelStart += 1;
                    mExpectedSelEnd = mExpectedSelStart;
                }
                break;
            case KeyEvent.KEYCODE_DEL:
                if (mTextBeforeCursor.length() > 0) {
                    mTextBeforeCursor.delete(
                            mTextBeforeCursor.length() - 1,
                            mTextBeforeCursor.length());
                }

                if (mExpectedSelStart > 0 && mExpectedSelStart == mExpectedSelEnd) {
                    // TODO: Handle surrogate pairs.
                    mExpectedSelStart -= 1;
                }
                mExpectedSelEnd = mExpectedSelStart;
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                if (null != keyEvent.getCharacters()) {
                    mTextBeforeCursor.append(keyEvent.getCharacters());
                    if (hasCursorPosition()) {
                        mExpectedSelStart += keyEvent.getCharacters().length();
                        mExpectedSelEnd = mExpectedSelStart;
                    }
                }
                break;
            default:
                final String text = StringUtils.newSingleCodePointString(keyEvent.getUnicodeChar());
                mTextBeforeCursor.append(text);
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
        if (start < 0 || end < 0) {
            return;
        }
        if (mExpectedSelStart == start && mExpectedSelEnd == end) {
            return;
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
     */
    public int getUnicodeSteps(int chars, boolean rightSidePointer) {
        int steps = 0;
        if (chars < 0) {
            CharSequence charsBeforeCursor = rightSidePointer && hasSelection() ?
                    getSelectedText(0) :
                    getTextBeforeCursor(-chars * 2);
            if (charsBeforeCursor != null && charsBeforeCursor != "") {
                for (int i = charsBeforeCursor.length() - 1; i >= 0 && chars < 0; i--, chars++, steps--) {
                    if (Character.isSurrogate(charsBeforeCursor.charAt(i))) {
                        steps--;
                        i--;
                    }
                }
            }
        } else if (chars > 0) {
            CharSequence charsAfterCursor = !rightSidePointer && hasSelection() ?
                    getSelectedText(0) :
                    getTextAfterCursor(chars * 2);
            if (charsAfterCursor != null && charsAfterCursor != "") {
                for (int i = 0; i < charsAfterCursor.length() && chars > 0; i++, chars--, steps++) {
                    if (Character.isSurrogate(charsAfterCursor.charAt(i))) {
                        steps++;
                        i++;
                    }
                }
            }
        }
        return steps;
    }
}
