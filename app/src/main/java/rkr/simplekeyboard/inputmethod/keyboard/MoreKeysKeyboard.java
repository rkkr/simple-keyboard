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
import android.util.Log;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardBuilder;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams;
import rkr.simplekeyboard.inputmethod.keyboard.internal.MoreKeySpec;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.TypefaceUtils;

public final class MoreKeysKeyboard extends Keyboard {
    private static final String TAG = MoreKeysKeyboard.class.getSimpleName();
    private final int mDefaultKeyCoordX;
    private static final float FLOAT_THRESHOLD = 0.0001f;

    MoreKeysKeyboard(final MoreKeysKeyboardParams params) {
        super(params);
        mDefaultKeyCoordX = Math.round(params.getDefaultKeyCoordX() + params.mOffsetX
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
        public float mOffsetX;

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
            // Add the horizontal padding because there is no horizontal gap on the outside edge,
            // but it is included in the key width, so this compensates for simple division and
            // comparison.
            final float availableWidth = parentKeyboardWidth - mLeftPadding - mRightPadding
                    + mHorizontalGap;
            if (availableWidth < keyWidth) {
                throw new IllegalArgumentException("Keyboard is too small to hold more keys: "
                        + availableWidth + " " + keyWidth);
            }
            mIsMoreKeysFixedOrder = isMoreKeysFixedOrder;
            mDefaultKeyWidth = keyWidth;
            mDefaultRowHeight = rowHeight;

            final int maxColumns = getMaxKeys(availableWidth, keyWidth);
            if (isMoreKeysFixedColumn) {
                int requestedNumColumns = Math.min(numKeys, numColumn);
                if (maxColumns < requestedNumColumns) {
                    Log.e(TAG, "Keyboard is too small to hold the requested more keys columns: "
                            + availableWidth + " " + keyWidth + " " + numKeys + " "
                            + requestedNumColumns + ". The number of columns was reduced.");
                    mNumColumns = maxColumns;
                } else {
                    mNumColumns = requestedNumColumns;
                }
                mNumRows = getNumRows(numKeys, mNumColumns);
            } else {
                int defaultNumColumns = Math.min(maxColumns, numColumn);
                mNumRows = getNumRows(numKeys, defaultNumColumns);
                mNumColumns = getOptimizedColumns(numKeys, defaultNumColumns, mNumRows);
            }
            final int topKeys = numKeys % mNumColumns;
            mTopKeys = topKeys == 0 ? mNumColumns : topKeys;

            final int numLeftKeys = (mNumColumns - 1) / 2;
            final int numRightKeys = mNumColumns - numLeftKeys; // including default key.
            // Maximum number of keys we can lay out on both side of the left edge of a key
            // centered on the parent key. Also, account for horizontal padding because there is no
            // horizontal gap on the outside edge.
            final float leftWidth = Math.max(coordXInParent - mLeftPadding - keyWidth / 2
                    + mHorizontalGap / 2, 0);
            final float rightWidth = Math.max(parentKeyboardWidth - coordXInParent + keyWidth / 2
                    - mRightPadding + mHorizontalGap / 2, 0);
            int maxLeftKeys = getMaxKeys(leftWidth, keyWidth);
            int maxRightKeys = getMaxKeys(rightWidth, keyWidth);
            // handle the case where the number of columns fits but doesn't have enough room
            // for the default key to be centered on the parent key
            if (numKeys >= mNumColumns && mNumColumns == maxColumns
                    && maxLeftKeys + maxRightKeys < maxColumns) {
                final float extraLeft = leftWidth - maxLeftKeys * keyWidth;
                final float extraRight = rightWidth - maxRightKeys * keyWidth;
                // put the extra key on whatever side has more space
                if (extraLeft > extraRight) {
                    maxLeftKeys++;
                } else {
                    maxRightKeys++;
                }
            }

            int leftKeys, rightKeys;
            if (numLeftKeys > maxLeftKeys) {
                leftKeys = maxLeftKeys;
                rightKeys = mNumColumns - leftKeys;
            } else if (numRightKeys > maxRightKeys + 1) {
                rightKeys = maxRightKeys + 1; // include default key
                leftKeys = mNumColumns - rightKeys;
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

            mGridWidth = Math.min(mGridWidth, mNumColumns);
            mGridHeight = Math.min(mGridHeight, mNumRows);
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

        private static int getOptimizedColumns(final int numKeys, final int maxColumns,
                                               final int numRows) {
            int numColumns = Math.min(numKeys, maxColumns);
            while (getTopRowEmptySlots(numKeys, numColumns) >= numRows) {
                numColumns--;
            }
            return numColumns;
        }

        private static int getNumRows(final int numKeys, final int numColumn) {
            return (numKeys + numColumn - 1) / numColumn;
        }

        private static int getMaxKeys(final float keyboardWidth, final float keyWidth) {
            final int maxKeys = Math.round(keyboardWidth / keyWidth);
            if (maxKeys * keyWidth > keyboardWidth + FLOAT_THRESHOLD) {
                return maxKeys - 1;
            }
            return maxKeys;
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
                // The bottom paddings don't need to be considered because the vertical positions
                // of both backgrounds and the keyboard were already adjusted with their bottom
                // paddings deducted. The keyboard's left/right/top paddings do need to be deducted
                // so the key including the paddings matches the key preview.
                final float horizontalPadding = mParams.mLeftPadding + mParams.mRightPadding;
                final float baseKeyWidth = keyPreviewVisibleWidth + mParams.mHorizontalGap;
                if (horizontalPadding > baseKeyWidth - FLOAT_THRESHOLD) {
                    // if the padding doesn't fit we'll just add it outside of the key preview
                    keyWidth = baseKeyWidth;
                } else {
                    keyWidth = baseKeyWidth - horizontalPadding;
                    // keep the more keys keyboard with uneven padding lined up with the key
                    // preview rather than centering the more keys keyboard's key with the parent
                    // key
                    mParams.mOffsetX = (mParams.mRightPadding - mParams.mLeftPadding) / 2;
                }
                final float baseRowHeight = keyPreviewVisibleHeight + mParams.mVerticalGap;
                if (mParams.mTopPadding > baseRowHeight - FLOAT_THRESHOLD) {
                    // if the padding doesn't fit we'll just add it outside of the key preview
                    rowHeight = baseRowHeight;
                } else {
                    rowHeight = baseRowHeight - mParams.mTopPadding;
                }
            } else {
                final float padding = context.getResources().getDimension(
                        R.dimen.config_more_keys_keyboard_key_horizontal_padding)
                        + (key.hasLabelsInMoreKeys()
                                ? mParams.mDefaultKeyWidth * LABEL_PADDING_RATIO : 0.0f);
                keyWidth = getMaxKeyWidth(key, mParams.mDefaultKeyWidth, padding, paintToMeasure);
                rowHeight = keyboard.mMostCommonKeyHeight + keyboard.mVerticalGap;
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
