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
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.IntentUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

import static rkr.simplekeyboard.inputmethod.latin.settings.SingleLanguageSettingsFragment.LOCALE_BUNDLE_KEY;

/**
 * "Languages" settings sub screen.
 */
public final class LanguagesSettingsFragment extends SubScreenFragment{
    private static final String TAG = LanguagesSettingsFragment.class.getSimpleName();

    private static final boolean DEBUG_SUBTYPE_ID = false;

    private RichInputMethodManager mRichImm;
    private CharSequence[] mEntries;
    private String[] mEntryValues;
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
    public void onResume() {
        super.onResume();
        final Context context = getActivity();
        buildContent(context);
    }

    @Override
    public void onPause() {
        super.onPause();
        // The enabled languages might change before coming back, so they will need to update. The
        // user can just press the add button again.
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
            mAlertDialog = null;
        }
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
            showLanguagePopup();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Build the preferences and them to this settings screen.
     * @param context the context for this application.
     */
    private void buildContent(final Context context) {
        final PreferenceGroup group = getPreferenceScreen();
        group.removeAll();
        group.setTitle(R.string.select_language);

        final Preference subtypeEnablerPreference = createSubtypeSettingLinkPreference(context);
        subtypeEnablerPreference.setTitle(R.string.enable_subtypes);
        subtypeEnablerPreference.setSummary(R.string.enable_subtypes_details);
        group.addPreference(subtypeEnablerPreference);

        final PreferenceCategory languageCategory = new PreferenceCategory(context);
        languageCategory.setTitle(R.string.user_languages);
        group.addPreference(languageCategory);

        setUpLanguages(group, context);
    }

    /**
     * Create the preference to open the system's subtype settings activity to enable or disable
     * subtypes for this IME.
     * @param context the context for this application.
     * @return the preference that was created.
     */
    private Preference createSubtypeSettingLinkPreference(final Context context) {
        final String imeId = mRichImm.getInputMethodIdOfThisIme();

        final Preference subtypeEnablerPreference = new Preference(context);
        subtypeEnablerPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final Intent intent =
                                IntentUtils.getInputLanguageSelectionIntent(imeId, context);
                        context.startActivity(intent);
                        return true;
                    }
                });
        return subtypeEnablerPreference;
    }

    /**
     * Add a preference for each of the used languages (enabled in the system or created additional
     * layouts that haven't been enabled in the system yet), and build the list of unused languages.
     * @param group the preference group to add preferences to.
     * @param context the context for this application.
     */
    private void setUpLanguages(final PreferenceGroup group, final Context context) {
        final HashSet<InputMethodSubtype> enabledSubtypes = mRichImm.getEnabledSubtypesOfThisIme();

        final Locale currentLocale = getResources().getConfiguration().locale;
        final Comparator<Locale> comparator = new LocaleComparator(currentLocale);

        final TreeSet<Locale> usedLocales = getUsedLocales(enabledSubtypes, comparator);
        final TreeSet<Locale> unusedLocales = getUnusedLocales(usedLocales, comparator);

        buildLanguagePreferences(usedLocales, group, context);
        setAdditionalLocaleEntries(unusedLocales);
    }

    /**
     * Get all of the languages that are use by the user. This is defined as the locale for any
     * default subtype that has been enabled in the system or any additional subtype that has been
     * selected (extra keyboard layout checked in the language specific setting screen), regardless
     * of whether it has been enabled in the system.
     * @param subtypes the list of subtypes for this IME that have been enabled.
     * @param comparator the comparator to sort the languages.
     * @return a tree set of locales for the used languages sorted using the specified comparator.
     */
    private TreeSet<Locale> getUsedLocales(final HashSet<InputMethodSubtype> subtypes,
                                           final Comparator<Locale> comparator) {
        TreeSet<Locale> locales = new TreeSet<>(comparator);

        for (final InputMethodSubtype subtype : subtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Enabled subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            locales.add(LocaleUtils.constructLocaleFromString(subtype.getLocale()));
        }

        return locales;
    }

    /**
     * Get the list of languages supported by this IME that aren't included in
     * {@link #getUsedLocales}.
     * @param usedLocales the used locales.
     * @param comparator the comparator to sort the languages.
     * @return a tree set of locales for the unused languages sorted using the specified comparator.
     */
    private TreeSet<Locale> getUnusedLocales(final TreeSet<Locale> usedLocales,
                                             final Comparator<Locale> comparator) {
        final TreeSet<Locale> locales = new TreeSet<>(comparator);
        for (String localeString : RichInputMethodManager.sSupportedLocales) {
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
    private void buildLanguagePreferences(final TreeSet<Locale> locales,
                                          final PreferenceGroup group, final Context context) {
        for (final Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            final SingleLanguagePreference pref =
                    new SingleLanguagePreference(context, localeString);
            group.addPreference(pref);
        }
    }

    /**
     * Check if any additional subtypes for a locale have been selected but not enabled in the
     * system.
     * @param locale the locale to check.
     * @param prefSubtypes the selected additional subtypes.
     * @param enabledSubtypes the enabled subtypes for this IME.
     * @return whether any selected additional subtypes for the locale still need to be enabled.
     */
    private boolean subtypesNeedEnabling(final String locale,
                                         final HashSet<InputMethodSubtype> prefSubtypes,
                                         final HashSet<InputMethodSubtype> enabledSubtypes) {
        for (final InputMethodSubtype subtype : prefSubtypes) {
            if (!locale.equals(subtype.getLocale())) {
                continue;
            }
            if (!enabledSubtypes.contains(subtype)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the list of unused languages that can be added.
     * @param locales the unused locales that are supported in this IME.
     */
    private void setAdditionalLocaleEntries(final TreeSet<Locale> locales) {
        mEntries = new CharSequence[locales.size()];
        mEntryValues = new String[locales.size()];
        int i = 0;
        for (Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            mEntryValues[i] = localeString;
            mEntries[i] =
                    SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }
    }

    /**
     * Show the popup to add a new language.
     */
    private void showLanguagePopup() {
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.add_language)
                .setItems(mEntries, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openSingleLanguageSettings(mEntryValues[which]);
                    }
                })
                .setNegativeButton(android.R.string.cancel,  new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        mAlertDialog.show();
    }

    /**
     * Open a language specific settings screen.
     * @param locale the locale for the setting screen to open.
     */
    private void openSingleLanguageSettings(String locale) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        Fragment fragment = new SingleLanguageSettingsFragment();
        Bundle extras = new Bundle();
        extras.putString(LOCALE_BUNDLE_KEY, locale);
        fragment.setArguments(extras);
        transaction.replace(mContainer.getId(), fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    /**
     * Comparator for {@link Locale} to order them alphabetically, but keeping the current language
     * first.
     */
    private static class LocaleComparator implements Comparator<Locale> {
        private final Locale mCurrentLocale;

        /**
         * Create a new LocaleComparator.
         * @param currentLocale the language and country to sort first.
         */
        public LocaleComparator(final Locale currentLocale) {
            mCurrentLocale = currentLocale;
        }

        @Override
        public int compare(Locale a, Locale b) {
            if (a.getLanguage().equals(b.getLanguage())) {
                if (a.getCountry().equals(b.getCountry())) {
                    return 0;
                }
                if (a.getLanguage().equals(mCurrentLocale.getLanguage())) {
                    if (a.getCountry().equals(mCurrentLocale.getCountry())) {
                        // current language and country should be at the top
                        return -1;
                    }
                    if (b.getCountry().equals(mCurrentLocale.getCountry())) {
                        // current language should be at above others
                        return 1;
                    }
                }
            } else {
                if (a.getLanguage().equals(mCurrentLocale.getLanguage())) {
                    // current language should be at above others
                    return -1;
                } else if (b.getLanguage().equals(mCurrentLocale.getLanguage())) {
                    // current language should be at above others
                    return 1;
                }
            }
            final String aDisplay = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(
                    LocaleUtils.getLocaleString(a));
            final String bDisplay = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(
                    LocaleUtils.getLocaleString(b));
            return aDisplay.compareToIgnoreCase(bDisplay);
        }
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

            setTitle(SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(localeString));
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
