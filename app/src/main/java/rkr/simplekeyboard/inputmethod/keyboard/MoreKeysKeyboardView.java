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

package rkr.simplekeyboard.inputmethod.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.Constants;
import rkr.simplekeyboard.inputmethod.latin.common.CoordinateUtils;

/**
 * A view that renders a virtual {@link MoreKeysKeyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 */
public class MoreKeysKeyboardView extends KeyboardView implements MoreKeysPanel {
    private final int[] mCoordinates = CoordinateUtils.newInstance();

    protected final KeyDetector mKeyDetector;
    private Controller mController = EMPTY_CONTROLLER;
    protected KeyboardActionListener mListener;
    private int mOriginX;
    private int mOriginY;
    private Key mCurrentKey;

    private int mActivePointerId;

    public MoreKeysKeyboardView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.moreKeysKeyboardViewStyle);
    }

    public MoreKeysKeyboardView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray moreKeysKeyboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.MoreKeysKeyboardView, defStyle, R.style.MoreKeysKeyboardView);
        moreKeysKeyboardViewAttr.recycle();
        mKeyDetector = new MoreKeysDetector(getResources().getDimension(
                R.dimen.config_more_keys_keyboard_slide_allowance));
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard != null) {
            final int width = keyboard.mOccupiedWidth + getPaddingLeft() + getPaddingRight();
            final int height = keyboard.mOccupiedHeight + getPaddingTop() + getPaddingBottom();
            setMeasuredDimension(width, height);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public void setKeyboard(final Keyboard keyboard) {
        super.setKeyboard(keyboard);
        mKeyDetector.setKeyboard(
                keyboard, -getPaddingLeft(), -getPaddingTop() + getVerticalCorrection());
    }

    @Override
    public void showMoreKeysPanel(final View parentView, final Controller controller,
            final int pointX, final int pointY, final KeyboardActionListener listener) {
        mController = controller;
        mListener = listener;
        final View container = getContainerView();
        // The coordinates of panel's left-top corner in parentView's coordinate system.
        // We need to consider background drawable paddings.
        final int x = pointX - getDefaultCoordX() - container.getPaddingLeft() - getPaddingLeft();
        final int y = pointY - container.getMeasuredHeight() + container.getPaddingBottom()
                + getPaddingBottom();

        parentView.getLocationInWindow(mCoordinates);
        // Ensure the horizontal position of the panel does not extend past the parentView edges.
        final int maxX = parentView.getMeasuredWidth() - container.getMeasuredWidth();
        final int panelX = Math.max(0, Math.min(maxX, x)) + CoordinateUtils.x(mCoordinates);
        final int panelY = y + CoordinateUtils.y(mCoordinates);
        container.setX(panelX);
        container.setY(panelY);

        mOriginX = x + container.getPaddingLeft();
        mOriginY = y + container.getPaddingTop();
        controller.onShowMoreKeysPanel(this);
    }

    /**
     * Returns the default x coordinate for showing this panel.
     */
    protected int getDefaultCoordX() {
        return ((MoreKeysKeyboard)getKeyboard()).getDefaultCoordX();
    }

    @Override
    public void onDownEvent(final int x, final int y, final int pointerId) {
        mActivePointerId = pointerId;
        mCurrentKey = detectKey(x, y);
    }

    @Override
    public void onMoveEvent(final int x, final int y, final int pointerId) {
        if (mActivePointerId != pointerId) {
            return;
        }
        final boolean hasOldKey = (mCurrentKey != null);
        mCurrentKey = detectKey(x, y);
        if (hasOldKey && mCurrentKey == null) {
            // A more keys keyboard is canceled when detecting no key.
            mController.onCancelMoreKeysPanel();
        }
    }

    @Override
    public void onUpEvent(final int x, final int y, final int pointerId) {
        if (mActivePointerId != pointerId) {
            return;
        }
        // Calling {@link #detectKey(int,int,int)} here is harmless because the last move event and
        // the following up event share the same coordinates.
        mCurrentKey = detectKey(x, y);
        if (mCurrentKey != null) {
            updateReleaseKeyGraphics(mCurrentKey);
            onKeyInput(mCurrentKey);
            mCurrentKey = null;
        }
    }

    /**
     * Performs the specific action for this panel when the user presses a key on the panel.
     */
    protected void onKeyInput(final Key key) {
        final int code = key.getCode();
        if (code == Constants.CODE_OUTPUT_TEXT) {
            mListener.onTextInput(mCurrentKey.getOutputText());
        } else if (code != Constants.CODE_UNSPECIFIED) {
            mListener.onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false /* isKeyRepeat */);
        }
    }

    private Key detectKey(int x, int y) {
        final Key oldKey = mCurrentKey;
        final Key newKey = mKeyDetector.detectHitKey(x, y);
        if (newKey == oldKey) {
            return newKey;
        }
        // A new key is detected.
        if (oldKey != null) {
            updateReleaseKeyGraphics(oldKey);
            invalidateKey(oldKey);
        }
        if (newKey != null) {
            updatePressKeyGraphics(newKey);
            invalidateKey(newKey);
        }
        return newKey;
    }

    private void updateReleaseKeyGraphics(final Key key) {
        key.onReleased();
        invalidateKey(key);
    }

    private void updatePressKeyGraphics(final Key key) {
        key.onPressed();
        invalidateKey(key);
    }

    @Override
    public void dismissMoreKeysPanel() {
        if (!isShowingInParent()) {
            return;
        }
        mController.onDismissMoreKeysPanel();
    }

    @Override
    public int translateX(final int x) {
        return x - mOriginX;
    }

    @Override
    public int translateY(final int y) {
        return y - mOriginY;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent me) {
        final int action = me.getActionMasked();
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);
        final int pointerId = me.getPointerId(index);
        switch (action) {
        case MotionEvent.ACTION_DOWN:
        case MotionEvent.ACTION_POINTER_DOWN:
            onDownEvent(x, y, pointerId);
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_POINTER_UP:
            onUpEvent(x, y, pointerId);
            break;
        case MotionEvent.ACTION_MOVE:
            onMoveEvent(x, y, pointerId);
            break;
        }
        return true;
    }

    private View getContainerView() {
        return (View)getParent();
    }

    @Override
    public void showInParent(final ViewGroup parentView) {
        removeFromParent();
        parentView.addView(getContainerView());
    }

    @Override
    public void removeFromParent() {
        final View containerView = getContainerView();
        final ViewGroup currentParent = (ViewGroup)containerView.getParent();
        if (currentParent != null) {
            currentParent.removeView(containerView);
        }
    }

    @Override
    public boolean isShowingInParent() {
        return (getContainerView().getParent() != null);
    }
}
