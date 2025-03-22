/*
 * Copyright (C) 2011 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.graphics.Insets;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public final class ViewLayoutUtils {
    private ViewLayoutUtils() {
        // This utility class is not publicly instantiable.
    }

    public static MarginLayoutParams newLayoutParam(final ViewGroup placer, final int width,
            final int height) {
        if (placer instanceof FrameLayout) {
            return new FrameLayout.LayoutParams(width, height);
        } else if (placer instanceof RelativeLayout) {
            return new RelativeLayout.LayoutParams(width, height);
        } else if (placer == null) {
            throw new NullPointerException("placer is null");
        } else {
            throw new IllegalArgumentException("placer is neither FrameLayout nor RelativeLayout: "
                    + placer.getClass().getName());
        }
    }

    public static void placeViewAt(final View view, final int x, final int y, final int w,
            final int h) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof MarginLayoutParams) {
            final MarginLayoutParams marginLayoutParams = (MarginLayoutParams)lp;
            marginLayoutParams.width = w;
            marginLayoutParams.height = h;
            marginLayoutParams.setMargins(x, y, -50, 0);
        }
    }

    public static void updateLayoutHeightOf(final Window window, final int layoutHeight) {
        final WindowManager.LayoutParams params = window.getAttributes();
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight;
            window.setAttributes(params);
        }
    }

    public static void updateLayoutHeightOf(final View view, final int layoutHeight) {
        final ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params != null && params.height != layoutHeight) {
            params.height = layoutHeight;
            view.setLayoutParams(params);
        }
    }

    public static void updateLayoutGravityOf(final View view, final int layoutGravity) {
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)lp;
            if (params.gravity != layoutGravity) {
                params.gravity = layoutGravity;
                view.setLayoutParams(params);
            }
        } else if (lp instanceof FrameLayout.LayoutParams) {
            final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)lp;
            if (params.gravity != layoutGravity) {
                params.gravity = layoutGravity;
                view.setLayoutParams(params);
            }
        } else {
            throw new IllegalArgumentException("Layout parameter doesn't have gravity: "
                    + lp.getClass().getName());
        }
    }

    public static void applyViewInsets(final View view, final Insets insets) {
        if (insets != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            view.setLayoutParams(mlp);
        }
    }
}
