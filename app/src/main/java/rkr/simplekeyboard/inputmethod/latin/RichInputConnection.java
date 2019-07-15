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

import android.inputmethodservice.InputMethodService;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;

import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.common.UnicodeSurrogate;
import rkr.simplekeyboard.inputmethod.latin.settings.SpacingAndPunctuations;
import rkr.simplekeyboard.inputmethod.latin.utils.CapsModeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.DebugLogUtils;

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
    private static final boolean DBG = false;
    private static final boolean DEBUG_PREVIOUS_TEXT = false;
    private static final boolean DEBUG_BATCH_NESTING = false;
    private static final int INVALID_CURSOR_POSITION = -1;

    /**
     * The amount of time a {@link #reloadTextCache} call needs to take for the keyboard to enter
     */
    private static final long SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS = 1000;
    /**
     * The amount of time a {@link #getTextBeforeCursor} or {@link #getTextAfterCursor} call needs
     * to take for the keyboard to enter the {@link #hasSlowInputConnection} state.
     */
    private static final long SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS = 200;

    private static final int OPERATION_GET_TEXT_BEFORE_CURSOR = 0;
    private static final int OPERATION_RELOAD_TEXT_CACHE = 3;
    private static final String[] OPERATION_NAMES = new String[] {
            "GET_TEXT_BEFORE_CURSOR",
            "GET_TEXT_AFTER_CURSOR",
            "GET_WORD_RANGE_AT_CURSOR",
            "RELOAD_TEXT_CACHE"};

    /**
     * This variable contains an expected value for the selection start position. This is where the
     * cursor or selection start may end up after all the keyboard-triggered updates have passed. We
     * keep this to compare it to the actual selection start to guess whether the move was caused by
     * a keyboard command or not.
     * It's not really the selection start position: the selection start may not be there yet, and
     * in some cases, it may never arrive there.
     */
    public int mExpectedSelStart = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * The expected selection end.  Only differs from mExpectedSelStart if a non-empty selection is
     * expected.  The same caveats as mExpectedSelStart apply.
     */
    public int mExpectedSelEnd = INVALID_CURSOR_POSITION; // in chars, not code points
    /**
     * This contains the committed text immediately preceding the cursor and the composing
     * text, if any. It is refreshed when the cursor moves by calling upon the TextView.
     */
    private final StringBuilder mCommittedTextBeforeComposingText = new StringBuilder();
    /**
     * This contains the currently composing text, as LatinIME thinks the TextView is seeing it.
     */
    private final StringBuilder mComposingText = new StringBuilder();

    /**
     * This variable is a temporary object used in {@link #commitText(CharSequence,int)}
     * to avoid object creation.
     */
    private SpannableStringBuilder mTempObjectForCommitText = new SpannableStringBuilder();

    private final InputMethodService mParent;
    private InputConnection mIC;
    private int mNestLevel;

    public RichInputConnection(final InputMethodService parent) {
        mParent = parent;
        mIC = null;
        mNestLevel = 0;
    }

    public boolean isConnected() {
        return mIC != null;
    }

    private void checkConsistencyForDebug() {
        final ExtractedTextRequest r = new ExtractedTextRequest();
        r.hintMaxChars = 0;
        r.hintMaxLines = 0;
        r.token = 1;
        r.flags = 0;
        final ExtractedText et = mIC.getExtractedText(r, 0);
        final CharSequence beforeCursor = getTextBeforeCursor(Constants.EDITOR_CONTENTS_CACHE_SIZE,
                0);
        final StringBuilder internal = new StringBuilder(mCommittedTextBeforeComposingText)
                .append(mComposingText);
        if (null == et || null == beforeCursor) return;
        final int actualLength = Math.min(beforeCursor.length(), internal.length());
        if (internal.length() > actualLength) {
            internal.delete(0, internal.length() - actualLength);
        }
        final String reference = (beforeCursor.length() <= actualLength) ? beforeCursor.toString()
                : beforeCursor.subSequence(beforeCursor.length() - actualLength,
                        beforeCursor.length()).toString();
        if (et.selectionStart != mExpectedSelStart
                || !(reference.equals(internal.toString()))) {
            final String context = "Expected selection start = " + mExpectedSelStart
                    + "\nActual selection start = " + et.selectionStart
                    + "\nExpected text = " + internal.length() + " " + internal
                    + "\nActual text = " + reference.length() + " " + reference;
            ((LatinIME)mParent).debugDumpStateAndCrashWithException(context);
        } else {
            Log.e(TAG, DebugLogUtils.getStackTrace(2));
            Log.e(TAG, "Exp <> Actual : " + mExpectedSelStart + " <> " + et.selectionStart);
        }
    }

    public void beginBatchEdit() {
        if (++mNestLevel == 1) {
            mIC = mParent.getCurrentInputConnection();
            if (isConnected()) {
                mIC.beginBatchEdit();
            }
        } else {
            if (DBG) {
                throw new RuntimeException("Nest level too deep");
            }
            Log.e(TAG, "Nest level too deep : " + mNestLevel);
        }
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    public void endBatchEdit() {
        if (mNestLevel <= 0) Log.e(TAG, "Batch edit not in progress!"); // TODO: exception instead
        if (--mNestLevel == 0 && isConnected()) {
            mIC.endBatchEdit();
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
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
     * @return true if we were able to connect to the editor successfully, false otherwise. When
     *   this method returns false, the caches could not be correctly refreshed so they were only
     *   reset: the caller should try again later to return to normal operation.
     */
    public boolean resetCachesUponCursorMoveAndReturnSuccess(final int newSelStart,
            final int newSelEnd) {
        mExpectedSelStart = newSelStart;
        mExpectedSelEnd = newSelEnd;
        mComposingText.setLength(0);
        final boolean didReloadTextSuccessfully = reloadTextCache();
        if (!didReloadTextSuccessfully) {
            Log.d(TAG, "Will try to retrieve text later.");
            return false;
        }
        return true;
    }

    /**
     * Reload the cached text from the InputConnection.
     *
     * @return true if successful
     */
    private boolean reloadTextCache() {
        mCommittedTextBeforeComposingText.setLength(0);
        mIC = mParent.getCurrentInputConnection();
        // Call upon the inputconnection directly since our own method is using the cache, and
        // we want to refresh it.
        final CharSequence textBeforeCursor = getTextBeforeCursorAndDetectLaggyConnection(
                OPERATION_RELOAD_TEXT_CACHE,
                SLOW_INPUT_CONNECTION_ON_FULL_RELOAD_MS,
                Constants.EDITOR_CONTENTS_CACHE_SIZE,
                0 /* flags */);
        if (null == textBeforeCursor) {
            // For some reason the app thinks we are not connected to it. This looks like a
            // framework bug... Fall back to ground state and return false.
            mExpectedSelStart = INVALID_CURSOR_POSITION;
            mExpectedSelEnd = INVALID_CURSOR_POSITION;
            Log.e(TAG, "Unable to connect to the editor to retrieve text.");
            return false;
        }
        mCommittedTextBeforeComposingText.append(textBeforeCursor);
        return true;
    }

    private void checkBatchEdit() {
        if (mNestLevel != 1) {
            // TODO: exception instead
            Log.e(TAG, "Batch edit level incorrect : " + mNestLevel);
            Log.e(TAG, DebugLogUtils.getStackTrace(4));
        }
    }

    public void finishComposingText() {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        // TODO: this is not correct! The cursor is not necessarily after the composing text.
        // In the practice right now this is only called when input ends so it will be reset so
        // it works, but it's wrong and should be fixed.
        mCommittedTextBeforeComposingText.append(mComposingText);
        mComposingText.setLength(0);
        if (isConnected()) {
            mIC.finishComposingText();
        }
    }

    /**
     * Calls {@link InputConnection#commitText(CharSequence, int)}.
     *
     * @param text The text to commit. This may include styles.
     * @param newCursorPosition The new cursor position around the text.
     */
    public void commitText(final CharSequence text, final int newCursorPosition) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        mCommittedTextBeforeComposingText.append(text);
        // TODO: the following is exceedingly error-prone. Right now when the cursor is in the
        // middle of the composing word mComposingText only holds the part of the composing text
        // that is before the cursor, so this actually works, but it's terribly confusing. Fix this.
        mExpectedSelStart += text.length() - mComposingText.length();
        mExpectedSelEnd = mExpectedSelStart;
        mComposingText.setLength(0);
        if (isConnected()) {
            mTempObjectForCommitText.clear();
            mTempObjectForCommitText.append(text);
            final CharacterStyle[] spans = mTempObjectForCommitText.getSpans(
                    0, text.length(), CharacterStyle.class);
            for (final CharacterStyle span : spans) {
                final int spanStart = mTempObjectForCommitText.getSpanStart(span);
                final int spanEnd = mTempObjectForCommitText.getSpanEnd(span);
                final int spanFlags = mTempObjectForCommitText.getSpanFlags(span);
                // We have to adjust the end of the span to include an additional character.
                // This is to avoid splitting a unicode surrogate pair.
                // See rkr.simplekeyboard.inputmethod.latin.common.Constants.UnicodeSurrogate
                // See https://b.corp.google.com/issues/19255233
                if (0 < spanEnd && spanEnd < mTempObjectForCommitText.length()) {
                    final char spanEndChar = mTempObjectForCommitText.charAt(spanEnd - 1);
                    final char nextChar = mTempObjectForCommitText.charAt(spanEnd);
                    if (UnicodeSurrogate.isLowSurrogate(spanEndChar)
                            && UnicodeSurrogate.isHighSurrogate(nextChar)) {
                        mTempObjectForCommitText.setSpan(span, spanStart, spanEnd + 1, spanFlags);
                    }
                }
            }
            mIC.commitText(mTempObjectForCommitText, newCursorPosition);
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
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return Constants.TextUtils.CAP_MODE_OFF;
        }
        if (!TextUtils.isEmpty(mComposingText)) {
            // We have some composing text - we should be in MODE_CHARACTERS only.
            return TextUtils.CAP_MODE_CHARACTERS & inputType;
        }
        // TODO: this will generally work, but there may be cases where the buffer contains SOME
        // information but not enough to determine the caps mode accurately. This may happen after
        // heavy pressing of delete, for example DEFAULT_TEXT_CACHE_SIZE - 5 times or so.
        // getCapsMode should be updated to be able to return a "not enough info" result so that
        // we can get more context only when needed.
        if (TextUtils.isEmpty(mCommittedTextBeforeComposingText) && 0 != mExpectedSelStart) {
            if (!reloadTextCache()) {
                Log.w(TAG, "Unable to connect to the editor. "
                        + "Setting caps mode without knowing text.");
            }
        }
        // This never calls InputConnection#getCapsMode - in fact, it's a static method that
        // never blocks or initiates IPC.
        // TODO: don't call #toString() here. Instead, all accesses to
        // mCommittedTextBeforeComposingText should be done on the main thread.
        return CapsModeUtils.getCapsMode(mCommittedTextBeforeComposingText.toString(), inputType,
                spacingAndPunctuations);
    }

    public int getCodePointBeforeCursor() {
        final int length = mCommittedTextBeforeComposingText.length();
        if (length < 1) return Constants.NOT_A_CODE;
        return Character.codePointBefore(mCommittedTextBeforeComposingText, length);
    }

    public CharSequence getTextBeforeCursor(final int n, final int flags) {
        final int cachedLength =
                mCommittedTextBeforeComposingText.length() + mComposingText.length();
        // If we have enough characters to satisfy the request, or if we have all characters in
        // the text field, then we can return the cached version right away.
        // However, if we don't have an expected cursor position, then we should always
        // go fetch the cache again (as it happens, INVALID_CURSOR_POSITION < 0, so we need to
        // test for this explicitly)
        if (INVALID_CURSOR_POSITION != mExpectedSelStart
                && (cachedLength >= n || cachedLength >= mExpectedSelStart)) {
            final StringBuilder s = new StringBuilder(mCommittedTextBeforeComposingText);
            // We call #toString() here to create a temporary object.
            // In some situations, this method is called on a worker thread, and it's possible
            // the main thread touches the contents of mComposingText while this worker thread
            // is suspended, because mComposingText is a StringBuilder. This may lead to crashes,
            // so we call #toString() on it. That will result in the return value being strictly
            // speaking wrong, but since this is used for basing bigram probability off, and
            // it's only going to matter for one getSuggestions call, it's fine in the practice.
            s.append(mComposingText.toString());
            if (s.length() > n) {
                s.delete(0, s.length() - n);
            }
            return s;
        }
        return getTextBeforeCursorAndDetectLaggyConnection(
                OPERATION_GET_TEXT_BEFORE_CURSOR,
                SLOW_INPUT_CONNECTION_ON_PARTIAL_RELOAD_MS,
                n, flags);
    }

    private CharSequence getTextBeforeCursorAndDetectLaggyConnection(
            final int operation, final long timeout, final int n, final int flags) {
        mIC = mParent.getCurrentInputConnection();
        if (!isConnected()) {
            return null;
        }
        final long startTime = SystemClock.uptimeMillis();
        final CharSequence result = mIC.getTextBeforeCursor(n, flags);
        detectLaggyConnection(operation, timeout, startTime);
        return result;
    }

    private void detectLaggyConnection(final int operation, final long timeout, final long startTime) {
        final long duration = SystemClock.uptimeMillis() - startTime;
        if (duration >= timeout) {
            final String operationName = OPERATION_NAMES[operation];
            Log.w(TAG, "Slow InputConnection: " + operationName + " took " + duration + " ms.");
        }
    }

    public void deleteTextBeforeCursor(final int beforeLength) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        // TODO: the following is incorrect if the cursor is not immediately after the composition.
        // Right now we never come here in this case because we reset the composing state before we
        // come here in this case, but we need to fix this.
        final int remainingChars = mComposingText.length() - beforeLength;
        if (remainingChars >= 0) {
            mComposingText.setLength(remainingChars);
        } else {
            mComposingText.setLength(0);
            // Never cut under 0
            final int len = Math.max(mCommittedTextBeforeComposingText.length()
                    + remainingChars, 0);
            mCommittedTextBeforeComposingText.setLength(len);
        }
        if (mExpectedSelStart > beforeLength) {
            mExpectedSelStart -= beforeLength;
            mExpectedSelEnd -= beforeLength;
        } else {
            // There are fewer characters before the cursor in the buffer than we are being asked to
            // delete. Only delete what is there, and update the end with the amount deleted.
            mExpectedSelEnd -= mExpectedSelStart;
            mExpectedSelStart = 0;
        }
        if (isConnected()) {
            mIC.deleteSurroundingText(beforeLength, 0);
        }
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
    }

    public void performEditorAction(final int actionId) {
        mIC = mParent.getCurrentInputConnection();
        if (isConnected()) {
            mIC.performEditorAction(actionId);
        }
    }

    public void sendKeyEvent(final KeyEvent keyEvent) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
            // This method is only called for enter or backspace when speaking to old applications
            // (target SDK <= 15 (Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)), or for digits.
            // When talking to new applications we never use this method because it's inherently
            // racy and has unpredictable results, but for backward compatibility we continue
            // sending the key events for only Enter and Backspace because some applications
            // mistakenly catch them to do some stuff.
            switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_ENTER:
                mCommittedTextBeforeComposingText.append("\n");
                mExpectedSelStart += 1;
                mExpectedSelEnd = mExpectedSelStart;
                break;
            case KeyEvent.KEYCODE_DEL:
                if (0 == mComposingText.length()) {
                    if (mCommittedTextBeforeComposingText.length() > 0) {
                        mCommittedTextBeforeComposingText.delete(
                                mCommittedTextBeforeComposingText.length() - 1,
                                mCommittedTextBeforeComposingText.length());
                    }
                } else {
                    mComposingText.delete(mComposingText.length() - 1, mComposingText.length());
                }

                if (mExpectedSelStart > 0 && mExpectedSelStart == mExpectedSelEnd) {
                    // TODO: Handle surrogate pairs.
                    mExpectedSelStart -= 1;
                }
                mExpectedSelEnd = mExpectedSelStart;
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                if (null != keyEvent.getCharacters()) {
                    mCommittedTextBeforeComposingText.append(keyEvent.getCharacters());
                    mExpectedSelStart += keyEvent.getCharacters().length();
                    mExpectedSelEnd = mExpectedSelStart;
                }
                break;
            default:
                final String text = StringUtils.newSingleCodePointString(keyEvent.getUnicodeChar());
                mCommittedTextBeforeComposingText.append(text);
                mExpectedSelStart += text.length();
                mExpectedSelEnd = mExpectedSelStart;
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
     * @return Returns true on success, false on failure: either the input connection is no longer
     * valid when setting the selection or when retrieving the text cache at that point, or
     * invalid arguments were passed.
     */
    public boolean setSelection(final int start, final int end) {
        if (DEBUG_BATCH_NESTING) checkBatchEdit();
        if (DEBUG_PREVIOUS_TEXT) checkConsistencyForDebug();
        if (start < 0 || end < 0) {
            return false;
        }
        mExpectedSelStart = start;
        mExpectedSelEnd = end;
        if (isConnected()) {
            final boolean isIcValid = mIC.setSelection(start, end);
            if (!isIcValid) {
                return false;
            }
        }
        return reloadTextCache();
    }

    /**
     * Try to get the text from the editor to expose lies the framework may have been
     * telling us. Concretely, when the device rotates and when the keyboard reopens in the same
     * text field after having been closed with the back key, the frameworks tells us about where
     * the cursor used to be initially in the editor at the time it first received the focus; this
     * may be completely different from the place it is upon rotation. Since we don't have any
     * means to get the real value, try at least to ask the text view for some characters and
     * detect the most damaging cases: when the cursor position is declared to be much smaller
     * than it really is.
     */
    public void tryFixLyingCursorPosition() {
        mIC = mParent.getCurrentInputConnection();
        final CharSequence textBeforeCursor = getTextBeforeCursor(
                Constants.EDITOR_CONTENTS_CACHE_SIZE, 0);
        final CharSequence selectedText = isConnected() ? mIC.getSelectedText(0 /* flags */) : null;
        if (null == textBeforeCursor ||
                (!TextUtils.isEmpty(selectedText) && mExpectedSelEnd == mExpectedSelStart)) {
            // If textBeforeCursor is null, we have no idea what kind of text field we have or if
            // thinking about the "cursor position" actually makes any sense. In this case we
            // remember a meaningless cursor position. Contrast this with an empty string, which is
            // valid and should mean the cursor is at the start of the text.
            // Also, if we expect we don't have a selection but we DO have non-empty selected text,
            // then the framework lied to us about the cursor position. In this case, we should just
            // revert to the most basic behavior possible for the next action (backspace in
            // particular comes to mind), so we remember a meaningless cursor position which should
            // result in degraded behavior from the next input.
            // Interestingly, in either case, chances are any action the user takes next will result
            // in a call to onUpdateSelection, which should set things right.
            mExpectedSelStart = mExpectedSelEnd = Constants.NOT_A_CURSOR_POSITION;
        } else {
            final int textLength = textBeforeCursor.length();
            if (textLength < Constants.EDITOR_CONTENTS_CACHE_SIZE
                    && (textLength > mExpectedSelStart
                            ||  mExpectedSelStart < Constants.EDITOR_CONTENTS_CACHE_SIZE)) {
                // It should not be possible to have only one of those variables be
                // NOT_A_CURSOR_POSITION, so if they are equal, either the selection is zero-sized
                // (simple cursor, no selection) or there is no cursor/we don't know its pos
                final boolean wasEqual = mExpectedSelStart == mExpectedSelEnd;
                mExpectedSelStart = textLength;
                // We can't figure out the value of mLastSelectionEnd :(
                // But at least if it's smaller than mLastSelectionStart something is wrong,
                // and if they used to be equal we also don't want to make it look like there is a
                // selection.
                if (wasEqual || mExpectedSelStart > mExpectedSelEnd) {
                    mExpectedSelEnd = mExpectedSelStart;
                }
            }
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
}
