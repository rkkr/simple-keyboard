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
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import java.util.Set;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;

/* package private */ class InputMethodSettingsImpl {
    private Preference mSubtypeEnablerPreference;
    private RichInputMethodManager mRichImm;

    /**
     * Initialize internal states of this object.
     * @param context the context for this application.
     * @param prefScreen a PreferenceScreen of PreferenceActivity or PreferenceFragment.
     * @return true if this application is an IME and has two or more subtypes, false otherwise.
     */
    public boolean init(final Context context, final PreferenceScreen prefScreen) {
        RichInputMethodManager.init(context);
        mRichImm = RichInputMethodManager.getInstance();

        mSubtypeEnablerPreference = new Preference(context);
        mSubtypeEnablerPreference.setTitle(R.string.select_language);
        mSubtypeEnablerPreference.setFragment(LanguagesSettingsFragment.class.getName());
        prefScreen.addPreference(mSubtypeEnablerPreference);
        updateEnabledSubtypeList();
        return true;
    }

    private static String getEnabledSubtypesLabel(final RichInputMethodManager richImm) {
        if (richImm == null) {
            return null;
        }

        final Set<Subtype> subtypes = richImm.getEnabledSubtypes(true);

        final StringBuilder sb = new StringBuilder();
        for (final Subtype subtype : subtypes) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(subtype.getName());
        }
        return sb.toString();
    }

    public void updateEnabledSubtypeList() {
        if (mSubtypeEnablerPreference != null) {
            final String summary = getEnabledSubtypesLabel(mRichImm);
            if (!TextUtils.isEmpty(summary)) {
                mSubtypeEnablerPreference.setSummary(summary);
            }
        }
    }
}
