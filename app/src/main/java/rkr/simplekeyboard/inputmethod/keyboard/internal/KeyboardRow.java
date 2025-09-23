/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2021 Raimondas Rimkus
 * Copyright (C) 2020 wittmane
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
import android.content.res.TypedArray;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayDeque;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.Key;
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard;
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils;

/**
 * Container for keys in the keyboard. All keys in a row are at the same Y-coordinate.
 * Some of the key size defaults can be overridden per row from what the {@link Keyboard}
 * defines.
 */
public final class KeyboardRow {
    private static final String TAG = KeyboardRow.class.getSimpleName();
    private static final float FLOAT_THRESHOLD = 0.0001f;

    // keyWidth enum constants
    private static final int KEYWIDTH_NOT_ENUM = 0;
    private static final int KEYWIDTH_FILL_RIGHT = -1;

    private final KeyboardParams mParams;

    /** The y coordinate of the top edge of all keys in the row, excluding the top padding. */
    private final float mY;
    /** The height of this row and all keys in it, including the top and bottom padding. */
    private final float mRowHeight;
    /** The top padding of all of the keys in the row. */
    private final float mKeyTopPadding;
    /** The bottom padding of all of the keys in the row. */
    private final float mKeyBottomPadding;

    /** A tracker for where the next key should start, excluding padding. */
    private float mNextKeyXPos;

    /** The x coordinate of the left edge of the current key, excluding the left padding. */
    private float mCurrentX;
    /** The width of the current key excluding the left and right padding. */
    private float mCurrentKeyWidth;
    /** The left padding of the current key. */
    private float mCurrentKeyLeftPadding;
    /** The right padding of the current key. */
    private float mCurrentKeyRightPadding;

    /** Flag indicating whether the previous key in the row was a spacer. */
    private boolean mLastKeyWasSpacer = false;
    /** The x coordinate of the right edge of the previous key, excluding the right padding. */
    private float mLastKeyRightEdge = 0;

    private final ArrayDeque<RowAttributes> mRowAttributesStack = new ArrayDeque<>();

    // TODO: Add keyActionFlags.
    private static class RowAttributes {
        /** Default padded width of a key in this row. */
        public final float mDefaultKeyPaddedWidth;
        /** Default keyLabelFlags in this row. */
        public final int mDefaultKeyLabelFlags;
        /** Default backgroundType for this row */
        public final int mDefaultBackgroundType;

        /**
         * Parse and create key attributes. This constructor is used to parse Row tag.
         *
         * @param keyAttr an attributes array of Row tag.
         * @param defaultKeyPaddedWidth a default padded key width.
         * @param keyboardWidth the keyboard width that is required to calculate keyWidth attribute.
         */
        public RowAttributes(final TypedArray keyAttr, final float defaultKeyPaddedWidth,
                final float keyboardWidth) {
            mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr,
                    R.styleable.Keyboard_Key_keyWidth, keyboardWidth, defaultKeyPaddedWidth);
            mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0);
            mDefaultBackgroundType = keyAttr.getInt(R.styleable.Keyboard_Key_backgroundType,
                    Key.BACKGROUND_TYPE_NORMAL);
        }

        /**
         * Parse and update key attributes using default attributes. This constructor is used
         * to parse include tag.
         *
         * @param keyAttr an attributes array of include tag.
         * @param defaultRowAttr default Row attributes.
         * @param keyboardWidth the keyboard width that is required to calculate keyWidth attribute.
         */
        public RowAttributes(final TypedArray keyAttr, final RowAttributes defaultRowAttr,
                final float keyboardWidth) {
            mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr,
                    R.styleable.Keyboard_Key_keyWidth, keyboardWidth,
                    defaultRowAttr.mDefaultKeyPaddedWidth);
            mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0)
                    | defaultRowAttr.mDefaultKeyLabelFlags;
            mDefaultBackgroundType = keyAttr.getInt(R.styleable.Keyboard_Key_backgroundType,
                    defaultRowAttr.mDefaultBackgroundType);
        }
    }

    public KeyboardRow(final Resources res, final KeyboardParams params,
            final XmlPullParser parser, final float y) {
        mParams = params;
        final TypedArray keyboardAttr = res.obtainAttributes(Xml.asAttributeSet(parser),
                R.styleable.Keyboard);
        if (y < FLOAT_THRESHOLD) {
            // The top row should use the keyboard's top padding instead of the vertical gap
            mKeyTopPadding = params.mTopPadding;
        } else {
            // All of the vertical gap will be used as bottom padding rather than split between the
            // top and bottom because it is probably more likely for users to click below a key
            mKeyTopPadding = 0;
        }
        final float baseRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                R.styleable.Keyboard_rowHeight, params.mBaseHeight, params.mDefaultRowHeight);
        float keyHeight = baseRowHeight - params.mVerticalGap;
        final float rowEndY = y + mKeyTopPadding + keyHeight + params.mVerticalGap;
        final float keyboardBottomEdge = params.mOccupiedHeight - params.mBottomPadding;
        if (rowEndY > keyboardBottomEdge - FLOAT_THRESHOLD) {
            // The bottom row's padding should go to the bottom of the keyboard (this might be
            // slightly more than the keyboard's bottom padding if the rows don't add up to 100%).
            // We'll consider it the bottom row as long as the row's normal bottom padding overlaps
            // with the keyboard's bottom padding any amount.
            final float keyEndY = y + mKeyTopPadding + keyHeight;
            final float keyOverflow = keyEndY - keyboardBottomEdge;
            if (keyOverflow > FLOAT_THRESHOLD) {
                if (Math.round(keyOverflow) > 0) {
                    // Only bother logging an error when expected rounding wouldn't resolve this
                    Log.e(TAG, "The row is too tall to fit in the keyboard (" + keyOverflow
                            + " px). The height was reduced to fit.");
                }
                keyHeight = Math.max(keyboardBottomEdge - y - mKeyTopPadding, 0);
            }
            mKeyBottomPadding = Math.max(params.mOccupiedHeight - keyEndY, 0);
        } else {
            mKeyBottomPadding = params.mVerticalGap;
        }
        mRowHeight = mKeyTopPadding + keyHeight + mKeyBottomPadding;
        keyboardAttr.recycle();
        final TypedArray keyAttr = res.obtainAttributes(Xml.asAttributeSet(parser),
                R.styleable.Keyboard_Key);
        mRowAttributesStack.push(new RowAttributes(
                keyAttr, params.mDefaultKeyPaddedWidth, params.mBaseWidth));
        keyAttr.recycle();

        mY = y + mKeyTopPadding;
        mLastKeyRightEdge = 0;
        mNextKeyXPos = params.mLeftPadding;
    }

    public void pushRowAttributes(final TypedArray keyAttr) {
        final RowAttributes newAttributes = new RowAttributes(
                keyAttr, mRowAttributesStack.peek(), mParams.mBaseWidth);
        mRowAttributesStack.push(newAttributes);
    }

    public void popRowAttributes() {
        mRowAttributesStack.pop();
    }

    private float getDefaultKeyPaddedWidth() {
        return mRowAttributesStack.peek().mDefaultKeyPaddedWidth;
    }

    public int getDefaultKeyLabelFlags() {
        return mRowAttributesStack.peek().mDefaultKeyLabelFlags;
    }

    public int getDefaultBackgroundType() {
        return mRowAttributesStack.peek().mDefaultBackgroundType;
    }

    /**
     * Update the x position for the next key based on what is set in the keyXPos attribute.
     * @param keyAttr the Key XML attributes array.
     */
    public void updateXPos(final TypedArray keyAttr) {
        if (keyAttr == null || !keyAttr.hasValue(R.styleable.Keyboard_Key_keyXPos)) {
            return;
        }

        // keyXPos is based on the base width, but we need to add in the keyboard padding to
        // determine the actual position in the keyboard.
        final float keyXPos = ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyXPos,
                mParams.mBaseWidth, 0) + mParams.mLeftPadding;
        // keyXPos shouldn't be less than mLastKeyRightEdge or this key will overlap the adjacent
        // key on its left hand side.
        if (keyXPos + FLOAT_THRESHOLD < mLastKeyRightEdge) {
            Log.e(TAG, "The specified keyXPos (" + keyXPos
                    + ") is smaller than the next available x position (" + mLastKeyRightEdge
                    + "). The x position was increased to avoid overlapping keys.");
            mNextKeyXPos = mLastKeyRightEdge;
        } else {
            mNextKeyXPos = keyXPos;
        }
    }

    /**
     * Determine the next key's dimensions so they can be retrieved using {@link #getKeyX()},
     * {@link #getKeyWidth()}, etc.
     * @param keyAttr the Key XML attributes array.
     * @param isSpacer flag indicating if the key is a spacer.
     */
    public void setCurrentKey(final TypedArray keyAttr, final boolean isSpacer) {
        // Split gap on both sides of key
        final float defaultGap = mParams.mHorizontalGap / 2;

        updateXPos(keyAttr);
        final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
        float keyWidth;
        if (isSpacer) {
            final float leftGap = Math.min(mNextKeyXPos - mLastKeyRightEdge - defaultGap,
                    defaultGap);
            // Spacers don't have horizontal gaps but should include that space in its width
            mCurrentX = mNextKeyXPos - leftGap;
            keyWidth = getKeyWidth(keyAttr) + leftGap;
            if (mCurrentX + keyWidth + FLOAT_THRESHOLD < keyboardRightEdge) {
                // Add what is normally the default right gap for non-edge spacers
                keyWidth += defaultGap;
            }
            mCurrentKeyLeftPadding = 0;
            mCurrentKeyRightPadding = 0;
        } else {
            mCurrentX = mNextKeyXPos;
            if (mLastKeyRightEdge < FLOAT_THRESHOLD || mLastKeyWasSpacer) {
                // The first key in the row and a key next to a spacer should have a left padding
                // that spans the available distance
                mCurrentKeyLeftPadding = mCurrentX - mLastKeyRightEdge;
            } else {
                // Split the gap between the adjacent keys
                mCurrentKeyLeftPadding = (mCurrentX - mLastKeyRightEdge) / 2;
            }
            keyWidth = getKeyWidth(keyAttr);
            // We can't know this before seeing the next key, so just use the default. The key can
            // be updated later.
            mCurrentKeyRightPadding = defaultGap;
        }
        final float keyOverflow = mCurrentX + keyWidth - keyboardRightEdge;
        if (keyOverflow > FLOAT_THRESHOLD) {
            if (Math.round(keyOverflow) > 0) {
                // Only bother logging an error when expected rounding wouldn't resolve this
                Log.e(TAG, "The " + (isSpacer ? "spacer" : "key")
                        + " is too wide to fit in the keyboard (" + keyOverflow
                        + " px). The width was reduced to fit.");
            }
            keyWidth = Math.max(keyboardRightEdge - mCurrentX, 0);
        }

        mCurrentKeyWidth = keyWidth;

        // Calculations for the current key are done. Prep for the next key.
        mLastKeyRightEdge = mCurrentX + keyWidth;
        mLastKeyWasSpacer = isSpacer;
        // Set the next key's default position. Spacers only add half because their width includes
        // what is normally the horizontal gap.
        mNextKeyXPos = mLastKeyRightEdge + (isSpacer ? defaultGap : mParams.mHorizontalGap);
    }

    private float getKeyWidth(final TypedArray keyAttr) {
        if (keyAttr == null) {
            return getDefaultKeyPaddedWidth() - mParams.mHorizontalGap;
        }
        final int widthType = ResourceUtils.getEnumValue(keyAttr,
                R.styleable.Keyboard_Key_keyWidth, KEYWIDTH_NOT_ENUM);
        switch (widthType) {
            case KEYWIDTH_FILL_RIGHT:
                // If keyWidth is fillRight, the actual key width will be determined to fill
                // out the area up to the right edge of the keyboard.
                final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
                return keyboardRightEdge - mCurrentX;
            default: // KEYWIDTH_NOT_ENUM
                return ResourceUtils.getFraction(keyAttr, R.styleable.Keyboard_Key_keyWidth,
                        mParams.mBaseWidth, getDefaultKeyPaddedWidth()) - mParams.mHorizontalGap;
        }
    }

    public float getRowHeight() {
        return mRowHeight;
    }

    public float getKeyY() {
        return mY;
    }

    public float getKeyX() {
        return mCurrentX;
    }

    public float getKeyWidth() {
        return mCurrentKeyWidth;
    }

    public float getKeyHeight() {
        return mRowHeight - mKeyTopPadding - mKeyBottomPadding;
    }

    public float getKeyTopPadding() {
        return mKeyTopPadding;
    }

    public float getKeyBottomPadding() {
        return mKeyBottomPadding;
    }

    public float getKeyLeftPadding() {
        return mCurrentKeyLeftPadding;
    }

    public float getKeyRightPadding() {
        return mCurrentKeyRightPadding;
    }
}
