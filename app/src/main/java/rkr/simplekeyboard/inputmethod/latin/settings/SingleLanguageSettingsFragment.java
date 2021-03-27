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
import android.view.inputmethod.InputMethodInfo;
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

        mPrefAdditionalSubtypes = loadPrefSubtypes();

        final Bundle args = getArguments();
        if (args != null) {
            final String locale = getArguments().getString(LOCALE_BUNDLE_KEY);
            setContent(locale, context);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mPrefAdditionalSubtypes = loadPrefSubtypes();
        for (SubtypePreference pref : mSubtypePrefs) {
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

    private HashSet<InputMethodSubtype> loadPrefSubtypes() {
        return new HashSet<>(Arrays.asList(mRichImm.getAdditionalSubtypes()));
    }


    private HashSet<InputMethodSubtype> loadEnabledSubtypes() {
        return new HashSet<>(mRichImm.getMyEnabledInputMethodSubtypeList(true));
    }

    private List<InputMethodSubtype> loadDefaultSubtypes(final String locale) {
        final InputMethodInfo imi = mRichImm.getInputMethodInfoOfThisIme();
        final int count = imi.getSubtypeCount();
        List<InputMethodSubtype> localeSubtypes = new ArrayList<>();
        //TODO: fix the order of the items - they probably should be in some fixed order
        for (int i = 0; i < count; i++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(i);
            if (locale != null && !locale.equals(subtype.getLocale())) {
                continue;
            }

            //TODO: try to find a way to get the default subtypes that doesn't involve checking the preferences so it doesn't need to load twice when opening the fragment
            if (mPrefAdditionalSubtypes.contains(subtype)) {
                continue;
            }

            localeSubtypes.add(subtype);
        }

        return localeSubtypes;
    }

    private void setContent(final String locale, final Context context) {
        final PreferenceGroup group = getPreferenceScreen();
        final String title = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(locale);
        group.setTitle(title);
        group.removeAll();

        final PreferenceCategory mainCategory = new PreferenceCategory(context);
        mainCategory.setTitle(title);
        group.addPreference(mainCategory);

        addSubtypePreferences(locale, context);
    }

    private void addSubtypePreferences(final String locale, final Context context) {
        if (locale == null) {
            return;
        }
        final PreferenceGroup group = getPreferenceScreen();

        List<InputMethodSubtype> localeSubtypes = loadDefaultSubtypes(locale);
        boolean ascii = false;
        //TODO: fix the order of the items - they probably should be in some fixed order - this seems to match method.xml, so this might be fine
        for (InputMethodSubtype subtype : localeSubtypes) {
            if (subtype.isAsciiCapable()) {
                ascii = true;
            }
            createSubtypePreference(subtype, true, false, group, context);
        }
        if (ascii) {
            final String[] predefinedKeyboardLayoutSet = context.getResources().getStringArray(
                    R.array.predefined_layouts);
            //TODO: fix the order of the items - they probably should be in some fixed order - maybe the order of the array above is fine
            for (final String layout : predefinedKeyboardLayoutSet) {
                InputMethodSubtype genericSubtype =
                        AdditionalSubtypeUtils.createAdditionalSubtype(locale, layout);

                //TODO: maybe use SubtypeLocaleUtils.getKeyboardLayoutSetName
                if (isDuplicateSubtype(genericSubtype, localeSubtypes)) {
                    continue;
                }

                createSubtypePreference(genericSubtype, false, true, group, context);
            }
        }
    }

    private boolean isDuplicateSubtype(final InputMethodSubtype subtype, final List<InputMethodSubtype> subtypes) {
        final String layoutName = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype);
        final String localeString = subtype.getLocale();
        for (InputMethodSubtype curSubtype : subtypes) {
            final String curLayoutName = SubtypeLocaleUtils.getKeyboardLayoutSetName(curSubtype);
            //TODO: the locale check is probably unnecessary for how this is used
            if (localeString.equals(curSubtype.getLocale())
                    && layoutName.equals(curLayoutName)) {
                return true;
            }
        }
        return false;
    }

    private void createSubtypePreference(final InputMethodSubtype subtype, final boolean isChecked,
                                         final boolean isEnabled, final PreferenceGroup group,
                                         final Context context) {
        final SubtypePreference pref = new SubtypePreference(context, subtype);
        pref.setTitle(SubtypeLocaleUtils.getKeyboardLayoutDisplayName(subtype, context));
        pref.setChecked(isChecked);
        pref.setEnabled(isEnabled);
        group.addPreference(pref);

        mSubtypePrefs.add(pref);

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                updateEnablePrompt();

                final SubtypePreference pref = (SubtypePreference)preference;
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
                // The subtypes will be actually set in the input method manager later in onPause
                // so that if a user unchecks an enabled subtype and then checks it again before
                // leaving the activity, the user isn't forced to go re-enable it, since nothing
                // really changed

                return true;
            }
        });
    }

    private void updateEnablePrompt() {
        final boolean needsEnabling = subtypeNeedsEnabling();
        Button button = getActivity().findViewById(R.id.input_style_enable_prompt);
        button.setVisibility(needsEnabling ? View.VISIBLE : View.GONE);
    }

    private boolean subtypeNeedsEnabling() {
        boolean hasDefaultSubtypesEnabled = false;
        boolean hasAdditionalSubtypesEnabled = false;
        for (SubtypePreference pref : mSubtypePrefs) {
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

    private void setAdditionalSubtypes() {
        boolean hasChanges = false;
        for (SubtypePreference pref : mSubtypePrefs) {
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

    private static class SubtypePreference extends SwitchPreference {
        final InputMethodSubtype mSubtype;

        public SubtypePreference(final Context context, final InputMethodSubtype subtype) {
            super(context);
            mSubtype = subtype;
        }

        public InputMethodSubtype getSubtype() {
            return mSubtype;
        }
    }
}
