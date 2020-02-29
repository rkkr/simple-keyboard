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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayDeque;
import java.util.HashMap;

import rkr.simplekeyboard.inputmethod.keyboard.Key;
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.ViewLayoutUtils;

/**
 * This class controls pop up key previews. This class decides:
 * - what kind of key previews should be shown.
 * - where key previews should be placed.
 * - how key previews should be shown and dismissed.
 */
public final class KeyPreviewChoreographer {
    // Free {@link KeyPreviewView} pool that can be used for key preview.
    private final ArrayDeque<KeyPreviewView> mFreeKeyPreviewViews = new ArrayDeque<>();
    // Map from {@link Key} to {@link KeyPreviewView} that is currently being displayed as key
    // preview.
    private final HashMap<Key,KeyPreviewView> mShowingKeyPreviewViews = new HashMap<>();

    private final KeyPreviewDrawParams mParams;

    public KeyPreviewChoreographer(final KeyPreviewDrawParams params) {
        mParams = params;
    }

    public KeyPreviewView getKeyPreviewView(final Key key, final ViewGroup placerView) {
        KeyPreviewView keyPreviewView = mShowingKeyPreviewViews.remove(key);
        if (keyPreviewView != null) {
            return keyPreviewView;
        }
        keyPreviewView = mFreeKeyPreviewViews.poll();
        if (keyPreviewView != null) {
            return keyPreviewView;
        }
        final Context context = placerView.getContext();
        keyPreviewView = new KeyPreviewView(context, null /* attrs */);
        keyPreviewView.setBackgroundResource(mParams.mPreviewBackgroundResId);
        placerView.addView(keyPreviewView, ViewLayoutUtils.newLayoutParam(placerView, 0, 0));
        return keyPreviewView;
    }

    public void dismissKeyPreview(final Key key, final boolean withAnimation) {
        if (key == null) {
            return;
        }
        final KeyPreviewView keyPreviewView = mShowingKeyPreviewViews.get(key);
        if (keyPreviewView == null) {
            return;
        }
        final Object tag = keyPreviewView.getTag();
        if (withAnimation) {
            if (tag instanceof KeyPreviewAnimators) {
                final KeyPreviewAnimators animators = (KeyPreviewAnimators)tag;
                animators.startDismiss();
                return;
            }
        }
        // Dismiss preview without animation.
        mShowingKeyPreviewViews.remove(key);
        if (tag instanceof Animator) {
            ((Animator)tag).cancel();
        }
        keyPreviewView.setTag(null);
        keyPreviewView.setVisibility(View.INVISIBLE);
        mFreeKeyPreviewViews.add(keyPreviewView);
    }

    public void placeAndShowKeyPreview(final Key key, final KeyboardIconsSet iconsSet,
            final KeyDrawParams drawParams, final int[] keyboardOrigin,
            final ViewGroup placerView, final boolean withAnimation) {
        final KeyPreviewView keyPreviewView = getKeyPreviewView(key, placerView);
        placeKeyPreview(
                key, keyPreviewView, iconsSet, drawParams, keyboardOrigin);
        showKeyPreview(key, keyPreviewView, withAnimation);
    }

    private void placeKeyPreview(final Key key, final KeyPreviewView keyPreviewView,
            final KeyboardIconsSet iconsSet, final KeyDrawParams drawParams,
            final int[] originCoords) {
        keyPreviewView.setPreviewVisual(key, iconsSet, drawParams);
        keyPreviewView.measure(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mParams.setGeometry(keyPreviewView);
        final int previewWidth = Math.max(keyPreviewView.getMeasuredWidth(), mParams.mMinPreviewWidth);
        final int previewHeight = mParams.mPreviewHeight;
        final int keyDrawWidth = key.getDrawWidth();
        // The key preview is horizontally aligned with the center of the visible part of the
        // parent key. If it doesn't fit in this {@link KeyboardView}, it is moved inward to fit and
        // the left/right background is used if such background is specified.
        int previewX = key.getDrawX() - (previewWidth - keyDrawWidth) / 2
                + CoordinateUtils.x(originCoords);
        // The key preview is placed vertically above the top edge of the parent key with an
        // arbitrary offset.
        final int previewY = key.getY() - previewHeight + mParams.mPreviewOffset
                + CoordinateUtils.y(originCoords);

        ViewLayoutUtils.placeViewAt(
                keyPreviewView, previewX, previewY, previewWidth, previewHeight);
        //keyPreviewView.setPivotX(previewWidth / 2.0f);
        //keyPreviewView.setPivotY(previewHeight);
    }

    void showKeyPreview(final Key key, final KeyPreviewView keyPreviewView,
            final boolean withAnimation) {
        if (!withAnimation) {
            keyPreviewView.setVisibility(View.VISIBLE);
            mShowingKeyPreviewViews.put(key, keyPreviewView);
            return;
        }

        // Show preview with animation.
        final Animator dismissAnimator = createDismissAnimator(key, keyPreviewView);
        final KeyPreviewAnimators animators = new KeyPreviewAnimators(dismissAnimator);
        keyPreviewView.setTag(animators);
        showKeyPreview(key, keyPreviewView, false /* withAnimation */);
    }

    private Animator createDismissAnimator(final Key key, final KeyPreviewView keyPreviewView) {
        final Animator dismissAnimator = mParams.createDismissAnimator(keyPreviewView);
        dismissAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(final Animator animator) {
                dismissKeyPreview(key, false /* withAnimation */);
            }
        });
        return dismissAnimator;
    }

    private static class KeyPreviewAnimators extends AnimatorListenerAdapter {
        private final Animator mDismissAnimator;

        public KeyPreviewAnimators(final Animator dismissAnimator) {
            mDismissAnimator = dismissAnimator;
        }

        public void startDismiss() {
            mDismissAnimator.start();
        }
    }
}
