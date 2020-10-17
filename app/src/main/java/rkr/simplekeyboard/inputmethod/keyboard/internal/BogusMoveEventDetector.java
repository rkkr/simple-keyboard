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

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.Log;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.define.DebugFlags;

// This hack is applied to certain classes of tablets.
public final class BogusMoveEventDetector {
    private static final String TAG = BogusMoveEventDetector.class.getSimpleName();
    private static final boolean DEBUG_MODE = DebugFlags.DEBUG_ENABLED;

    // Move these thresholds to resource.
    // These thresholds' unit is a diagonal length of a key.
    private static final float BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD = 0.53f;

    private static boolean sNeedsProximateBogusDownMoveUpEventHack;

    public static void init(final Resources res) {
        // The proximate bogus down move up event hack is needed for a device such like,
        // 1) is large tablet, or 2) is small tablet and the screen density is less than hdpi.
        // Though it seems odd to use screen density as criteria of the quality of the touch
        // screen, the small table that has a less density screen than hdpi most likely has been
        // made with the touch screen that needs the hack.
        final int screenMetrics = res.getInteger(R.integer.config_screen_metrics);
        final boolean isLargeTablet = (screenMetrics == Constants.SCREEN_METRICS_LARGE_TABLET);
        final boolean isSmallTablet = (screenMetrics == Constants.SCREEN_METRICS_SMALL_TABLET);
        final int densityDpi = res.getDisplayMetrics().densityDpi;
        final boolean hasLowDensityScreen = (densityDpi < DisplayMetrics.DENSITY_HIGH);
        final boolean needsTheHack = isLargeTablet || (isSmallTablet && hasLowDensityScreen);
        if (DEBUG_MODE) {
            final int sw = res.getConfiguration().smallestScreenWidthDp;
            Log.d(TAG, "needsProximateBogusDownMoveUpEventHack=" + needsTheHack
                    + " smallestScreenWidthDp=" + sw + " densityDpi=" + densityDpi
                    + " screenMetrics=" + screenMetrics);
        }
        sNeedsProximateBogusDownMoveUpEventHack = needsTheHack;
    }

    private int mAccumulatedDistanceThreshold;

    // Accumulated distance from actual and artificial down keys.
    /* package */ int mAccumulatedDistanceFromDownKey;
    private int mActualDownX;
    private int mActualDownY;

    public void setKeyboardGeometry(final int keyPaddedWidth, final int keyPaddedHeight) {
        final float keyDiagonal = (float)Math.hypot(keyPaddedWidth, keyPaddedHeight);
        mAccumulatedDistanceThreshold = (int)(
                keyDiagonal * BOGUS_MOVE_ACCUMULATED_DISTANCE_THRESHOLD);
    }

    public void onActualDownEvent(final int x, final int y) {
        mActualDownX = x;
        mActualDownY = y;
    }

    public void onDownKey() {
        mAccumulatedDistanceFromDownKey = 0;
    }

    public void onMoveKey(final int distance) {
        mAccumulatedDistanceFromDownKey += distance;
    }

    public boolean hasTraveledLongDistance(final int x, final int y) {
        if (!sNeedsProximateBogusDownMoveUpEventHack) {
            return false;
        }
        final int dx = Math.abs(x - mActualDownX);
        final int dy = Math.abs(y - mActualDownY);
        // A bogus move event should be a horizontal movement. A vertical movement might be
        // a sloppy typing and should be ignored.
        return dx >= dy && mAccumulatedDistanceFromDownKey >= mAccumulatedDistanceThreshold;
    }

    public int getAccumulatedDistanceFromDownKey() {
        return mAccumulatedDistanceFromDownKey;
    }
}
