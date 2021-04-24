/*
 * Copyright (C) 2013 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import rkr.simplekeyboard.inputmethod.R;

public final class IntentUtils {

    private IntentUtils() {
        // This utility class is not publicly instantiable.
    }

    public static Intent getInputLanguageSelectionIntent(final String imeId,
                                                         final Context context) {
        final CharSequence title = context.getString(R.string.language_selection_title);
        final Intent intent =
                new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
        if (!TextUtils.isEmpty(imeId)) {
            intent.putExtra(android.provider.Settings.EXTRA_INPUT_METHOD_ID, imeId);
        }
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
       return intent;
    }
}
