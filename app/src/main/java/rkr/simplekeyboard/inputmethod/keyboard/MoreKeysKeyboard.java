/*
 * Copyright (C) 2011 The Android Open Source Project
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
                + (params.mDefaultKeyPaddedWidth - params.mHorizontalGap) / 2);
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
         * @param keyPaddedWidth more keys keyboard key width in pixel, including horizontal gap.
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
        public void setParameters(final int numKeys, final int numColumn,
                                  final float keyPaddedWidth, final float rowHeight,
                                  final float coordXInParent, final int parentKeyboardWidth,
                                  final boolean isMoreKeysFixedColumn,
                                  final boolean isMoreKeysFixedOrder) {
            // Add the horizontal padding because there is no horizontal gap on the outside edge,
            // but it is included in the key width, so this compensates for simple division and
            // comparison.
            final float availableWidth = parentKeyboardWidth - mLeftPadding - mRightPadding
                    + mHorizontalGap;
            if (availableWidth < keyPaddedWidth) {
                throw new IllegalArgumentException("Keyboard is too small to hold more keys: "
                        + availableWidth + " " + keyPaddedWidth);
            }
            mIsMoreKeysFixedOrder = isMoreKeysFixedOrder;
            mDefaultKeyPaddedWidth = keyPaddedWidth;
            mDefaultRowHeight = rowHeight;

            final int maxColumns = getMaxKeys(availableWidth, keyPaddedWidth);
            if (isMoreKeysFixedColumn) {
                int requestedNumColumns = Math.min(numKeys, numColumn);
                if (maxColumns < requestedNumColumns) {
                    Log.e(TAG, "Keyboard is too small to hold the requested more keys columns: "
                            + availableWidth + " " + keyPaddedWidth + " " + numKeys + " "
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
            // Determine the maximum number of keys we can lay out on both side of the left edge of
            // a key centered on the parent key. Also, account for horizontal padding because there
            // is no horizontal gap on the outside edge.
            final float leftWidth = Math.max(coordXInParent - mLeftPadding - keyPaddedWidth / 2
                    + mHorizontalGap / 2, 0);
            final float rightWidth = Math.max(parentKeyboardWidth - coordXInParent
                    + keyPaddedWidth / 2 - mRightPadding + mHorizontalGap / 2, 0);
            int maxLeftKeys = getMaxKeys(leftWidth, keyPaddedWidth);
            int maxRightKeys = getMaxKeys(rightWidth, keyPaddedWidth);
            // Handle the case where the number of columns fits but doesn't have enough room
            // for the default key to be centered on the parent key.
            if (numKeys >= mNumColumns && mNumColumns == maxColumns
                    && maxLeftKeys + maxRightKeys < maxColumns) {
                final float extraLeft = leftWidth - maxLeftKeys * keyPaddedWidth;
                final float extraRight = rightWidth - maxRightKeys * keyPaddedWidth;
                // Put the extra key on whatever side has more space
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
            } else if (numRightKeys > maxRightKeys) {
                // Make sure the default key is included even if it doesn't exactly fit (the default
                // key just won't be completely centered on the parent key)
                rightKeys = Math.max(maxRightKeys, 1);
                leftKeys = mNumColumns - rightKeys;
            } else {
                leftKeys = numLeftKeys;
                rightKeys = numRightKeys;
            }
            mLeftKeys = leftKeys;
            mRightKeys = rightKeys;

            // Adjustment of the top row.
            mTopRowAdjustment = getTopRowAdjustment();
            mColumnWidth = mDefaultKeyPaddedWidth;
            mBaseWidth = mNumColumns * mColumnWidth;
            // Need to subtract the right most column's gutter only.
            mOccupiedWidth = Math.round(mBaseWidth + mLeftPadding + mRightPadding - mHorizontalGap);
            mBaseHeight = mNumRows * mDefaultRowHeight;
            // Need to subtract the bottom row's gutter only.
            mOccupiedHeight = Math.round(mBaseHeight + mTopPadding + mBottomPadding - mVerticalGap);

            // The proximity grid size can be reduced because the more keys keyboard is probably
            // smaller and doesn't need extra precision from smaller cells.
            mGridWidth = Math.min(mGridWidth, mNumColumns);
            mGridHeight = Math.min(mGridHeight, mNumRows);
        }

        private int getTopRowAdjustment() {
            final int numOffCenterKeys = Math.abs(mRightKeys - 1 - mLeftKeys);
            // Don't center if there are more keys in the top row than can be centered around the
            // default more key or if there is an odd number of keys in the top row (already will
            // be centered).
            if (mTopKeys > mNumColumns - numOffCenterKeys || mTopKeys % 2 == 1) {
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

        private static int getMaxKeys(final float keyboardWidth, final float keyPaddedWidth) {
            // This is effectively the same as returning (int)(keyboardWidth / keyPaddedWidth)
            // except this handles floating point errors better since rounding in the wrong
            // directing here doesn't cause an issue, but truncating incorrectly from an error
            // could be a problem (eg: the keyboard width is an exact multiple of the key width
            // could return one less than the expected number).
            final int maxKeys = Math.round(keyboardWidth / keyPaddedWidth);
            if (maxKeys * keyPaddedWidth > keyboardWidth + FLOAT_THRESHOLD) {
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

            final float keyPaddedWidth, rowHeight;
            if (isSingleMoreKeyWithPreview) {
                // Use pre-computed width and height if this more keys keyboard has only one key to
                // mitigate visual flicker between key preview and more keys keyboard.
                // The bottom paddings don't need to be considered because the vertical positions
                // of both backgrounds and the keyboard were already adjusted with their bottom
                // paddings deducted. The keyboard's left/right/top paddings do need to be deducted
                // so the key including the paddings matches the key preview.
                final float keyboardHorizontalPadding = mParams.mLeftPadding
                        + mParams.mRightPadding;
                final float baseKeyPaddedWidth = keyPreviewVisibleWidth + mParams.mHorizontalGap;
                if (keyboardHorizontalPadding > baseKeyPaddedWidth - FLOAT_THRESHOLD) {
                    // If the padding doesn't fit we'll just add it outside of the key preview.
                    keyPaddedWidth = baseKeyPaddedWidth;
                } else {
                    keyPaddedWidth = baseKeyPaddedWidth - keyboardHorizontalPadding;
                    // Keep the more keys keyboard with uneven padding lined up with the key
                    // preview rather than centering the more keys keyboard's key with the parent
                    // key.
                    mParams.mOffsetX = (mParams.mRightPadding - mParams.mLeftPadding) / 2;
                }
                final float baseKeyPaddedHeight = keyPreviewVisibleHeight + mParams.mVerticalGap;
                if (mParams.mTopPadding > baseKeyPaddedHeight - FLOAT_THRESHOLD) {
                    // If the padding doesn't fit we'll just add it outside of the key preview.
                    rowHeight = baseKeyPaddedHeight;
                } else {
                    rowHeight = baseKeyPaddedHeight - mParams.mTopPadding;
                }
            } else {
                final float defaultKeyWidth = mParams.mDefaultKeyPaddedWidth
                        - mParams.mHorizontalGap;
                final float padding = context.getResources().getDimension(
                        R.dimen.config_more_keys_keyboard_key_horizontal_padding)
                        + (key.hasLabelsInMoreKeys()
                        ? defaultKeyWidth * LABEL_PADDING_RATIO : 0.0f);
                keyPaddedWidth = getMaxKeyWidth(key, defaultKeyWidth, padding, paintToMeasure)
                        + mParams.mHorizontalGap;
                rowHeight = keyboard.mMostCommonKeyHeight + keyboard.mVerticalGap;
            }
            final MoreKeySpec[] moreKeys = key.getMoreKeys();
            mParams.setParameters(moreKeys.length, key.getMoreKeysColumnNumber(), keyPaddedWidth,
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
                final float width = params.mDefaultKeyPaddedWidth - params.mHorizontalGap;
                final float height = params.mDefaultRowHeight - params.mVerticalGap;
                final float keyLeftEdge = params.getX(n, row);
                final float keyTopEdge = params.getY(row);
                final float keyRightEdge = keyLeftEdge + width;
                final float keyBottomEdge = keyTopEdge + height;

                final float keyboardLeftEdge = params.mLeftPadding;
                final float keyboardRightEdge = params.mOccupiedWidth - params.mRightPadding;
                final float keyboardTopEdge = params.mTopPadding;
                final float keyboardBottomEdge = params.mOccupiedHeight - params.mBottomPadding;

                final float keyLeftPadding = keyLeftEdge < keyboardLeftEdge + FLOAT_THRESHOLD
                                ? params.mLeftPadding : params.mHorizontalGap / 2;
                final float keyRightPadding = keyRightEdge > keyboardRightEdge - FLOAT_THRESHOLD
                                ? params.mRightPadding : params.mHorizontalGap / 2;
                final float keyTopPadding = keyTopEdge < keyboardTopEdge + FLOAT_THRESHOLD
                                ? params.mTopPadding : params.mVerticalGap / 2;
                final float keyBottomPadding = keyBottomEdge > keyboardBottomEdge - FLOAT_THRESHOLD
                                ? params.mBottomPadding : params.mVerticalGap / 2;

                final Key key = moreKeySpec.buildKey(keyLeftEdge, keyTopEdge, width, height,
                        keyLeftPadding, keyRightPadding, keyTopPadding, keyBottomPadding,
                        moreKeyFlags);
                params.onAddKey(key);
            }
            return new MoreKeysKeyboard(params);
        }
    }
}
