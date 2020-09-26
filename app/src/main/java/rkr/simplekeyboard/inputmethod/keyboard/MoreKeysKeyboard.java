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
import android.graphics.Paint;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardBuilder;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams;
import rkr.simplekeyboard.inputmethod.keyboard.internal.MoreKeySpec;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils;

public final class MoreKeysKeyboard extends Keyboard {
    private final int mDefaultKeyCoordX;
    private static final float FLOAT_THRESHOLD = 0.0001f;

    MoreKeysKeyboard(final MoreKeysKeyboardParams params) {
        super(params);
        mDefaultKeyCoordX = Math.round(params.getDefaultKeyCoordX()
                + (params.mDefaultKeyWidth - params.mHorizontalGap) / 2);
    }

    public int getDefaultCoordX() {
        return mDefaultKeyCoordX;
    }

    static class MoreKeysKeyboardParams extends KeyboardParams {
        public boolean mIsMoreKeysFixedOrder;
        /* package */int mTopRowAdjustment;
        public int mNumRows;
        public int mNumColumns;
        public int mTopKeys;
        public int mLeftKeys;
        public int mRightKeys; // includes default key.
        public float mColumnWidth;

        public MoreKeysKeyboardParams() {
            super();
        }

        /**
         * Set keyboard parameters of more keys keyboard.
         *
         * @param numKeys number of keys in this more keys keyboard.
         * @param numColumn number of columns of this more keys keyboard.
         * @param keyWidth more keys keyboard key width in pixel, including horizontal gap.
         * @param rowHeight more keys keyboard row height in pixel, including vertical gap.
         * @param coordXInParent coordinate x of the key preview in parent keyboard.
         * @param parentKeyboardWidth parent keyboard width in pixel.
         * @param isMoreKeysFixedColumn true if more keys keyboard should have
         *   <code>numColumn</code> columns. Otherwise more keys keyboard should have
         *   <code>numColumn</code> columns at most.
         * @param isMoreKeysFixedOrder true if the order of more keys is determined by the order in
         *   the more keys' specification. Otherwise the order of more keys is automatically
         *   determined.
         */
        public void setParameters(final int numKeys, final int numColumn, final float keyWidth,
                final float rowHeight, final float coordXInParent, final int parentKeyboardWidth,
                final boolean isMoreKeysFixedColumn, final boolean isMoreKeysFixedOrder) {
            mIsMoreKeysFixedOrder = isMoreKeysFixedOrder;
            mDefaultKeyWidth = keyWidth;
            mDefaultRowHeight = rowHeight;

            final int numRows = (numKeys + numColumn - 1) / numColumn;
            mNumRows = numRows;
            final int numColumns = isMoreKeysFixedColumn ? Math.min(numKeys, numColumn)
                    : getOptimizedColumns(numKeys, numColumn);
            if (Math.round(parentKeyboardWidth - mLeftPadding - mRightPadding) < Math.round(
                    Math.min(numKeys, numColumns) * keyWidth)) {
                throw new IllegalArgumentException("Keyboard is too small to hold more keys: "
                        + parentKeyboardWidth + " " + keyWidth + " " + numKeys + " " + numColumns);
            }
            mNumColumns = numColumns;
            final int topKeys = numKeys % numColumns;
            mTopKeys = topKeys == 0 ? numColumns : topKeys;

            final int numLeftKeys = (numColumns - 1) / 2;
            final int numRightKeys = numColumns - numLeftKeys; // including default key.
            // Maximum number of keys we can layout both side of the parent key's left edge
            final float leftWidth = Math.max(coordXInParent - mLeftPadding - keyWidth / 2, 0);
            int maxLeftKeys = Math.round(leftWidth / keyWidth);
            if (maxLeftKeys * keyWidth > leftWidth + FLOAT_THRESHOLD) {
                maxLeftKeys--;
            }
            final float rightWidth = Math.max(
                    parentKeyboardWidth - coordXInParent + keyWidth / 2 - mRightPadding, 0);
            int maxRightKeys = Math.round(rightWidth / keyWidth);
            if (maxRightKeys * keyWidth > rightWidth + FLOAT_THRESHOLD) {
                maxRightKeys--;
            }
            int leftKeys, rightKeys;
            if (numLeftKeys > maxLeftKeys) {
                leftKeys = maxLeftKeys;
                rightKeys = numColumns - leftKeys;
            } else if (numRightKeys > maxRightKeys + 1) {
                rightKeys = maxRightKeys + 1; // include default key
                leftKeys = numColumns - rightKeys;
            } else {
                leftKeys = numLeftKeys;
                rightKeys = numRightKeys;
            }
            // If the left keys fill the left side of the parent key, entire more keys keyboard
            // should be shifted to the right unless the parent key is on the left edge.
            if (maxLeftKeys == leftKeys && leftKeys > 0) {
                leftKeys--;
                rightKeys++;
            }
            // If the right keys fill the right side of the parent key, entire more keys
            // should be shifted to the left unless the parent key is on the right edge.
            if (maxRightKeys == rightKeys - 1 && rightKeys > 1) {
                leftKeys++;
                rightKeys--;
            }
            mLeftKeys = leftKeys;
            mRightKeys = rightKeys;

            // Adjustment of the top row.
            mTopRowAdjustment = isMoreKeysFixedOrder ? getFixedOrderTopRowAdjustment()
                    : getAutoOrderTopRowAdjustment();
            mColumnWidth = mDefaultKeyWidth;
            mBaseWidth = mNumColumns * mColumnWidth;
            mOccupiedWidth = Math.round(mBaseWidth + mLeftPadding + mRightPadding - mHorizontalGap);
            // Need to subtract the bottom row's gutter only.
            mBaseHeight = mNumRows * mDefaultRowHeight;
            mOccupiedHeight = Math.round(mBaseHeight + mTopPadding + mBottomPadding - mVerticalGap);
        }

        private int getFixedOrderTopRowAdjustment() {
            if (mNumRows == 1 || mTopKeys % 2 == 1 || mTopKeys == mNumColumns
                    || mLeftKeys == 0  || mRightKeys == 1) {
                return 0;
            }
            return -1;
        }

        private int getAutoOrderTopRowAdjustment() {
            if (mNumRows == 1 || mTopKeys == 1 || mNumColumns % 2 == mTopKeys % 2
                    || mLeftKeys == 0 || mRightKeys == 1) {
                return 0;
            }
            return -1;
        }

        // Return key position according to column count (0 is default).
        /* package */int getColumnPos(final int n) {
            return mIsMoreKeysFixedOrder ? getFixedOrderColumnPos(n) : getAutomaticColumnPos(n);
        }

        private int getFixedOrderColumnPos(final int n) {
            final int col = n % mNumColumns;
            final int row = n / mNumColumns;
            if (!isTopRow(row)) {
                return col - mLeftKeys;
            }
            final int rightSideKeys = mTopKeys / 2;
            final int leftSideKeys = mTopKeys - (rightSideKeys + 1);
            final int pos = col - leftSideKeys;
            final int numLeftKeys = mLeftKeys + mTopRowAdjustment;
            final int numRightKeys = mRightKeys - 1;
            if (numRightKeys >= rightSideKeys && numLeftKeys >= leftSideKeys) {
                return pos;
            } else if (numRightKeys < rightSideKeys) {
                return pos - (rightSideKeys - numRightKeys);
            } else { // numLeftKeys < leftSideKeys
                return pos + (leftSideKeys - numLeftKeys);
            }
        }

        private int getAutomaticColumnPos(final int n) {
            final int col = n % mNumColumns;
            final int row = n / mNumColumns;
            int leftKeys = mLeftKeys;
            if (isTopRow(row)) {
                leftKeys += mTopRowAdjustment;
            }
            if (col == 0) {
                // default position.
                return 0;
            }

            int pos = 0;
            int right = 1; // include default position key.
            int left = 0;
            int i = 0;
            while (true) {
                // Assign right key if available.
                if (right < mRightKeys) {
                    pos = right;
                    right++;
                    i++;
                }
                if (i >= col)
                    break;
                // Assign left key if available.
                if (left < leftKeys) {
                    left++;
                    pos = -left;
                    i++;
                }
                if (i >= col)
                    break;
            }
            return pos;
        }

        private static int getTopRowEmptySlots(final int numKeys, final int numColumns) {
            final int remainings = numKeys % numColumns;
            return remainings == 0 ? 0 : numColumns - remainings;
        }

        private int getOptimizedColumns(final int numKeys, final int maxColumns) {
            int numColumns = Math.min(numKeys, maxColumns);
            while (getTopRowEmptySlots(numKeys, numColumns) >= mNumRows) {
                numColumns--;
            }
            return numColumns;
        }

        public float getDefaultKeyCoordX() {
            return mLeftKeys * mColumnWidth + mLeftPadding;
        }

        public float getX(final int n, final int row) {
            final float x = getColumnPos(n) * mColumnWidth + getDefaultKeyCoordX();
            if (isTopRow(row)) {
                return x + mTopRowAdjustment * (mColumnWidth / 2);
            }
            return x;
        }

        public float getY(final int row) {
            return (mNumRows - 1 - row) * mDefaultRowHeight + mTopPadding;
        }

        private boolean isTopRow(final int rowCount) {
            return mNumRows > 1 && rowCount == mNumRows - 1;
        }
    }

    public static class Builder extends KeyboardBuilder<MoreKeysKeyboardParams> {
        private final Key mParentKey;

        private static final float LABEL_PADDING_RATIO = 0.2f;

        /**
         * The builder of MoreKeysKeyboard.
         * @param context the context of {@link MoreKeysKeyboardView}.
         * @param key the {@link Key} that invokes more keys keyboard.
         * @param keyboard the {@link Keyboard} that contains the parentKey.
         * @param isSingleMoreKeyWithPreview true if the <code>key</code> has just a single
         *        "more key" and its key popup preview is enabled.
         * @param keyPreviewVisibleWidth the width of visible part of key popup preview.
         * @param keyPreviewVisibleHeight the height of visible part of key popup preview
         * @param paintToMeasure the {@link Paint} object to measure a "more key" width
         */
        public Builder(final Context context, final Key key, final Keyboard keyboard,
                final boolean isSingleMoreKeyWithPreview, final int keyPreviewVisibleWidth,
                final int keyPreviewVisibleHeight, final Paint paintToMeasure) {
            super(context, new MoreKeysKeyboardParams());
            load(keyboard.mMoreKeysTemplate, keyboard.mId);

            // TODO: More keys keyboard's vertical gap is currently calculated heuristically.
            // Should revise the algorithm.
            mParams.mVerticalGap = keyboard.mVerticalGap / 2;
            // This {@link MoreKeysKeyboard} is invoked from the <code>key</code>.
            mParentKey = key;

            final float keyWidth, rowHeight;
            if (isSingleMoreKeyWithPreview) {
                // Use pre-computed width and height if this more keys keyboard has only one key to
                // mitigate visual flicker between key preview and more keys keyboard.
                // Caveats for the visual assets: To achieve this effect, both the key preview
                // backgrounds and the more keys keyboard panel background have the exact same
                // left/right/top paddings. The bottom paddings of both backgrounds don't need to
                // be considered because the vertical positions of both backgrounds were already
                // adjusted with their bottom paddings deducted.
                keyWidth = keyPreviewVisibleWidth + mParams.mHorizontalGap;
                rowHeight = keyPreviewVisibleHeight + mParams.mVerticalGap;
            } else {
                final float padding = context.getResources().getDimension(
                        R.dimen.config_more_keys_keyboard_key_horizontal_padding)
                        + (key.hasLabelsInMoreKeys()
                                ? mParams.mDefaultKeyWidth * LABEL_PADDING_RATIO : 0.0f);
                keyWidth = getMaxKeyWidth(key, mParams.mDefaultKeyWidth, padding, paintToMeasure);
                rowHeight = keyboard.mMostCommonKeyHeight;
            }
            final MoreKeySpec[] moreKeys = key.getMoreKeys();
            mParams.setParameters(moreKeys.length, key.getMoreKeysColumnNumber(), keyWidth,
                    rowHeight, key.getX() + key.getWidth() / 2f, keyboard.mId.mWidth,
                    key.isMoreKeysFixedColumn(), key.isMoreKeysFixedOrder());
        }

        private static float getMaxKeyWidth(final Key parentKey, final float minKeyWidth,
                final float padding, final Paint paint) {
            float maxWidth = minKeyWidth;
            for (final MoreKeySpec spec : parentKey.getMoreKeys()) {
                final String label = spec.mLabel;
                // If the label is single letter, minKeyWidth is enough to hold the label.
                if (label != null && StringUtils.codePointCount(label) > 1) {
                    maxWidth = Math.max(maxWidth,
                            TypefaceUtils.getStringWidth(label, paint) + padding);
                }
            }
            return maxWidth;
        }

        @Override
        public MoreKeysKeyboard build() {
            final MoreKeysKeyboardParams params = mParams;
            final int moreKeyFlags = mParentKey.getMoreKeyLabelFlags();
            final MoreKeySpec[] moreKeys = mParentKey.getMoreKeys();
            for (int n = 0; n < moreKeys.length; n++) {
                final MoreKeySpec moreKeySpec = moreKeys[n];
                final int row = n / params.mNumColumns;
                final float x = params.getX(n, row);
                final float y = params.getY(row);
                final float width = params.mDefaultKeyWidth - params.mHorizontalGap;
                final float height = params.mDefaultRowHeight - params.mVerticalGap;
                final float leftGap = x < params.mLeftPadding + FLOAT_THRESHOLD
                        ? params.mLeftPadding : params.mHorizontalGap / 2;
                final float rightGap = x + width > params.mOccupiedWidth - params.mRightPadding
                        - FLOAT_THRESHOLD
                        ? params.mRightPadding : params.mHorizontalGap / 2;
                final float topGap = y < params.mTopPadding + FLOAT_THRESHOLD
                        ? params.mTopPadding : params.mVerticalGap / 2;
                final float bottomGap = y + height > params.mOccupiedHeight - params.mBottomPadding
                        - FLOAT_THRESHOLD
                        ? params.mBottomPadding : params.mVerticalGap / 2;
                final Key key = moreKeySpec.buildKey(x, y, width, height, leftGap, rightGap,
                        topGap, bottomGap, moreKeyFlags);
                params.onAddKey(key);
            }
            return new MoreKeysKeyboard(params);
        }
    }
}
