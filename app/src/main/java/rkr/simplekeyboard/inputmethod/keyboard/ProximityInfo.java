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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProximityInfo {
    private static final List<Key> EMPTY_KEY_LIST = Collections.emptyList();

    private final int mGridWidth;
    private final int mGridHeight;
    private final int mGridSize;
    private final int mCellWidth;
    private final int mCellHeight;
    // TODO: Find a proper name for mKeyboardMinWidth
    private final int mKeyboardMinWidth;
    private final int mKeyboardHeight;
    private final List<Key> mSortedKeys;
    private final List<Key>[] mGridNeighbors;

    @SuppressWarnings("unchecked")
    ProximityInfo(final int gridWidth, final int gridHeight, final int minWidth, final int height,
            final List<Key> sortedKeys) {
        mGridWidth = gridWidth;
        mGridHeight = gridHeight;
        mGridSize = mGridWidth * mGridHeight;
        mCellWidth = (minWidth + mGridWidth - 1) / mGridWidth;
        mCellHeight = (height + mGridHeight - 1) / mGridHeight;
        mKeyboardMinWidth = minWidth;
        mKeyboardHeight = height;
        mSortedKeys = sortedKeys;
        mGridNeighbors = new List[mGridSize];
        if (minWidth == 0 || height == 0) {
            // No proximity required. Keyboard might be more keys keyboard.
            return;
        }
        computeNearestNeighbors();
    }

    private void computeNearestNeighbors() {
        final int keyCount = mSortedKeys.size();
        final int gridSize = mGridNeighbors.length;
        final int maxKeyRight = mGridWidth * mCellWidth;
        final int maxKeyBottom = mGridHeight * mCellHeight;

        // For large layouts, 'neighborsFlatBuffer' is about 80k of memory: gridSize is usually 512,
        // keycount is about 40 and a pointer to a Key is 4 bytes. This contains, for each cell,
        // enough space for as many keys as there are on the keyboard. Hence, every
        // keycount'th element is the start of a new cell, and each of these virtual subarrays
        // start empty with keycount spaces available. This fills up gradually in the loop below.
        // Since in the practice each cell does not have a lot of neighbors, most of this space is
        // actually just empty padding in this fixed-size buffer.
        final Key[] neighborsFlatBuffer = new Key[gridSize * keyCount];
        final int[] neighborCountPerCell = new int[gridSize];
        for (final Key key : mSortedKeys) {
            if (key.isSpacer()) continue;

            // Iterate through all of the cells that overlap with the clickable region of the
            // current key and add the key as a neighbor.
            final int keyX = key.getX();
            final int keyY = key.getY();
            final int keyTop = keyY - key.getTopPadding();
            final int keyBottom = Math.min(keyY + key.getHeight() + key.getBottomPadding(),
                    maxKeyBottom);
            final int keyLeft = keyX - key.getLeftPadding();
            final int keyRight = Math.min(keyX + key.getWidth() + key.getRightPadding(),
                    maxKeyRight);
            final int yDeltaToGrid = keyTop % mCellHeight;
            final int xDeltaToGrid = keyLeft % mCellWidth;
            final int yStart = keyTop - yDeltaToGrid;
            final int xStart = keyLeft - xDeltaToGrid;
            int baseIndexOfCurrentRow = (yStart / mCellHeight) * mGridWidth + (xStart / mCellWidth);
            for (int cellTop = yStart; cellTop < keyBottom; cellTop += mCellHeight) {
                int index = baseIndexOfCurrentRow;
                for (int cellLeft = xStart; cellLeft < keyRight; cellLeft += mCellWidth) {
                    neighborsFlatBuffer[index * keyCount + neighborCountPerCell[index]] = key;
                    ++neighborCountPerCell[index];
                    ++index;
                }
                baseIndexOfCurrentRow += mGridWidth;
            }
        }

        for (int i = 0; i < gridSize; ++i) {
            final int indexStart = i * keyCount;
            final int indexEnd = indexStart + neighborCountPerCell[i];
            final ArrayList<Key> neighbors = new ArrayList<>(indexEnd - indexStart);
            for (int index = indexStart; index < indexEnd; index++) {
                neighbors.add(neighborsFlatBuffer[index]);
            }
            mGridNeighbors[i] = Collections.unmodifiableList(neighbors);
        }
    }

    public List<Key> getNearestKeys(final int x, final int y) {
        if (x >= 0 && x < mKeyboardMinWidth && y >= 0 && y < mKeyboardHeight) {
            int index = (y / mCellHeight) * mGridWidth + (x / mCellWidth);
            if (index < mGridSize) {
                return mGridNeighbors[index];
            }
        }
        return EMPTY_KEY_LIST;
    }
}
