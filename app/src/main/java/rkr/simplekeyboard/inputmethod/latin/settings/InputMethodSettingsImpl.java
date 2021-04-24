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

package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.List;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.utils.IntentUtils;

/* package private */ class InputMethodSettingsImpl {
    private Preference mSubtypeEnablerPreference;
    private InputMethodManager mImm;
    private InputMethodInfo mImi;
    private Context mContext;

    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    public boolean init(final Context context, final PreferenceScreen prefScreen) {
        mContext = context;
        mImm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mImi = getMyImi(context, mImm);
        if (mImi == null || mImi.getSubtypeCount() <= 1) {
            return false;
        }
        mSubtypeEnablerPreference = new Preference(context);
        mSubtypeEnablerPreference.setTitle(R.string.select_language);
        mSubtypeEnablerPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final Intent intent =
                                IntentUtils.getInputLanguageSelectionIntent(mImi.getId(), context);
                        context.startActivity(intent);
                        return true;
                    }
                });
        prefScreen.addPreference(mSubtypeEnablerPreference);
        updateEnabledSubtypeList();
        return true;
    }

    private static InputMethodInfo getMyImi(Context context, InputMethodManager imm) {
        final List<InputMethodInfo> imis = imm.getInputMethodList();
        for (int i = 0; i < imis.size(); ++i) {
            final InputMethodInfo imi = imis.get(i);
            if (imis.get(i).getPackageName().equals(context.getPackageName())) {
                return imi;
            }
        }
        return null;
    }

    private static String getEnabledSubtypesLabel(
            Context context, InputMethodManager imm, InputMethodInfo imi) {
        if (context == null || imm == null || imi == null) return null;
        final List<InputMethodSubtype> subtypes = imm.getEnabledInputMethodSubtypeList(imi, true);
        final StringBuilder sb = new StringBuilder();
        final int N = subtypes.size();
        for (int i = 0; i < N; ++i) {
            final InputMethodSubtype subtype = subtypes.get(i);
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(subtype.getDisplayName(context, imi.getPackageName(),
                    imi.getServiceInfo().applicationInfo));
        }
        return sb.toString();
    }

    public void updateEnabledSubtypeList() {
        if (mSubtypeEnablerPreference != null) {
            final String summary = getEnabledSubtypesLabel(mContext, mImm, mImi);
            if (!TextUtils.isEmpty(summary)) {
                mSubtypeEnablerPreference.setSummary(summary);
            }
        }
    }
}
