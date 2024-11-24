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

package rkr.simplekeyboard.inputmethod.keyboard.internal;

import android.os.Message;
import android.view.ViewConfiguration;

import rkr.simplekeyboard.inputmethod.keyboard.Key;
import rkr.simplekeyboard.inputmethod.keyboard.PointerTracker;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.utils.LeakGuardHandlerWrapper;

public final class TimerHandler extends LeakGuardHandlerWrapper<DrawingProxy>
        implements TimerProxy {
    private static final int MSG_TYPING_STATE_EXPIRED = 0;
    private static final int MSG_REPEAT_KEY = 1;
    private static final int MSG_LONGPRESS_KEY = 2;
    private static final int MSG_LONGPRESS_SHIFT_KEY = 3;
    private static final int MSG_DOUBLE_TAP_SHIFT_KEY = 4;
    private static final int MSG_DISMISS_KEY_PREVIEW = 6;

    private final int mIgnoreAltCodeKeyTimeout;

    public TimerHandler(final DrawingProxy ownerInstance, final int ignoreAltCodeKeyTimeout) {
        super(ownerInstance);
        mIgnoreAltCodeKeyTimeout = ignoreAltCodeKeyTimeout;
    }

    @Override
    public void handleMessage(final Message msg) {
        final DrawingProxy drawingProxy = getOwnerInstance();
        if (drawingProxy == null) {
            return;
        }
        switch (msg.what) {
        case MSG_REPEAT_KEY:
            final PointerTracker tracker1 = (PointerTracker) msg.obj;
            tracker1.onKeyRepeat(msg.arg1 /* code */, msg.arg2 /* repeatCount */);
            break;
        case MSG_LONGPRESS_KEY:
        case MSG_LONGPRESS_SHIFT_KEY:
            cancelLongPressTimers();
            final PointerTracker tracker2 = (PointerTracker) msg.obj;
            tracker2.onLongPressed();
            break;
        case MSG_DISMISS_KEY_PREVIEW:
            drawingProxy.onKeyReleased((Key) msg.obj, false /* withAnimation */);
            break;
        }
    }

    @Override
    public void startKeyRepeatTimerOf(final PointerTracker tracker, final int repeatCount,
            final int delay) {
        final Key key = tracker.getKey();
        if (key == null || delay == 0) {
            return;
        }
        sendMessageDelayed(
                obtainMessage(MSG_REPEAT_KEY, key.getCode(), repeatCount, tracker), delay);
    }

    private void cancelKeyRepeatTimerOf(final PointerTracker tracker) {
        removeMessages(MSG_REPEAT_KEY, tracker);
    }

    public void cancelKeyRepeatTimers() {
        removeMessages(MSG_REPEAT_KEY);
    }

    // TODO: Suppress layout changes in key repeat mode
    public boolean isInKeyRepeat() {
        return hasMessages(MSG_REPEAT_KEY);
    }

    @Override
    public void startLongPressTimerOf(final PointerTracker tracker, final int delay) {
        final Key key = tracker.getKey();
        if (key == null) {
            return;
        }
        // Use a separate message id for long pressing shift key, because long press shift key
        // timers should be canceled when other key is pressed.
        final int messageId = (key.getCode() == Constants.CODE_SHIFT)
                ? MSG_LONGPRESS_SHIFT_KEY : MSG_LONGPRESS_KEY;
        sendMessageDelayed(obtainMessage(messageId, tracker), delay);
    }

    @Override
    public void cancelLongPressTimersOf(final PointerTracker tracker) {
        removeMessages(MSG_LONGPRESS_KEY, tracker);
        removeMessages(MSG_LONGPRESS_SHIFT_KEY, tracker);
    }

    @Override
    public void cancelLongPressShiftKeyTimer() {
        removeMessages(MSG_LONGPRESS_SHIFT_KEY);
    }

    public void cancelLongPressTimers() {
        removeMessages(MSG_LONGPRESS_KEY);
        removeMessages(MSG_LONGPRESS_SHIFT_KEY);
    }

    @Override
    public void startTypingStateTimer(final Key typedKey) {
        if (typedKey.isModifier() || typedKey.altCodeWhileTyping()) {
            return;
        }

        final boolean isTyping = isTypingState();
        removeMessages(MSG_TYPING_STATE_EXPIRED);
        final DrawingProxy drawingProxy = getOwnerInstance();
        if (drawingProxy == null) {
            return;
        }

        // When user hits the space or the enter key, just cancel the while-typing timer.
        final int typedCode = typedKey.getCode();
        if (typedCode == Constants.CODE_SPACE || typedCode == Constants.CODE_ENTER) {
            if (isTyping) {
                drawingProxy.startWhileTypingAnimation(DrawingProxy.FADE_IN);
            }
            return;
        }

        sendMessageDelayed(
                obtainMessage(MSG_TYPING_STATE_EXPIRED), mIgnoreAltCodeKeyTimeout);
        if (isTyping) {
            return;
        }
        drawingProxy.startWhileTypingAnimation(DrawingProxy.FADE_OUT);
    }

    @Override
    public boolean isTypingState() {
        return hasMessages(MSG_TYPING_STATE_EXPIRED);
    }

    @Override
    public void startDoubleTapShiftKeyTimer() {
        sendMessageDelayed(obtainMessage(MSG_DOUBLE_TAP_SHIFT_KEY),
                ViewConfiguration.getDoubleTapTimeout());
    }

    @Override
    public void cancelDoubleTapShiftKeyTimer() {
        removeMessages(MSG_DOUBLE_TAP_SHIFT_KEY);
    }

    @Override
    public boolean isInDoubleTapShiftKeyTimeout() {
        return hasMessages(MSG_DOUBLE_TAP_SHIFT_KEY);
    }

    @Override
    public void cancelKeyTimersOf(final PointerTracker tracker) {
        cancelKeyRepeatTimerOf(tracker);
        cancelLongPressTimersOf(tracker);
    }

    public void cancelAllKeyTimers() {
        cancelKeyRepeatTimers();
        cancelLongPressTimers();
    }

    public void postDismissKeyPreview(final Key key, final long delay) {
        sendMessageDelayed(obtainMessage(MSG_DISMISS_KEY_PREVIEW, key), delay);
    }

    public void cancelAllMessages() {
        cancelAllKeyTimers();
        removeMessages(MSG_DISMISS_KEY_PREVIEW);
    }
}
