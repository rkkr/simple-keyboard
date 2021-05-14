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
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.MySubtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.InputMethodSubtypeUtils;
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
            loadDefaultSubtypes(locale);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        final HashSet<MySubtype> enabledSubtypes = mRichImm.getEnabledSubtypesOfThisIme();
        for (final SubtypePreference pref : mSubtypePrefs) {
            pref.setChecked(enabledSubtypes.contains(pref.getSubtype()));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * Get the default subtypes for a locale.
     * @param locale the locale for the subtypes to include.
     * @return the default subtypes for the specified locale.
     */
    private List<InputMethodSubtype> loadDefaultSubtypes(final String locale) {

        List<InputMethodSubtype> defaultSubtypes = mRichImm.getDefaultSubtypesOfThisIme();

        List<MySubtype> defaultSubtypes2 = new ArrayList<>();
        for (final String supportedLocale : SubtypeUtils.sSupportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            defaultSubtypes2.add(subtypes.get(0));
            if (supportedLocale.equals("bg") || supportedLocale.equals("hi") || supportedLocale.equals("ne_NP")) {
                defaultSubtypes2.add(subtypes.get(1));
            }
        }
//        compareSubtypes(defaultSubtypes, defaultSubtypes2);
        printSubtypes(defaultSubtypes2, "default");

        List<InputMethodSubtype> allXmlSubtypes = new ArrayList<>();
        final String[] predefinedKeyboardLayoutSets = getActivity().getResources().getStringArray(
                R.array.predefined_layouts);
        for (final InputMethodSubtype subtype : defaultSubtypes) {
            allXmlSubtypes.add(subtype);
            if (subtype.isAsciiCapable()) {
                for (final String predefinedLayout : predefinedKeyboardLayoutSets) {
                    if (SubtypeLocaleUtils.getKeyboardLayoutSetName(subtype).equals(predefinedLayout)) {
                        continue;
                    }
                    allXmlSubtypes.add(InputMethodSubtypeUtils.createAdditionalSubtype(subtype.getLocale(), predefinedLayout));
                }
            }
        }

        List<MySubtype> allJavaSubtypes = new ArrayList<>();
        for (final String supportedLocale : SubtypeUtils.sSupportedLocales) {
            List<MySubtype> subtypes = SubtypeUtils.getSubtypes(supportedLocale, getActivity().getResources());
            allJavaSubtypes.addAll(subtypes);
        }

//        compareSubtypes(allXmlSubtypes, allJavaSubtypes);
        printSubtypes(allJavaSubtypes, "all");

        List<InputMethodSubtype> localeSubtypes = new ArrayList<>();
        for (final InputMethodSubtype subtype : defaultSubtypes) {
            if (!locale.equals(subtype.getLocale())) {
                continue;
            }
            localeSubtypes.add(subtype);
        }

        return localeSubtypes;
    }

    private void compareSubtypes(List<InputMethodSubtype> xmlSubtypes, List<MySubtype> javaSubtypes) {
        final String sizeMessage = "defaultSubtypesXml: " + xmlSubtypes.size() + ", defaultSubtypesJava: " + javaSubtypes.size();
        if (xmlSubtypes.size() == javaSubtypes.size()) {
            Log.w(TAG, sizeMessage);
        } else {
            Log.e(TAG, sizeMessage);
        }
        for (int i = 0; i < Math.min(xmlSubtypes.size(), javaSubtypes.size()); i++) {
            final InputMethodSubtype subtypeXml = xmlSubtypes.get(i);
            final MySubtype subtypeJava = javaSubtypes.get(i);

            final String nameXml = SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtypeXml);
            final String nameJava = subtypeJava.getName();
            final boolean labelSame = nameXml.equals(nameJava);
            final String labelMessage = "label: equal=" + labelSame
                    + (labelSame ? " " + nameXml : " xml=" + nameXml + ", java=" + nameJava);


            final boolean localeSame = subtypeXml.getLocale().equals(subtypeJava.getLocale());
            final String localeMessage = "locale: equal=" + localeSame
                    + (localeSame ? " " + subtypeXml.getLocale() : " xml=" + subtypeXml.getLocale() + ", java=" + subtypeJava.getLocale());


            final String layoutXml = SubtypeLocaleUtils.getKeyboardLayoutSetName(subtypeXml);
            final String layoutJava = subtypeJava.getLayoutSet();
            final boolean layoutSame = layoutXml.equals(layoutJava);
            final String layoutMessage = "layout: equal=" + layoutSame
                    + (layoutSame ? " " + layoutXml : " xml=" + layoutXml + ", java=" + layoutJava);


            final String layoutNameXml = SubtypeLocaleUtils.getKeyboardLayoutDisplayName(subtypeXml, getActivity());
            final String layoutNameJava = subtypeJava.getLayoutDisplayName();
            final boolean layoutNameSame = layoutNameXml.equals(layoutNameJava);
            final String layoutNameMessage = "layout name: equal=" + layoutNameSame
                    + (layoutNameSame ? " " + layoutNameXml : " xml=" + layoutNameXml + ", java=" + layoutNameJava);


            if (labelSame && localeSame && layoutSame && layoutNameSame) {
                Log.w(TAG, labelMessage);
                Log.w(TAG, localeMessage);
                Log.w(TAG, layoutMessage);
                Log.w(TAG, layoutNameMessage);
            } else {
                Log.e(TAG, labelMessage);
                Log.e(TAG, localeMessage);
                Log.e(TAG, layoutMessage);
                Log.e(TAG, layoutNameMessage);
            }
        }
    }

    private void printSubtypes(List<MySubtype> javaSubtypes, String prefix) {
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

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                final SubtypePreference pref = (SubtypePreference) preference;
                if (pref.isChecked()) {
                    return mRichImm.addSubtype(pref.getSubtype());
                } else {
                    return mRichImm.removeSubtype(pref.getSubtype());
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
