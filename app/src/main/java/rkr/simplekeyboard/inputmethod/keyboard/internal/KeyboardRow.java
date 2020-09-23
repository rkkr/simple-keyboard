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
    /** The height of this row. */
    private final float mRowHeight;

    private final float mTopPadding;
    private final float mBottomPadding;

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

    private final float mCurrentY;
    // Will be updated by {@link Key}'s constructor.
    private float mCurrentX;

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

        mCurrentY = y;
        mCurrentX = params.mLeftPadding;
    }

    public float getRowHeight() {
        return mRowHeight;
    }

    public float getTopPadding() {
        return mTopPadding;
    }

    public float getBottomPadding() {
        return mBottomPadding;
    }

    public void pushRowAttributes(final TypedArray keyAttr) {
        final RowAttributes newAttributes = new RowAttributes(
                keyAttr, mRowAttributesStack.peek(), mParams.mBaseWidth);
        mRowAttributesStack.push(newAttributes);
    }

    public void popRowAttributes() {
        mRowAttributesStack.pop();
    }

    public float getDefaultKeyWidth() {
        return mRowAttributesStack.peek().mDefaultKeyWidth;
    }

    public int getDefaultKeyLabelFlags() {
        return mRowAttributesStack.peek().mDefaultKeyLabelFlags;
    }

    public int getDefaultBackgroundType() {
        return mRowAttributesStack.peek().mDefaultBackgroundType;
    }

    public void setXPos(final float keyXPos) {
        mCurrentX = keyXPos;
    }

    public void advanceXPos(final float width) {
        mCurrentX += width;
    }

    public float getKeyY() {
        return mCurrentY;
    }

    public float getKeyX(final TypedArray keyAttr) {
        if (keyAttr == null || !keyAttr.hasValue(R.styleable.Keyboard_Key_keyXPos)) {
            return mCurrentX;
        }
        final float keyXPos = keyAttr.getFraction(R.styleable.Keyboard_Key_keyXPos,
                (int)(mParams.mBaseWidth * 100), (int)(mParams.mBaseWidth * 100), 0) / 100;
        if (keyXPos >= 0) {
            return keyXPos + mParams.mLeftPadding;
        }
        // If keyXPos is negative, the actual x-coordinate will be
        // keyboardWidth + keyXPos.
        // keyXPos shouldn't be less than mCurrentX because drawable area for this
        // key starts at mCurrentX. Or, this key will overlaps the adjacent key on
        // its left hand side.
        final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
        return Math.max(keyXPos + keyboardRightEdge, mCurrentX);
    }

    public float getKeyWidth(final TypedArray keyAttr, final float keyXPos) {
        if (keyAttr == null) {
            return getDefaultKeyWidth();
        }
        final int widthType = ResourceUtils.getEnumValue(keyAttr,
                R.styleable.Keyboard_Key_keyWidth, KEYWIDTH_NOT_ENUM);
        switch (widthType) {
        case KEYWIDTH_FILL_RIGHT:
            // If keyWidth is fillRight, the actual key width will be determined to fill
            // out the area up to the right edge of the keyboard.
            final float keyboardRightEdge = mParams.mOccupiedWidth - mParams.mRightPadding;
            return keyboardRightEdge - keyXPos + mParams.mHorizontalGap;
        default: // KEYWIDTH_NOT_ENUM
            return keyAttr.getFraction(R.styleable.Keyboard_Key_keyWidth,
                    (int)(mParams.mBaseWidth * 100), (int)(mParams.mBaseWidth * 100),
                    getDefaultKeyWidth() * 100) / 100;
        }
    }
}
