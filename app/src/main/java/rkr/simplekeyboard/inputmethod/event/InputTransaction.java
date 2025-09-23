/*
 * Copyright (C) 2014 The Android Open Source Project
 * Copyright (C) 2018 Raimondas Rimkus
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

package rkr.simplekeyboard.inputmethod.event;

import rkr.simplekeyboard.inputmethod.latin.settings.SettingsValues;

/**
 * An object encapsulating a single transaction for input.
 */
public class InputTransaction {
    // UPDATE_LATER is stronger than UPDATE_NOW. The reason for this is, if we have to update later,
    // it's because something will change that we can't evaluate now, which means that even if we
    // re-evaluate now we'll have to do it again later. The only case where that wouldn't apply
    // would be if we needed to update now to find out the new state right away, but then we
    // can't do it with this deferred mechanism anyway.
    public static final int SHIFT_NO_UPDATE = 0;
    public static final int SHIFT_UPDATE_NOW = 1;
    public static final int SHIFT_UPDATE_LATER = 2;

    // Initial conditions
    public final SettingsValues mSettingsValues;

    // Outputs
    private int mRequiredShiftUpdate = SHIFT_NO_UPDATE;

    public InputTransaction(final SettingsValues settingsValues) {
        mSettingsValues = settingsValues;
    }

    /**
     * Indicate that this transaction requires some type of shift update.
     * @param updateType What type of shift update this requires.
     */
    public void requireShiftUpdate(final int updateType) {
        mRequiredShiftUpdate = Math.max(mRequiredShiftUpdate, updateType);
    }

    /**
     * Gets what type of shift update this transaction requires.
     * @return The shift update type.
     */
    public int getRequiredShiftUpdate() {
        return mRequiredShiftUpdate;
    }
}
