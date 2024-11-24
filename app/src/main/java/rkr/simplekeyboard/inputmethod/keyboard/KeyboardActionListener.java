/*
 * Copyright (C) 2010 The Android Open Source Project
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

import rkr.simplekeyboard.inputmethod.latin.common.Constants;

public interface KeyboardActionListener {
    /**
     * Called when the user presses a key. This is sent before the {@link #onCodeInput} is called.
     * For keys that repeat, this is only called once.
     *
     * @param primaryCode the unicode of the key being pressed. If the touch is not on a valid key,
     *            the value will be zero.
     * @param repeatCount how many times the key was repeated. Zero if it is the first press.
     * @param isSinglePointer true if pressing has occurred while no other key is being pressed.
     */
    void onPressKey(int primaryCode, int repeatCount, boolean isSinglePointer);

    /**
     * Called when the user releases a key. This is sent after the {@link #onCodeInput} is called.
     * For keys that repeat, this is only called once.
     *
     * @param primaryCode the code of the key that was released
     * @param withSliding true if releasing has occurred because the user slid finger from the key
     *             to other key without releasing the finger.
     */
    void onReleaseKey(int primaryCode, boolean withSliding);

    /**
     * Send a key code to the listener.
     *
     * @param primaryCode this is the code of the key that was pressed
     * @param x x-coordinate pixel of touched event. If {@link #onCodeInput} is not called by
     *            {@link PointerTracker} or so, the value should be
     *            {@link Constants#NOT_A_COORDINATE}. If it's called on insertion from the
     *            suggestion strip, it should be {@link Constants#SUGGESTION_STRIP_COORDINATE}.
     * @param y y-coordinate pixel of touched event. If {@link #onCodeInput} is not called by
     *            {@link PointerTracker} or so, the value should be
     *            {@link Constants#NOT_A_COORDINATE}.If it's called on insertion from the
     *            suggestion strip, it should be {@link Constants#SUGGESTION_STRIP_COORDINATE}.
     * @param isKeyRepeat true if this is a key repeat, false otherwise
     */
    // TODO: change this to send an Event object instead
    void onCodeInput(int primaryCode, int x, int y, boolean isKeyRepeat);

    /**
     * Sends a string of characters to the listener.
     *
     * @param text the string of characters to be registered.
     */
    void onTextInput(final String rawText);

    /**
     * Called when user finished sliding key input.
     */
    void onFinishSlidingInput();

    /**
     * Send a non-"code input" custom request to the listener.
     * @return true if the request has been consumed, false otherwise.
     */
    boolean onCustomRequest(int requestCode);
    void onMovePointer(int steps);
    void onMoveDeletePointer(int steps);
    void onUpWithDeletePointerActive();

    KeyboardActionListener EMPTY_LISTENER = new Adapter();

    class Adapter implements KeyboardActionListener {
        @Override
        public void onPressKey(int primaryCode, int repeatCount, boolean isSinglePointer) {}
        @Override
        public void onReleaseKey(int primaryCode, boolean withSliding) {}
        @Override
        public void onCodeInput(int primaryCode, int x, int y, boolean isKeyRepeat) {}
        @Override
        public void onTextInput(String text) {}
        @Override
        public void onFinishSlidingInput() {}
        @Override
        public boolean onCustomRequest(int requestCode) {
            return false;
        }
        @Override
        public void onMovePointer(int steps) {}
        @Override
        public void onMoveDeletePointer(int steps) {}
        @Override
        public void onUpWithDeletePointerActive() {}
    }
}
