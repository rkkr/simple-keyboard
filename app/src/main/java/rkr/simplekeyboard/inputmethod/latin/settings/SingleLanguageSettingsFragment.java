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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.MySubtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeUtils;

/**
 * Settings sub screen for a specific language.
 */
public final class SingleLanguageSettingsFragment extends PreferenceFragment {
    private static final String TAG = SingleLanguageSettingsFragment.class.getSimpleName();

    public static final String LOCALE_BUNDLE_KEY = "LOCALE";

    private RichInputMethodManager mRichImm;

    private final List<SubtypePreference> mSubtypePrefs = new ArrayList<>();

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
            //TODO: delete - testing
//            testLogSubtypes();
//            perfTestSubtypeCreation();
//            enableAllSubtypes();
//            disableAllSubtypes();
//            enableAllDefaultSubtypes();
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Set<MySubtype> enabledSubtypes = mRichImm.getEnabledSubtypesOfThisIme(false);
        for (final SubtypePreference pref : mSubtypePrefs) {
            pref.setChecked(enabledSubtypes.contains(pref.getSubtype()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void perfTestSubtypeCreation() {
        final List<String> supportedLocales = SubtypeUtils.getSupportedLocales();

        final long[] times = new long[supportedLocales.size()];
        final int numRuns = 10000;

        for (int localeIndex = 0; localeIndex < supportedLocales.size(); localeIndex++) {
            final String supportedLocale = supportedLocales.get(localeIndex);
            long start = System.currentTimeMillis();
            for (int runIndex = 0; runIndex < numRuns; runIndex++) {
                MySubtype subtype = SubtypeUtils.getDefaultSubtype(supportedLocale, getActivity().getResources());
            }
            long finish = System.currentTimeMillis();
            times[localeIndex] = finish - start;
        }

        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        long totalTime = 0;
        for (final long time : times) {
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
            totalTime += time;
        }
        Log.w(TAG, "perfTestSubtypeCreation: minTime=" + String.format("%.3f", (double)minTime / numRuns) + "ms");
        Log.w(TAG, "perfTestSubtypeCreation: maxTime=" + String.format("%.3f", (double)maxTime / numRuns) + "ms");
        Log.w(TAG, "perfTestSubtypeCreation: averageTime=" + String.format("%.3f", (double)totalTime / times.length / numRuns) + "ms");
        Log.w(TAG, "perfTestSubtypeCreation: totalTime=" + totalTime + "ms runs=" + numRuns);
    }

    private void enableAllSubtypes() {
        final List<String> supportedLocales = SubtypeUtils.getSupportedLocales();

        long start = System.currentTimeMillis();
        for (final String supportedLocale : supportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            for (final MySubtype subtype : subtypes) {
                mRichImm.addSubtype(subtype);
            }
        }
        long finish = System.currentTimeMillis();
        Log.w(TAG, "enableAllSubtypes: " + (finish - start) + "ms");
    }

    private void disableAllSubtypes() {
        final List<String> supportedLocales = SubtypeUtils.getSupportedLocales();

        long start = System.currentTimeMillis();
        for (final String supportedLocale : supportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            for (final MySubtype subtype : subtypes) {
                mRichImm.removeSubtype(subtype);
            }
        }
        long finish = System.currentTimeMillis();
        Log.w(TAG, "disableAllSubtypes: " + (finish - start) + "ms");
    }

    private void enableAllDefaultSubtypes() {
        final List<String> supportedLocales = SubtypeUtils.getSupportedLocales();

        long start = System.currentTimeMillis();
        for (final String supportedLocale : supportedLocales) {
            MySubtype subtype = SubtypeUtils.getDefaultSubtype(supportedLocale, getActivity().getResources());
            mRichImm.addSubtype(subtype);
        }
        long finish = System.currentTimeMillis();
        Log.w(TAG, "enableAllDefaultSubtypes: " + (finish - start) + "ms");
    }

    private void testLogSubtypes() {
        final List<String> supportedLocales = SubtypeUtils.getSupportedLocales();

        List<MySubtype> defaultSubtypes = new ArrayList<>();
        for (final String supportedLocale : supportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            defaultSubtypes.add(subtypes.get(0));
            if (supportedLocale.equals("bg") || supportedLocale.equals("hi") || supportedLocale.equals("ne_NP")) {
                defaultSubtypes.add(subtypes.get(1));
            }
        }
        printSubtypes(defaultSubtypes, "default");

        List<MySubtype> allJavaSubtypes = new ArrayList<>();
        for (final String supportedLocale : supportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            allJavaSubtypes.addAll(subtypes);
        }
        printSubtypes(allJavaSubtypes, "all");
    }

    private void printSubtypes(Collection<MySubtype> javaSubtypes, String prefix) {
        Log.w(TAG, "printSubtypes: " + prefix + ": size=" + javaSubtypes.size());
        for (final MySubtype subtype : javaSubtypes) {
            Log.w(TAG, "printSubtypes: " + prefix + ": getName=" + subtype.getName());
            Log.w(TAG, "printSubtypes: " + prefix + ": getLocale=" + subtype.getLocale());
            Log.w(TAG, "printSubtypes: " + prefix + ": getLayoutDisplayName=" + subtype.getLayoutDisplayName());
            Log.w(TAG, "printSubtypes: " + prefix + ": getLocaleDisplayNameInLocale=" + subtype.getLocaleDisplayNameInLocale());
            Log.w(TAG, "printSubtypes: " + prefix + ": getLanguageDisplayNameInLocale=" + subtype.getLanguageDisplayNameInLocale());
            Log.w(TAG, "printSubtypes: " + prefix + ": getLayoutSet=" + subtype.getLayoutSet());
        }
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
        final String title = context.getString(R.string.generic_language_layouts,
                SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(locale));
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
        final List<MySubtype> subtypes = SubtypeUtils.getSubtypes(locale, context.getResources());
        for (final MySubtype subtype : subtypes) {
            final SubtypePreference pref = createSubtypePreference(subtype, context);
            group.addPreference(pref);
            mSubtypePrefs.add(pref);
        }
    }

    /**
     * Create a preference for a keyboard layout subtype.
     * @param subtype the subtype that the preference enables.
     * @param context the context for this application.
     * @return the preference that was created.
     */
    private SubtypePreference createSubtypePreference(final MySubtype subtype,
                                                      final Context context) {
        final SubtypePreference pref = new SubtypePreference(context, subtype);
        pref.setTitle(subtype.getLayoutDisplayName());
        pref.setChecked(false);
        pref.setEnabled(true);

        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                if (!(newValue instanceof Boolean)) {
                    return false;
                }
                final boolean isEnabling = (boolean)newValue;
                final SubtypePreference pref = (SubtypePreference) preference;
                if (isEnabling) {
                    return mRichImm.addSubtype(pref.getSubtype());
                } else {
                    final boolean removed = mRichImm.removeSubtype(pref.getSubtype());
                    if (!removed) {
                        Toast.makeText(SingleLanguageSettingsFragment.this.getActivity(),
                                R.string.layout_not_disabled, Toast.LENGTH_SHORT).show();
                    }
                    return removed;
                }
            }
        });

        return pref;
    }

    /**
     * Preference for a keyboard layout.
     */
    private static class SubtypePreference extends SwitchPreference {
        final MySubtype mSubtype;

        /**
         * Create a subtype preference.
         * @param context the context for this application.
         * @param subtype the subtype to create the preference for.
         */
        public SubtypePreference(final Context context, final MySubtype subtype) {
            super(context);
            mSubtype = subtype;
        }

        /**
         * Get the subtype that this preference represents.
         * @return the subtype.
         */
        public MySubtype getSubtype() {
            return mSubtype;
        }
    }
}
