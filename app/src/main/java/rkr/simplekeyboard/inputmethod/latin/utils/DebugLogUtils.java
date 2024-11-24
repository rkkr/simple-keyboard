/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.util.Log;

import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags;

/**
 * A class for logging and debugging utility methods.
 */
public final class DebugLogUtils {
    private final static String TAG = DebugLogUtils.class.getSimpleName();
    private final static boolean sDBG = DebugFlags.DEBUG_ENABLED;

    /**
     * Calls .toString() on its non-null argument or returns "null"
     * @param o the object to convert to a string
     * @return the result of .toString() or null
     */
    public static String s(final Object o) {
        return null == o ? "null" : o.toString();
    }

    /**
     * Helper log method to ease null-checks and adding spaces.
     *
     * This sends all arguments to the log, separated by spaces. Any null argument is converted
     * to the "null" string. It uses a very visible tag and log level for debugging purposes.
     *
     * @param args the stuff to send to the log
     */
    public static void l(final Object... args) {
        if (!sDBG) return;
        final StringBuilder sb = new StringBuilder();
        for (final Object o : args) {
            sb.append(s(o).toString());
            sb.append(" ");
        }
        Log.e(TAG, sb.toString());
    }

    /**
     * Helper log method to put stuff in red.
     *
     * This does the same as #l but prints in red
     *
     * @param args the stuff to send to the log
     */
    public static void r(final Object... args) {
        if (!sDBG) return;
        final StringBuilder sb = new StringBuilder("\u001B[31m");
        for (final Object o : args) {
            sb.append(s(o).toString());
            sb.append(" ");
        }
        sb.append("\u001B[0m");
        Log.e(TAG, sb.toString());
    }
}
