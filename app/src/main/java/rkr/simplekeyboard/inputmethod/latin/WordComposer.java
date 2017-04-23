/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.support.annotation.NonNull;

import java.util.ArrayList;

import rkr.simplekeyboard.inputmethod.event.CombinerChain;
import rkr.simplekeyboard.inputmethod.event.Event;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;

/**
 * A place to store the currently composing word with information such as adjacent key codes as well
 */
public final class WordComposer {
    public static final int CAPS_MODE_OFF = 0;
    // 1 is shift bit, 2 is caps bit, 4 is auto bit but this is just a convention as these bits
    // aren't used anywhere in the code
    public static final int CAPS_MODE_MANUAL_SHIFTED = 0x1;
    public static final int CAPS_MODE_MANUAL_SHIFT_LOCKED = 0x3;
    public static final int CAPS_MODE_AUTO_SHIFTED = 0x5;
    public static final int CAPS_MODE_AUTO_SHIFT_LOCKED = 0x7;

    private CombinerChain mCombinerChain;
    private String mCombiningSpec; // Memory so that we don't uselessly recreate the combiner chain

    // The list of events that served to compose this string.
    private final ArrayList<Event> mEvents;
    private boolean mIsBatchMode;

    // Cache these values for performance
    private CharSequence mTypedWordCache;
    // This is the number of code points entered so far. This is not limited to MAX_WORD_LENGTH.
    // In general, this contains the size of mPrimaryKeyCodes, except when this is greater than
    // MAX_WORD_LENGTH in which case mPrimaryKeyCodes only contain the first MAX_WORD_LENGTH
    // code points.
    private int mCodePointSize;
    private int mCursorPositionWithinWord;

    /**
     * Whether the composing word has the only first char capitalized.
     */
    private boolean mIsOnlyFirstCharCapitalized;

    public WordComposer() {
        mCombinerChain = new CombinerChain("");
        mEvents = new ArrayList<>();
        mIsBatchMode = false;
        mCursorPositionWithinWord = 0;
        refreshTypedWordCache();
    }

    /**
     * Restart the combiners, possibly with a new spec.
     * @param combiningSpec The spec string for combining. This is found in the extra value.
     */
    public void restartCombining(final String combiningSpec) {
        final String nonNullCombiningSpec = null == combiningSpec ? "" : combiningSpec;
        if (!nonNullCombiningSpec.equals(mCombiningSpec)) {
            mCombinerChain = new CombinerChain(
                    mCombinerChain.getComposingWordWithCombiningFeedback().toString());
            mCombiningSpec = nonNullCombiningSpec;
        }
    }

    /**
     * Clear out the keys registered so far.
     */
    public void reset() {
        mCombinerChain.reset();
        mEvents.clear();
        mIsOnlyFirstCharCapitalized = false;
        mIsBatchMode = false;
        mCursorPositionWithinWord = 0;
        refreshTypedWordCache();
    }

    private final void refreshTypedWordCache() {
        mTypedWordCache = mCombinerChain.getComposingWordWithCombiningFeedback();
        mCodePointSize = Character.codePointCount(mTypedWordCache, 0, mTypedWordCache.length());
    }

    /**
     * Number of keystrokes in the composing word.
     * @return the number of keystrokes
     */
    public int size() {
        return mCodePointSize;
    }

    public final boolean isComposingWord() {
        return size() > 0;
    }

    /**
     * Process an event and return an event, and return a processed event to apply.
     * @param event the unprocessed event.
     * @return the processed event. Never null, but may be marked as consumed.
     */
    @NonNull
    public Event processEvent(@NonNull final Event event) {
        final Event processedEvent = mCombinerChain.processEvent(mEvents, event);
        // The retained state of the combiner chain may have changed while processing the event,
        // so we need to update our cache.
        refreshTypedWordCache();
        mEvents.add(event);
        return processedEvent;
    }

    /**
     * Apply a processed input event.
     *
     * All input events should be supported, including software/hardware events, characters as well
     * as deletions, multiple inputs and gestures.
     *
     * @param event the event to apply. Must not be null.
     */
    public void applyProcessedEvent(final Event event) {
        mCombinerChain.applyProcessedEvent(event);
        final int primaryCode = event.mCodePoint;
        final int newIndex = size();
        refreshTypedWordCache();
        mCursorPositionWithinWord = mCodePointSize;
        // We may have deleted the last one.
        if (0 == mCodePointSize) {
            mIsOnlyFirstCharCapitalized = false;
        }
        if (Constants.CODE_DELETE != event.mKeyCode) {
            if (0 == newIndex) {
                mIsOnlyFirstCharCapitalized = Character.isUpperCase(primaryCode);
            } else {
                mIsOnlyFirstCharCapitalized = mIsOnlyFirstCharCapitalized
                        && !Character.isUpperCase(primaryCode);
            }
        }
    }

    /**
     * When the cursor is moved by the user, we need to update its position.
     * If it falls inside the currently composing word, we don't reset the composition, and
     * only update the cursor position.
     *
     * @param expectedMoveAmount How many java chars to move the cursor. Negative values move
     * the cursor backward, positive values move the cursor forward.
     * @return true if the cursor is still inside the composing word, false otherwise.
     */
    public boolean moveCursorByAndReturnIfInsideComposingWord(final int expectedMoveAmount) {
        int actualMoveAmount = 0;
        int cursorPos = mCursorPositionWithinWord;
        // TODO: Don't make that copy. We can do this directly from mTypedWordCache.
        final int[] codePoints = StringUtils.toCodePointArray(mTypedWordCache);
        if (expectedMoveAmount >= 0) {
            // Moving the cursor forward for the expected amount or until the end of the word has
            // been reached, whichever comes first.
            while (actualMoveAmount < expectedMoveAmount && cursorPos < codePoints.length) {
                actualMoveAmount += Character.charCount(codePoints[cursorPos]);
                ++cursorPos;
            }
        } else {
            // Moving the cursor backward for the expected amount or until the start of the word
            // has been reached, whichever comes first.
            while (actualMoveAmount > expectedMoveAmount && cursorPos > 0) {
                --cursorPos;
                actualMoveAmount -= Character.charCount(codePoints[cursorPos]);
            }
        }
        // If the actual and expected amounts differ, we crossed the start or the end of the word
        // so the result would not be inside the composing word.
        if (actualMoveAmount != expectedMoveAmount) {
            return false;
        }
        mCursorPositionWithinWord = cursorPos;
        mCombinerChain.applyProcessedEvent(mCombinerChain.processEvent(
                mEvents, Event.createCursorMovedEvent(cursorPos)));
        return true;
    }

    /**
     * Set the currently composing word to the one passed as an argument.
     * This will register NOT_A_COORDINATE for X and Ys, and use the passed keyboard for proximity.
     * @param codePoints the code points to set as the composing word.
     * @param coordinates the x, y coordinates of the key in the CoordinateUtils format
     */
    public void setComposingWord(final int[] codePoints, final int[] coordinates) {
        reset();
        final int length = codePoints.length;
        for (int i = 0; i < length; ++i) {
            final Event processedEvent =
                    processEvent(Event.createEventForCodePointFromAlreadyTypedText(codePoints[i],
                    CoordinateUtils.xFromArray(coordinates, i),
                    CoordinateUtils.yFromArray(coordinates, i)));
            applyProcessedEvent(processedEvent);
        }
    }

    /**
     * Returns the word as it was typed, without any correction applied.
     * @return the word that was typed so far. Never returns null.
     */
    public String getTypedWord() {
        return mTypedWordCache.toString();
    }

    // `type' should be one of the LastComposedWord.COMMIT_TYPE_* constants above.
    // committedWord should contain suggestion spans if applicable.
    public LastComposedWord commitWord(final int type, final CharSequence committedWord,
                                       final String separatorString) {
        // Note: currently, we come here whenever we commit a word. If it's a MANUAL_PICK
        // or a DECIDED_WORD we may cancel the commit later; otherwise, we should deactivate
        // the last composed word to ensure this does not happen.
        final LastComposedWord lastComposedWord = new LastComposedWord(mEvents,
                mTypedWordCache.toString(), committedWord, separatorString);
        if (type != LastComposedWord.COMMIT_TYPE_DECIDED_WORD
                && type != LastComposedWord.COMMIT_TYPE_MANUAL_PICK) {
            lastComposedWord.deactivate();
        }
        mIsBatchMode = false;
        mCombinerChain.reset();
        mEvents.clear();
        mCodePointSize = 0;
        mIsOnlyFirstCharCapitalized = false;
        refreshTypedWordCache();
        mCursorPositionWithinWord = 0;
        return lastComposedWord;
    }

    public boolean isBatchMode() {
        return mIsBatchMode;
    }
}
