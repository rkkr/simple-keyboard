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

import android.content.res.Resources;
import android.content.res.TypedArray;
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
    private static final float FLOAT_THRESHOLD = 0.0001f;

    // keyWidth enum constants
    private static final int KEYWIDTH_NOT_ENUM = 0;
    private static final int KEYWIDTH_FILL_RIGHT = -1;

    private final KeyboardParams mParams;

    /** The y coordinate of the top edge of the row and all keys in it, including the top padding. */
    private final float mY;
    /** The height of this row (and all keys in it including the top and bottom padding). */
    private final float mRowHeight;
    /** The top padding of all of the keys in the row. */
    private final float mTopPadding;
    /** The bottom padding of all of the keys in the row. */
    private final float mBottomPadding;

    /** The left-most coordinate that isn't already occupied by a key in the row. */
    private float mNextAvailableX;
    /** A tracker for where the next key should start, excluding padding. */
    private float mNextKeyXPos;

    /** The x coordinate of the left edge of the current key, including the left padding. */
    private float mCurrentX;
    /** The width of the current key including the left and right padding. */
    private float mCurrentCellWidth;
    /** The left padding of the current key. */
    private float mCurrentLeftPadding;
    /** The right padding of the current key. */
    private float mCurrentRightPadding;

    /** Flag indicating whether the previous key in the row was a spacer. */
    private boolean mLastKeyWasSpacer = false;
    /** The x coordinate of the right edge of the previous key, excluding the right padding. */
    private float mLastKeyRightEdge = 0;

    private final ArrayDeque<RowAttributes> mRowAttributesStack = new ArrayDeque<>();

    // TODO: Add keyActionFlags.
    private static class RowAttributes {
        /** Default width of a key in this row. */
        public final float mDefaultKeyWidth;
        /** Default keyLabelFlags in this row. */
        public final int mDefaultKeyLabelFlags;
        /** Default backgroundType for this row */
        public final int mDefaultBackgroundType;

        /**
         * Parse and create key attributes. This constructor is used to parse Row tag.
         *
         * @param keyAttr an attributes array of Row tag.
         * @param defaultKeyWidth a default key width.
         * @param keyboardWidth the keyboard width that is required to calculate keyWidth attribute.
         */
        public RowAttributes(final TypedArray keyAttr, final float defaultKeyWidth,
                final float keyboardWidth) {
            mDefaultKeyWidth = keyAttr.getFraction(R.styleable.Keyboard_Key_keyWidth,
                    (int)(keyboardWidth * 100), (int)(keyboardWidth * 100),
                    defaultKeyWidth * 100) / 100;
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
            mDefaultKeyWidth = keyAttr.getFraction(R.styleable.Keyboard_Key_keyWidth,
                    (int)(keyboardWidth * 100), (int)(keyboardWidth * 100),
                    defaultRowAttr.mDefaultKeyWidth * 100) / 100;
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
            // the top row should use the keyboard's top padding instead of the vertical gap
            mTopPadding = params.mTopPadding;
        } else {
            mTopPadding = params.mVerticalGap / 2;
        }
        final float baseRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                R.styleable.Keyboard_rowHeight, (int)(params.mBaseHeight * 100),
                params.mDefaultRowHeight * 100) / 100;
        final float keyHeight = baseRowHeight - params.mVerticalGap;
        final float rowEndY = y + mTopPadding + keyHeight + params.mVerticalGap / 2;
        if (rowEndY > params.mOccupiedHeight - params.mBottomPadding - FLOAT_THRESHOLD) {
            // the bottom row's padding should go to the bottom of the keyboard (this might be
            // slightly more than the keyboard's bottom padding if the rows don't add up to 100%)
            // we'll consider it the bottom row as long as the row's normal bottom padding overlaps
            // with the keyboard's bottom padding any amount
            final float keyEndY = y + mTopPadding + keyHeight;
            mBottomPadding = Math.max(params.mOccupiedHeight - keyEndY, params.mBottomPadding);
        } else {
            mBottomPadding = params.mVerticalGap / 2;
        }
        mRowHeight = mTopPadding + keyHeight + mBottomPadding;
        keyboardAttr.recycle();
        final TypedArray keyAttr = res.obtainAttributes(Xml.asAttributeSet(parser),
                R.styleable.Keyboard_Key);
        mRowAttributesStack.push(new RowAttributes(
                keyAttr, params.mDefaultKeyWidth, params.mBaseWidth));
        keyAttr.recycle();

        mY = y;
        mNextAvailableX = 0;
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

    private float getDefaultKeyWidth() {
        return mRowAttributesStack.peek().mDefaultKeyWidth;
    }

    public int getDefaultKeyLabelFlags() {
        return mRowAttributesStack.peek().mDefaultKeyLabelFlags;
    }

    public int getDefaultBackgroundType() {
        return mRowAttributesStack.peek().mDefaultBackgroundType;
    }

    public void updateXPos(final TypedArray keyAttr) {
        if (keyAttr == null || !keyAttr.hasValue(R.styleable.Keyboard_Key_keyXPos)) {
            return;
        }

        final float keyXPos = keyAttr.getFraction(R.styleable.Keyboard_Key_keyXPos,
                (int)(mParams.mBaseWidth * 100), (int)(mParams.mBaseWidth * 100), 0) / 100;

        if (keyXPos >= 0) {
            mNextKeyXPos = keyXPos + mParams.mLeftPadding;
        } else {
            // If keyXPos is negative, the actual x-coordinate will be
            // keyboardWidth + keyXPos.
            // keyXPos shouldn't be less than mNextAvailableX because drawable area for this
            // key starts at mNextAvailableX. Or this key will overlaps the adjacent key on
            // its left hand side.
            final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
            mNextKeyXPos = Math.max(keyXPos + keyboardRightEdge, mNextAvailableX);
        }
    }

    public void setCurrentKey(final TypedArray keyAttr, final boolean isSpacer) {
        // split gap on both sides of key
        final float defaultGap = mParams.mHorizontalGap / 2;

        updateXPos(keyAttr);
        final float keyWidth;
        if (isSpacer) {
            final float leftGap = Math.min(mNextKeyXPos - mNextAvailableX, defaultGap);
            // spacers don't have horizontal gaps but should include that space in its width
            mCurrentX = mNextKeyXPos - leftGap;
            keyWidth = getKeyWidth(keyAttr) + leftGap + defaultGap;
            mCurrentLeftPadding = 0;
            mCurrentRightPadding = 0;
        } else {
            if (mLastKeyRightEdge < FLOAT_THRESHOLD) {
                // first key in row
                mCurrentX = 0;
                mCurrentLeftPadding = mNextKeyXPos;
            } else if (mLastKeyWasSpacer) {
                // the key next to a spacer should have a horizontal gap that spans the distance
                mCurrentX = mNextAvailableX;
                mCurrentLeftPadding = mNextKeyXPos - mNextAvailableX;
            } else {
                // split the gap between the adjacent keys
                mCurrentLeftPadding = (mNextKeyXPos - mLastKeyRightEdge) / 2;
                mCurrentX = mLastKeyRightEdge + mCurrentLeftPadding;
            }
            keyWidth = getKeyWidth(keyAttr);
            // we can't know this before seeing the next key, so just use the default. the key can
            // be updated later
            mCurrentRightPadding = defaultGap;
        }

        mCurrentCellWidth = keyWidth + mCurrentLeftPadding + mCurrentRightPadding;

        // calculations for the current key are done. prep for the next key
        mLastKeyRightEdge = mCurrentX + mCurrentCellWidth - mCurrentRightPadding;
        mLastKeyWasSpacer = isSpacer;
        mNextAvailableX = mCurrentX + mCurrentCellWidth;
        // set the next key's default position
        mNextKeyXPos = mNextAvailableX + defaultGap;
    }

    private float getKeyWidth(final TypedArray keyAttr) {
        if (keyAttr == null) {
            return getDefaultKeyWidth() - mParams.mHorizontalGap;
        }
        final int widthType = ResourceUtils.getEnumValue(keyAttr,
                R.styleable.Keyboard_Key_keyWidth, KEYWIDTH_NOT_ENUM);
        switch (widthType) {
            case KEYWIDTH_FILL_RIGHT:
                // If keyWidth is fillRight, the actual key width will be determined to fill
                // out the area up to the right edge of the keyboard.
                final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
                return keyboardRightEdge - mCurrentX - mCurrentLeftPadding;
            default: // KEYWIDTH_NOT_ENUM
                return keyAttr.getFraction(R.styleable.Keyboard_Key_keyWidth,
                        (int)(mParams.mBaseWidth * 100), (int)(mParams.mBaseWidth * 100),
                        getDefaultKeyWidth() * 100) / 100 - mParams.mHorizontalGap;
        }
    }

    public float getRowY() {
        return mY;
    }

    public float getCellX() {
        return mCurrentX;
    }

    public float getRowHeight() {
        return mRowHeight;
    }

    public float getCellWidth() {
        return mCurrentCellWidth;
    }

    public float getCellTopPadding() {
        return mTopPadding;
    }

    public float getCellBottomPadding() {
        return mBottomPadding;
    }

    public float getCellLeftPadding() {
        return mCurrentLeftPadding;
    }

    public float getCellRightPadding() {
        return mCurrentRightPadding;
    }
}
