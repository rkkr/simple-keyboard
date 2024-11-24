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

package rkr.simplekeyboard.inputmethod.keyboard.internal;

import rkr.simplekeyboard.inputmethod.keyboard.Key;
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker;

public interface TimerProxy {
    /**
     * Start a timer to detect if a user is typing keys.
     * @param typedKey the key that is typed.
     */
    void startTypingStateTimer(Key typedKey);

    /**
     * Check if a user is key typing.
     * @return true if a user is in typing.
     */
    boolean isTypingState();

    /**
     * Start a timer to simulate repeated key presses while a user keep pressing a key.
     * @param tracker the {@link PointerTracker} that points the key to be repeated.
     * @param repeatCount the number of times that the key is repeating. Starting from 1.
     * @param delay the interval delay to the next key repeat, in millisecond.
     */
    void startKeyRepeatTimerOf(PointerTracker tracker, int repeatCount, int delay);

    /**
     * Start a timer to detect a long pressed key.
     * If a key pointed by <code>tracker</code> is a shift key, start another timer to detect
     * long pressed shift key.
     * @param tracker the {@link PointerTracker} that starts long pressing.
     * @param delay the delay to fire the long press timer, in millisecond.
     */
    void startLongPressTimerOf(PointerTracker tracker, int delay);

    /**
     * Cancel timers for detecting a long pressed key and a long press shift key.
     * @param tracker cancel long press timers of this {@link PointerTracker}.
     */
    void cancelLongPressTimersOf(PointerTracker tracker);

    /**
     * Cancel a timer for detecting a long pressed shift key.
     */
    void cancelLongPressShiftKeyTimer();

    /**
     * Cancel timers for detecting repeated key press, long pressed key, and long pressed shift key.
     * @param tracker the {@link PointerTracker} that starts timers to be canceled.
     */
    void cancelKeyTimersOf(PointerTracker tracker);

    /**
     * Start a timer to detect double tapped shift key.
     */
    void startDoubleTapShiftKeyTimer();

    /**
     * Cancel a timer of detecting double tapped shift key.
     */
    void cancelDoubleTapShiftKeyTimer();

    /**
     * Check if a timer of detecting double tapped shift key is running.
     * @return true if detecting double tapped shift key is on going.
     */
    boolean isInDoubleTapShiftKeyTimeout();
}
