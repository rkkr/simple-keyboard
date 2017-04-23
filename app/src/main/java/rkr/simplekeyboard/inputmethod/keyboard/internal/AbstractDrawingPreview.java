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

package rkr.simplekeyboard.inputmethod.keyboard.internal;

import android.graphics.Canvas;
import android.support.annotation.NonNull;

import rkr.simplekeyboard.inputmethod.keyboard.MainKeyboardView;

/**
 * Abstract base class for previews that are drawn on DrawingPreviewPlacerView, e.g.,
 * GestureFloatingTextDrawingPreview.
 */
public abstract class AbstractDrawingPreview {
    private boolean mHasValidGeometry;

    /**
     * Set {@link MainKeyboardView} geometry and position in the window of input method.
     * The class that is overriding this method must call this super implementation.
     *
     * @param originCoords the top-left coordinates of the {@link MainKeyboardView} in
     *        the input method window coordinate-system. This is unused but has a point in an
     *        extended class.
     * @param width the width of {@link MainKeyboardView}.
     * @param height the height of {@link MainKeyboardView}.
     */
    public void setKeyboardViewGeometry(@NonNull final int[] originCoords, final int width,
            final int height) {
        mHasValidGeometry = (width > 0 && height > 0);
    }

    public abstract void onDeallocateMemory();

    /**
     * Draws the preview
     * @param canvas The canvas where the preview is drawn.
     */
    public abstract void drawPreview(@NonNull final Canvas canvas);
}
