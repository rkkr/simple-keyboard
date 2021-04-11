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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.AdditionalSubtypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.IntentUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

/**
 * Settings sub screen for a specific language.
 */
public final class SingleLanguageSettingsFragment extends PreferenceFragment {
    private static final String TAG = SingleLanguageSettingsFragment.class.getSimpleName();

    private static final boolean DEBUG_CUSTOM_INPUT_STYLES = false;

    public static final String LOCALE_BUNDLE_KEY = "LOCALE";

    private RichInputMethodManager mRichImm;
    private SharedPreferences mPrefs;

    private final List<SubtypePreference> mSubtypePrefs = new ArrayList<>();
    private HashSet<InputMethodSubtype> mEnabledSubtypes;
    private HashSet<InputMethodSubtype> mPrefAdditionalSubtypes;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(getActivity());
        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();
        addPreferencesFromResource(R.xml.empty_settings);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.single_language_preference_screen, container, false);
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        final Context context = getActivity();

        Button button = getActivity().findViewById(R.id.input_style_enable_prompt);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Context context = getActivity();
                final String imeId = mRichImm.getInputMethodIdOfThisIme();
                final Intent intent = IntentUtils.getInputLanguageSelectionIntent(imeId, context);
                context.startActivity(intent);
            }
        });

        final Bundle args = getArguments();
        if (args != null) {
            final String locale = getArguments().getString(LOCALE_BUNDLE_KEY);
            buildContent(locale, context);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefAdditionalSubtypes = loadPrefSubtypes();
        for (final SubtypePreference pref : mSubtypePrefs) {
            if (!pref.isEnabled()) {
                // skip default subtypes since they always exist
                continue;
            }
            pref.setChecked(mPrefAdditionalSubtypes.contains(pref.getSubtype()));
        }
        mEnabledSubtypes = loadEnabledSubtypes();
        updateEnablePrompt();
    }

    @Override
    public void onPause() {
        super.onPause();
        setAdditionalSubtypes();
    }

    /**
     * Get all of the additional subtypes that have been added.
     * @return the additional subtypes from the user preference.
     */
    private HashSet<InputMethodSubtype> loadPrefSubtypes() {
        return new HashSet<>(Arrays.asList(mRichImm.getAdditionalSubtypes()));
    }

    /**
     * Get all of the enabled subtypes of this IME.
     * @return the enabled subtypes.
     */
    private HashSet<InputMethodSubtype> loadEnabledSubtypes() {
        return new HashSet<>(mRichImm.getMyEnabledInputMethodSubtypeList(true));
    }

    /**
     * Get the default subtypes for a locale.
     * @param locale the locale for the subtypes to include.
     * @return the default subtypes for the specified locale.
     */
    private List<InputMethodSubtype> loadDefaultSubtypes(final String locale) {

        List<InputMethodSubtype> defaultSubtypes = mRichImm.getDefaultSubtypesOfThisIme();

        List<InputMethodSubtype> localeSubtypes = new ArrayList<>();
        for (final InputMethodSubtype subtype : defaultSubtypes) {
            if (!locale.equals(subtype.getLocale())) {
                continue;
            }
            localeSubtypes.add(subtype);
        }

        return localeSubtypes;
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
        final String title = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(locale);
        group.setTitle(title);
        group.removeAll();

        final PreferenceCategory mainCategory = new PreferenceCategory(context);
        mainCategory.setTitle(title);
        group.addPreference(mainCategory);

        buildSubtypePreferences(locale, group, context);
    }

    /**
     * Build the subtype preferences for a locale and them to the settings screen.
     * @param locale the locale string of the locale to add subtypes for.
     * @param group the preference group to add preferences to.
     * @param context the context for this application.
     */
    private void buildSubtypePreferences(final String locale, final PreferenceGroup group,
                                         final Context context) {
        final List<InputMethodSubtype> localeDefaultSubtypes = loadDefaultSubtypes(locale);
        final HashSet<String> localeDefaultLayoutSets = new HashSet<>();
        boolean ascii = false;
        for (final InputMethodSubtype subtype : localeDefaultSubtypes) {
            localeDefaultLayoutSets.add(SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype));
            if (subtype.isAsciiCapable()) {
                ascii = true;
            }

            final SubtypePreference pref = createSubtypePreference(subtype, true, false, context);
            group.addPreference(pref);
            mSubtypePrefs.add(pref);
        }

        if (ascii) {
            final String[] predefinedKeyboardLayoutSet = context.getResources().getStringArray(
                    R.array.predefined_layouts);

            for (final String layout : predefinedKeyboardLayoutSet) {
                if (localeDefaultLayoutSets.contains(layout)) {
                    continue;
                }

                InputMethodSubtype genericSubtype =
                        AdditionalSubtypeUtils.createAdditionalSubtype(locale, layout);
                final SubtypePreference pref =
                        createSubtypePreference(genericSubtype, false, true, context);
                group.addPreference(pref);
                mSubtypePrefs.add(pref);
            }
        }
    }

    /**
     * Create a preference for a keyboard layout subtype.
     * @param subtype the subtype that the preference enables.
     * @param isChecked whether the preference should start as checked (subtype enabled).
     * @param isEnabled whether the preference is clickable. This should only be true for custom
     *                  subtypes.
     * @param context the context for this application.
     * @return the preference that was created.
     */
    private SubtypePreference createSubtypePreference(final InputMethodSubtype subtype,
                                                      final boolean isChecked,
                                                      final boolean isEnabled,
                                                      final Context context) {
        final SubtypePreference pref = new SubtypePreference(context, subtype);
        pref.setTitle(SubtypeLocaleUtils.getKeyboardLayoutDisplayName(subtype, context));
        pref.setChecked(isChecked);
        pref.setEnabled(isEnabled);

        if (isEnabled) {
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    updateEnablePrompt();

                    final SubtypePreference pref = (SubtypePreference) preference;
                    if (pref.isChecked()) {
                        mPrefAdditionalSubtypes.add(pref.getSubtype());
                    } else {
                        mPrefAdditionalSubtypes.remove(pref.getSubtype());
                    }
                    final InputMethodSubtype[] subtypes =
                            mPrefAdditionalSubtypes.toArray(new InputMethodSubtype[0]);
                    final String prefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(subtypes);
                    if (DEBUG_CUSTOM_INPUT_STYLES) {
                        Log.i(TAG, "Save custom input styles: " + prefSubtypes);
                    }
                    Settings.writePrefAdditionalSubtypes(mPrefs, prefSubtypes);
                    // The subtypes will be actually set in the input method manager later in
                    // onPause so that if a user unchecks an enabled subtype and then checks it
                    // again before leaving the activity, the user isn't forced to go re-enable it,
                    // since nothing really changed

                    return true;
                }
            });
        }

        return pref;
    }

    /**
     * Check if any subtypes need to be enabled in the system, and if so, show the button to go to
     * the system setting to enable it.
     */
    private void updateEnablePrompt() {
        final boolean needsEnabling = subtypeNeedsEnabling();
        Button button = getActivity().findViewById(R.id.input_style_enable_prompt);
        button.setVisibility(needsEnabling ? View.VISIBLE : View.GONE);
    }

    /**
     * Check if any additional subtypes have been enabled internally but still need to be enabled in
     * the system or if no subtypes have been enabled in the system (including default subtypes).
     * @return whether any subtype needs to be enabled.
     */
    private boolean subtypeNeedsEnabling() {
        boolean hasDefaultSubtypesEnabled = false;
        boolean hasAdditionalSubtypesEnabled = false;
        for (final SubtypePreference pref : mSubtypePrefs) {
            if (!pref.isChecked()) {
                // unused subtypes don't need to be enabled
                continue;
            }
            if (!mEnabledSubtypes.contains(pref.getSubtype())) {
                if (pref.isEnabled()) {
                    // additional subtype needs enabling
                    return true;
                }
                // default subtypes should only be flagged as needing enabling if no other subtypes
                // are used
                continue;
            }
            if (pref.isEnabled()) {
                hasAdditionalSubtypesEnabled = true;
            } else {
                hasDefaultSubtypesEnabled = true;
            }
        }
        return !hasAdditionalSubtypesEnabled && !hasDefaultSubtypesEnabled;
    }

    /**
     * Update the additional subtypes preference if any changes have been made to enable or disable
     * alternate keyboard layouts for the language on the current settings screen. This also creates
     * or removes the additional subtype in the system.
     */
    private void setAdditionalSubtypes() {
        boolean hasChanges = false;
        for (final SubtypePreference pref : mSubtypePrefs) {
            if (!pref.isEnabled()) {
                continue;
            }
            final boolean subtypeExists = mEnabledSubtypes.contains(pref.getSubtype());
            if (!subtypeExists && pref.isChecked()) {
                // creating subtype
                hasChanges = true;
                break;
            } else if (subtypeExists && !pref.isChecked()) {
                // removing subtype
                hasChanges = true;
                break;
            }
        }
        if (!hasChanges) {
            return;
        }

        final InputMethodSubtype[] subtypes =
                mPrefAdditionalSubtypes.toArray(new InputMethodSubtype[0]);
        if (DEBUG_CUSTOM_INPUT_STYLES) {
            final String prefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(subtypes);
            Log.i(TAG, "Setting custom input styles: " + prefSubtypes);
        }
        mRichImm.setAdditionalInputMethodSubtypes(subtypes);
    }

    /**
     * Preference for a keyboard layout.
     */
    private static class SubtypePreference extends SwitchPreference {
        final InputMethodSubtype mSubtype;

        /**
         * Create a subtype preference.
         * @param context the context for this application.
         * @param subtype the subtype to create the preference for.
         */
        public SubtypePreference(final Context context, final InputMethodSubtype subtype) {
            super(context);
            mSubtype = subtype;
        }

        /**
         * Get the subtype that this preference represents.
         * @return the subtype.
         */
        public InputMethodSubtype getSubtype() {
            return mSubtype;
        }
    }
}
