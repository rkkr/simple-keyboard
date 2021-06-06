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

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
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
    private CharSequence[] mUnusedLocaleNames;
    private String[] mUnusedLocaleValues;
    private ViewGroup mContainer;
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
        mContainer = container;
        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        buildContent();
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.add_language, menu);

        // make the icon match the color of the text in the action bar
        TextView textView = findActionBarTitleView();
        if (textView != null) {
            MenuItem menuItem = menu.findItem(R.id.action_add_language);
            setIconColor(menuItem, textView.getCurrentTextColor());
        }
    }

    /**
     * Try to look up the {@link TextView} from the activity's {@link ActionBar}.
     * @return the {@link TextView} or null if it wasn't found.
     */
    private TextView findActionBarTitleView() {
        ArrayList<View> views = new ArrayList<>();
        mView.getRootView().findViewsWithText(views, getActivity().getActionBar().getTitle(),
                View.FIND_VIEWS_WITH_TEXT);
        if (views.size() == 1 && views.get(0) instanceof TextView) {
            return (TextView)views.get(0);
        }
        return null;
    }

    /**
     * Set a menu item's icon to specific color.
     * @param menuItem the menu item that should change colors.
     * @param color the color that the icon should be changed to.
     */
    private void setIconColor(final MenuItem menuItem, final int color) {
        if (menuItem != null) {
            Drawable drawable = menuItem.getIcon();
            if (drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_add_language) {
            showAddLanguagePopup();
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
     * Show the popup to add a new language.
     */
    private void showAddLanguagePopup() {
        final boolean[] checkedItems = new boolean[mUnusedLocaleNames.length];
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_language)
                .setMultiChoiceItems(mUnusedLocaleNames, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialogInterface, final int which,
                                        final boolean isChecked) {
                        // make sure the add button is only enabled when at least one language is
                        // checked
                        if (isChecked) {
                            mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            return;
                        }
                        for (final boolean itemChecked : checkedItems) {
                            if (itemChecked) {
                                // button should already be enabled - nothing to do
                                return;
                            }
                        }
                        mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    }
                })
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        final ArrayList<String> selectedLocales = new ArrayList<>();
                        // enable the default layout for all of the checked languages
                        for (int i = 0; i < checkedItems.length; i++) {
                            if (checkedItems[i]) {
                                selectedLocales.add(mUnusedLocaleValues[i]);
                                final Subtype subtype = SubtypeLocaleUtils.getDefaultSubtype(
                                        mUnusedLocaleValues[i],
                                        LanguagesSettingsFragment.this.getResources());
                                mRichImm.addSubtype(subtype);
                            }
                        }

                        if (selectedLocales.size() == 1) {
                            openSingleLanguageSettings(selectedLocales.get(0));
                        } else {
                            // refresh the list of enabled languages
                            buildContent();
                        }
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

    /**
     * Open a language specific settings screen.
     * @param locale the locale for the setting screen to open.
     */
    private void openSingleLanguageSettings(final String locale) {
        final FragmentManager fragmentManager = getFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        final Fragment fragment = new SingleLanguageSettingsFragment();
        final Bundle extras = new Bundle();
        extras.putString(LOCALE_BUNDLE_KEY, locale);
        fragment.setArguments(extras);
        transaction.replace(mContainer.getId(), fragment);
        transaction.addToBackStack(null);
        transaction.commit();
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
