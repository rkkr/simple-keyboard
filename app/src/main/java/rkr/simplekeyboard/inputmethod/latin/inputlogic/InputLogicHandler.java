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

package rkr.simplekeyboard.inputmethod.latin.inputlogic;

import android.os.HandlerThread;

import rkr.simplekeyboard.inputmethod.latin.LatinIME;

/**
 * A helper to manage deferred tasks for the input logic.
 */
class InputLogicHandler {
    // TODO: remove this reference.
    final LatinIME mLatinIME;
    final InputLogic mInputLogic;

    // A handler that never does anything. This is used for cases where events come before anything
    // is initialized, though probably only the monkey can actually do this.
    public static final InputLogicHandler NULL_HANDLER = new InputLogicHandler() {
    };

    InputLogicHandler() {
        mLatinIME = null;
        mInputLogic = null;
    }

    public InputLogicHandler(final LatinIME latinIME, final InputLogic inputLogic) {
        final HandlerThread handlerThread = new HandlerThread(
                InputLogicHandler.class.getSimpleName());
        handlerThread.start();
        mLatinIME = latinIME;
        mInputLogic = inputLogic;
    }
}
