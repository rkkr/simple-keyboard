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

import java.util.ArrayList;
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
    private List<SubtypePreference> mSubtypePreferences;

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
        mSubtypePreferences = new ArrayList<>();
        for (final Subtype subtype : subtypes) {
            final boolean isChecked = enabledSubtypes.contains(subtype);
            final SubtypePreference pref = createSubtypePreference(subtype, isChecked, context);
            group.addPreference(pref);
            mSubtypePreferences.add(pref);
        }

        // if there is only one subtype that is checked, the preference for it should be disabled to
        // prevent all of the subtypes for the language from being removed
        final List<SubtypePreference> checkedPrefs = getCheckedSubtypePreferences();
        if (checkedPrefs.size() == 1) {
            checkedPrefs.get(0).setEnabled(false);
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
                final List<SubtypePreference> checkedPrefs = getCheckedSubtypePreferences();
                if (checkedPrefs.size() == 1) {
                    checkedPrefs.get(0).setEnabled(false);
                }
                if (isEnabling) {
                    final boolean added = mRichImm.addSubtype(pref.getSubtype());
                    // if only one subtype was checked before, the preference would have been
                    // disabled, but now that there are two, it can be enabled to allow it to be
                    // unchecked
                    if (added && checkedPrefs.size() == 1) {
                        checkedPrefs.get(0).setEnabled(true);
                    }
                    return added;
                } else {
                    final boolean removed = mRichImm.removeSubtype(pref.getSubtype());
                    // if there is going to be only one subtype that is checked, the preference for
                    // it should be disabled to prevent all of the subtypes for the language from
                    // being removed
                    if (removed && checkedPrefs.size() == 2) {
                        final SubtypePreference onlyCheckedPref;
                        if (checkedPrefs.get(0).equals(pref)) {
                            onlyCheckedPref = checkedPrefs.get(1);
                        } else {
                            onlyCheckedPref = checkedPrefs.get(0);
                        }
                        onlyCheckedPref.setEnabled(false);
                    }
                    return removed;
                }
            }
        });

        return pref;
    }

    /**
     * Get a list of all of the subtype preferences that are currently checked.
     * @return a list of all of the subtype preferences that are checked.
     */
    private List<SubtypePreference> getCheckedSubtypePreferences() {
        final List<SubtypePreference> prefs = new ArrayList<>();
        for (final SubtypePreference pref : mSubtypePreferences) {
            if (pref.isChecked()) {
                prefs.add(pref);
            }
        }
        return prefs;
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
