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

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.MenuItemIconColorCompat;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

import static rkr.simplekeyboard.inputmethod.latin.settings.SingleLanguageSettingsFragment.LOCALE_BUNDLE_KEY;

/**
 * "Languages" settings sub screen.
 */
public final class LanguagesSettingsFragment extends PreferenceFragment {
    private static final String TAG = LanguagesSettingsFragment.class.getSimpleName();

    private static final boolean DEBUG_SUBTYPE_ID = false;

    private RichInputMethodManager mRichImm;
    private CharSequence[] mUsedLocaleNames;
    private String[] mUsedLocaleValues;
    private CharSequence[] mUnusedLocaleNames;
    private String[] mUnusedLocaleValues;
    private AlertDialog mAlertDialog;
    private View mView;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();

        addPreferencesFromResource(R.xml.empty_settings);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        mView = super.onCreateView(inflater, container, savedInstanceState);
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        buildContent();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.remove_language, menu);
        inflater.inflate(R.menu.add_language, menu);

        MenuItem addLanguageMenuItem = menu.findItem(R.id.action_add_language);
        MenuItemIconColorCompat.matchMenuIconColor(mView, addLanguageMenuItem,
                getActivity().getActionBar());
        MenuItem removeLanguageMenuItem = menu.findItem(R.id.action_remove_language);
        MenuItemIconColorCompat.matchMenuIconColor(mView, removeLanguageMenuItem,
                getActivity().getActionBar());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_add_language) {
            showAddLanguagePopup();
        } else if (itemId == R.id.action_remove_language) {
            showRemoveLanguagePopup();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Build the preferences and them to this settings screen.
     */
    private void buildContent() {
        final Context context = getActivity();
        final PreferenceGroup group = getPreferenceScreen();
        group.removeAll();

        final PreferenceCategory languageCategory = new PreferenceCategory(context);
        languageCategory.setTitle(R.string.user_languages);
        group.addPreference(languageCategory);

        final Comparator<Locale> comparator = new LocaleUtils.LocaleComparator();
        final Set<Subtype> enabledSubtypes = mRichImm.getEnabledSubtypes(false);
        final SortedSet<Locale> usedLocales = getUsedLocales(enabledSubtypes, comparator);
        final SortedSet<Locale> unusedLocales = getUnusedLocales(usedLocales, comparator);

        buildLanguagePreferences(usedLocales, group, context);
        setAdditionalLocaleEntries(unusedLocales);
        setExistingLocaleEntries(usedLocales);
    }

    /**
     * Get all of the unique languages from the subtypes that have been enabled.
     * @param subtypes the list of subtypes for this IME that have been enabled.
     * @param comparator the comparator to sort the languages.
     * @return a set of locales for the used languages sorted using the specified comparator.
     */
    private SortedSet<Locale> getUsedLocales(final Set<Subtype> subtypes,
                                             final Comparator<Locale> comparator) {
        final SortedSet<Locale> locales = new TreeSet<>(comparator);

        for (final Subtype subtype : subtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Enabled subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        subtype.getName()));
            }
            locales.add(subtype.getLocaleObject());
        }

        return locales;
    }

    /**
     * Get the list of languages supported by this IME that aren't included in
     * {@link #getUsedLocales}.
     * @param usedLocales the used locales.
     * @param comparator the comparator to sort the languages.
     * @return a set of locales for the unused languages sorted using the specified comparator.
     */
    private SortedSet<Locale> getUnusedLocales(final Set<Locale> usedLocales,
                                               final Comparator<Locale> comparator) {
        final SortedSet<Locale> locales = new TreeSet<>(comparator);
        for (String localeString : SubtypeLocaleUtils.getSupportedLocales()) {
            final Locale locale = LocaleUtils.constructLocaleFromString(localeString);
            if (usedLocales.contains(locale)) {
                continue;
            }
            locales.add(locale);
        }
        return locales;
    }

    /**
     * Create a language preference for each of the specified locales in the preference group. These
     * preferences will be added to the group in the order of the locales that are passed in.
     * @param locales the locales to add preferences for.
     * @param group the preference group to add preferences to.
     * @param context the context for this application.
     */
    private void buildLanguagePreferences(final SortedSet<Locale> locales,
                                          final PreferenceGroup group, final Context context) {
        for (final Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            final SingleLanguagePreference pref =
                    new SingleLanguagePreference(context, localeString);
            group.addPreference(pref);
        }
    }

    /**
     * Set the list of unused languages that can be added.
     * @param locales the unused locales that are supported in this IME.
     */
    private void setAdditionalLocaleEntries(final SortedSet<Locale> locales) {
        mUnusedLocaleNames = new CharSequence[locales.size()];
        mUnusedLocaleValues = new String[locales.size()];
        int i = 0;
        for (Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            mUnusedLocaleValues[i] = localeString;
            mUnusedLocaleNames[i] =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }
    }

    /**
     * Set the list of used languages that can be removed.
     * @param locales the enabled locales for this IME.
     */
    private void setExistingLocaleEntries(final SortedSet<Locale> locales) {
        mUsedLocaleNames = new CharSequence[locales.size()];
        mUsedLocaleValues = new String[locales.size()];
        int i = 0;
        for (Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            mUsedLocaleValues[i] = localeString;
            mUsedLocaleNames[i] =
                    LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }
    }

    /**
     * Show the popup to add a new language.
     */
    private void showAddLanguagePopup() {
        final boolean[] checkedItems = new boolean[mUnusedLocaleNames.length];
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_language)
                .setMultiChoiceItems(mUnusedLocaleNames, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface,
                                                final int which, final boolean isChecked) {
                                // make sure the add button is only enabled when at least one
                                // language is checked
                                if (isChecked) {
                                    mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                            .setEnabled(true);
                                    return;
                                }
                                for (final boolean itemChecked : checkedItems) {
                                    if (itemChecked) {
                                        // button should already be enabled - nothing to do
                                        return;
                                    }
                                }
                                mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                        .setEnabled(false);
                            }
                        })
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        // enable the default layout for all of the checked languages
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (!checkedItems[i]) {
                                continue;
                            }
                            final Subtype subtype = SubtypeLocaleUtils.getDefaultSubtype(
                                    mUnusedLocaleValues[i],
                                    LanguagesSettingsFragment.this.getResources());
                            mRichImm.addSubtype(subtype);
                        }

                        // refresh the list of enabled languages
                        buildContent();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                    }
                })
                .create();
        mAlertDialog.show();
        // disable the add button since nothing is checked by default
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void showRemoveLanguagePopup() {
        final boolean[] checkedItems = new boolean[mUsedLocaleNames.length];
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.remove_language)
                .setMultiChoiceItems(mUsedLocaleNames, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(final DialogInterface dialogInterface,
                                                final int which, final boolean isChecked) {
                                // make sure the remove button is only enabled when at least one
                                // language is checked (action will do something) and at least one
                                // language is unchecked (don't remove all languages)
                                boolean hasCheckedItem = false;
                                boolean hasUncheckedItem = false;
                                for (final boolean itemChecked : checkedItems) {
                                    if (itemChecked) {
                                        hasCheckedItem = true;
                                    } else {
                                        hasUncheckedItem = true;
                                    }
                                    if (hasCheckedItem && hasUncheckedItem) {
                                        break;
                                    }
                                }
                                mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                        .setEnabled(hasCheckedItem && hasUncheckedItem);
                            }
                        })
                .setPositiveButton(R.string.remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        // disable the layouts for all of the checked languages
                        for (int i = 0; i < checkedItems.length; i++) {
                            Log.w(TAG, mUsedLocaleNames[i] + ": " + checkedItems[i]);
                            if (!checkedItems[i]) {
                                continue;
                            }
                            final Set<Subtype> subtypes =
                                    mRichImm.getEnabledSubtypesForLocale(mUsedLocaleValues[i]);
                            for (final Subtype subtype : subtypes) {
                                mRichImm.removeSubtype(subtype);
                            }
                        }

                        // refresh the list of enabled languages
                        buildContent();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                    }
                })
                .create();
        mAlertDialog.show();
        // disable the remove button since nothing is checked by default
        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    /**
     * Preference to link to a language specific settings screen.
     */
    private static class SingleLanguagePreference extends Preference {
        private final String mLocale;
        private Bundle mExtras;

        /**
         * Create a new preference for a language.
         * @param context the context for this application.
         * @param localeString a string specification of a locale, in a format of "ll_cc_variant",
         *                     where "ll" is a language code, "cc" is a country code.
         */
        public SingleLanguagePreference(final Context context, final String localeString) {
            super(context);
            mLocale = localeString;

            setTitle(LocaleResourceUtils.getLocaleDisplayNameInSystemLocale(localeString));
            setFragment(SingleLanguageSettingsFragment.class.getName());
        }

        @Override
        public Bundle getExtras() {
            if (mExtras == null) {
                mExtras = new Bundle();
                mExtras.putString(LOCALE_BUNDLE_KEY, mLocale);
            }
            return mExtras;
        }

        @Override
        public Bundle peekExtras() {
            return mExtras;
        }
    }
}
