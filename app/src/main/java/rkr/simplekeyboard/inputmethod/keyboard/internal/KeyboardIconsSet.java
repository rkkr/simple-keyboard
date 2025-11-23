/*
 * Copyright (C) 2011 The Android Open Source Project
 * Copyright (C) 2019 Raimondas Rimkus
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
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.SparseIntArray;

import java.util.HashMap;

import rkr.simplekeyboard.inputmethod.R;

public final class KeyboardIconsSet {
    private static final String TAG = KeyboardIconsSet.class.getSimpleName();

    public static final String PREFIX_ICON = "!icon/";
    public static final int ICON_UNDEFINED = 0;
    private static final int ATTR_UNDEFINED = 0;

    private static final String NAME_UNDEFINED = "undefined";
    public static final String NAME_SHIFT_KEY = "shift_key";
    public static final String NAME_SHIFT_KEY_SHIFTED = "shift_key_shifted";
    public static final String NAME_DELETE_KEY = "delete_key";
    public static final String NAME_SETTINGS_KEY = "settings_key";
    public static final String NAME_SPACE_KEY = "space_key";
    public static final String NAME_SPACE_KEY_FOR_NUMBER_LAYOUT = "space_key_for_number_layout";
    public static final String NAME_ENTER_KEY = "enter_key";
    public static final String NAME_GO_KEY = "go_key";
    public static final String NAME_SEARCH_KEY = "search_key";
    public static final String NAME_SEND_KEY = "send_key";
    public static final String NAME_NEXT_KEY = "next_key";
    public static final String NAME_DONE_KEY = "done_key";
    public static final String NAME_PREVIOUS_KEY = "previous_key";
    public static final String NAME_TAB_KEY = "tab_key";
    public static final String NAME_LANGUAGE_SWITCH_KEY = "language_switch_key";
    public static final String NAME_ZWNJ_KEY = "zwnj_key";
    public static final String NAME_ZWJ_KEY = "zwj_key";

    private static final SparseIntArray ATTR_ID_TO_ICON_ID = new SparseIntArray();

    // Icon name to icon id map.
    private static final HashMap<String, Integer> sNameToIdsMap = new HashMap<>();

    private static final Object[] NAMES_AND_ATTR_IDS = {
        NAME_UNDEFINED,                   ATTR_UNDEFINED,
        NAME_SHIFT_KEY,                   R.drawable.sym_keyboard_shift,
        NAME_DELETE_KEY,                  R.drawable.sym_keyboard_delete,
        NAME_SETTINGS_KEY,                R.drawable.sym_keyboard_settings,
        NAME_SPACE_KEY,                   ATTR_UNDEFINED,
        NAME_ENTER_KEY,                   R.drawable.sym_keyboard_return,
        NAME_GO_KEY,                      R.drawable.sym_keyboard_go,
        NAME_SEARCH_KEY,                  R.drawable.sym_keyboard_search,
        NAME_SEND_KEY,                    R.drawable.sym_keyboard_send,
        NAME_NEXT_KEY,                    R.drawable.sym_keyboard_next,
        NAME_DONE_KEY,                    R.drawable.sym_keyboard_done,
        NAME_PREVIOUS_KEY,                R.drawable.sym_keyboard_previous,
        NAME_TAB_KEY,                     R.drawable.sym_keyboard_tab,
        NAME_SPACE_KEY_FOR_NUMBER_LAYOUT, R.drawable.sym_keyboard_space,
        NAME_SHIFT_KEY_SHIFTED,           R.drawable.sym_keyboard_shift_locked,
        NAME_LANGUAGE_SWITCH_KEY,         R.drawable.sym_keyboard_language_switch,
        NAME_ZWNJ_KEY,                    R.drawable.sym_keyboard_zwnj,
        NAME_ZWJ_KEY,                     R.drawable.sym_keyboard_zwj,
    };

    private static final int NUM_ICONS = NAMES_AND_ATTR_IDS.length / 2;
    private static final String[] ICON_NAMES = new String[NUM_ICONS];
    private final Drawable[] mIcons = new Drawable[NUM_ICONS];

    static {
        int iconId = ICON_UNDEFINED;
        for (int i = 0; i < NAMES_AND_ATTR_IDS.length; i += 2) {
            final String name = (String)NAMES_AND_ATTR_IDS[i];
            final Integer attrId = (Integer)NAMES_AND_ATTR_IDS[i + 1];
            if (attrId != ATTR_UNDEFINED) {
                ATTR_ID_TO_ICON_ID.put(attrId, iconId);
            }
            sNameToIdsMap.put(name, iconId);
            ICON_NAMES[iconId] = name;
            iconId++;
        }
    }

    public void loadIcons(final Resources resources, final Resources.Theme theme) {
        final int size = ATTR_ID_TO_ICON_ID.size();
        for (int index = 0; index < size; index++) {
            final int attrId = ATTR_ID_TO_ICON_ID.keyAt(index);
            try {
                final Drawable icon = resources.getDrawable(attrId, theme);
                setDefaultBounds(icon);
                final Integer iconId = ATTR_ID_TO_ICON_ID.get(attrId);
                mIcons[iconId] = icon;
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Drawable resource for icon #"
                        + resources.getResourceEntryName(attrId)
                        + " not found");
            }
        }
    }

    private static boolean isValidIconId(final int iconId) {
        return iconId >= 0 && iconId < ICON_NAMES.length;
    }

    public static String getIconName(final int iconId) {
        return isValidIconId(iconId) ? ICON_NAMES[iconId] : "unknown<" + iconId + ">";
    }

    public static int getIconId(final String name) {
        Integer iconId = sNameToIdsMap.get(name);
        if (iconId != null) {
            return iconId;
        }
        throw new RuntimeException("unknown icon name: " + name);
    }

    public Drawable getIconDrawable(final int iconId) {
        if (isValidIconId(iconId)) {
            return mIcons[iconId];
        }
        throw new RuntimeException("unknown icon id: " + getIconName(iconId));
    }

    private static void setDefaultBounds(final Drawable icon)  {
        if (icon != null) {
            icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        }
    }
}
