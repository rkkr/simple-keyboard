/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

/**
 * Settings sub screen for a specific language.
 */
public final class SingleLanguageSettingsFragment extends PreferenceFragment {

    public static final String LOCALE_BUNDLE_KEY = "LOCALE";

    private RichInputMethodManager mRichImm;
    private Subtype mSubtypeToRemove;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();
        addPreferencesFromResource(R.xml.empty_settings);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        final Context context = getActivity();

        final Bundle args = getArguments();
        if (args != null) {
            final String locale = getArguments().getString(LOCALE_BUNDLE_KEY);
            buildContent(locale, context);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onPause() {
        if (mSubtypeToRemove != null) {
            // notify the user that the unchecked subtype wasn't actually removed since nothing else
            // was added to replace it
            Toast.makeText(SingleLanguageSettingsFragment.this.getActivity(),
                    R.string.layout_not_disabled, Toast.LENGTH_SHORT).show();
            // set the corresponding preference to be checked in case the user returns to this
            // activity
            final PreferenceGroup group = getPreferenceScreen();
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                if (!(group.getPreference(i) instanceof SubtypePreference)) {
                    continue;
                }
                final SubtypePreference pref = (SubtypePreference) group.getPreference(i);
                if (mSubtypeToRemove.equals(pref.getSubtype())) {
                    pref.setChecked(true);
                    break;
                }
            }
        }
        super.onPause();
    }

    /**
     * Build the preferences and them to this settings screen.
     * @param locale the locale string of the locale to display content for.
     * @param context the context for this application.
     */
    private void buildContent(final String locale, final Context context) {
        if (locale == null) {
            return;
        }
        final PreferenceGroup group = getPreferenceScreen();
        group.removeAll();

        final PreferenceCategory mainCategory = new PreferenceCategory(context);
        final String localeName = LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(locale);
        mainCategory.setTitle(context.getString(R.string.generic_language_layouts, localeName));
        group.addPreference(mainCategory);

        buildSubtypePreferences(locale, group, context);
    }

    /**
     * Build preferences for all of the available subtypes for a locale and them to the settings
     * screen.
     * @param locale the locale string of the locale to add subtypes for.
     * @param group the preference group to add preferences to.
     * @param context the context for this application.
     */
    private void buildSubtypePreferences(final String locale, final PreferenceGroup group,
                                         final Context context) {
        final Set<Subtype> enabledSubtypes = mRichImm.getEnabledSubtypes(false);
        final List<Subtype> subtypes =
                SubtypeLocaleUtils.getSubtypes(locale, context.getResources());
        for (final Subtype subtype : subtypes) {
            final boolean isEnabled = enabledSubtypes.contains(subtype);
            final SubtypePreference pref = createSubtypePreference(subtype, isEnabled, context);
            group.addPreference(pref);
        }
    }

    /**
     * Create a preference for a keyboard layout subtype.
     * @param subtype the subtype that the preference enables.
     * @param checked whether the preference should be initially checked.
     * @param context the context for this application.
     * @return the preference that was created.
     */
    private SubtypePreference createSubtypePreference(final Subtype subtype,
                                                      final boolean checked,
                                                      final Context context) {
        final SubtypePreference pref = new SubtypePreference(context, subtype);
        pref.setTitle(subtype.getLayoutDisplayName());
        pref.setChecked(checked);

        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                if (!(newValue instanceof Boolean)) {
                    return false;
                }
                final boolean isEnabling = (boolean)newValue;
                final SubtypePreference pref = (SubtypePreference) preference;
                if (isEnabling) {
                    final boolean added = mRichImm.addSubtype(pref.getSubtype());
                    // remove the subtype that is pending to be removed (already unchecked)
                    if (added && mSubtypeToRemove != null
                            && (mSubtypeToRemove.equals(pref.getSubtype())
                            || mRichImm.removeSubtype(mSubtypeToRemove))) {
                        mSubtypeToRemove = null;
                    }
                    return added;
                } else {
                    final boolean removed = mRichImm.removeSubtype(pref.getSubtype());
                    if (!removed) {
                        // Allow the preference to be unchecked even though the subtype isn't
                        // actually removed. It will actually be removed when a different subtype
                        // is checked.
                        mSubtypeToRemove = pref.getSubtype();
                    }
                    return true;
                }
            }
        });

        return pref;
    }

    /**
     * Preference for a keyboard layout.
     */
    private static class SubtypePreference extends SwitchPreference {
        final Subtype mSubtype;

        /**
         * Create a subtype preference.
         * @param context the context for this application.
         * @param subtype the subtype to create the preference for.
         */
        public SubtypePreference(final Context context, final Subtype subtype) {
            super(context);
            mSubtype = subtype;
        }

        /**
         * Get the subtype that this preference represents.
         * @return the subtype.
         */
        public Subtype getSubtype() {
            return mSubtype;
        }
    }
}
