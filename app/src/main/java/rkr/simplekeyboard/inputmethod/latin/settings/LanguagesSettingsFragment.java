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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodManager;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

import static rkr.simplekeyboard.inputmethod.latin.settings.SingleLanguageSettingsFragment.LOCALE_BUNDLE_KEY;

public final class LanguagesSettingsFragment extends SubScreenFragment{
    private static final String TAG = LanguagesSettingsFragment.class.getSimpleName();
    // Note: We would like to turn this debug flag true in order to see what input styles are
    // defined in a bug-report.
    private static final boolean DEBUG_SUBTYPE_ID = false;

    private SharedPreferences mPrefs;
    private RichInputMethodManager mRichImm;
    private CharSequence[] mEntries;
    private String[] mEntryValues;
    private ViewGroup mContainer;
    private AlertDialog mAlertDialog;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(getActivity());
        RichInputMethodManager.init(getActivity());
        mRichImm = RichInputMethodManager.getInstance();

        addPreferencesFromResource(R.xml.additional_subtype_settings);//TODO: rename this file

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        mContainer = container;
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        final Context context = getActivity();
        setContent(context);
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
        inflater.inflate(R.menu.add_style, menu);//TODO: rename this file
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.action_add_style) {
            showLanguagePopup();
        }
        return super.onOptionsItemSelected(item);
    }

    private Preference createSubtypeSettingLinkPreference(final Context context) {
        final InputMethodInfo imi = mRichImm.getInputMethodInfoOfThisIme();

        final Preference subtypeEnablerPreference = new Preference(context);
        subtypeEnablerPreference
                .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        openSubtypeSettings(imi, context);
                        return true;
                    }
                });
        return subtypeEnablerPreference;
    }

    //TODO: move somewhere common
    public static void openSubtypeSettings(final InputMethodInfo imi, final Context context) {
        final CharSequence title = context.getString(R.string.select_language);
        final Intent intent =
                new Intent(android.provider.Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS);
        intent.putExtra(android.provider.Settings.EXTRA_INPUT_METHOD_ID, imi.getId());
        if (!TextUtils.isEmpty(title)) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private void setContent(final Context context) {
        final PreferenceGroup group = getPreferenceScreen();
        group.removeAll();

        final Preference subtypeEnablerPreference = createSubtypeSettingLinkPreference(context);
        subtypeEnablerPreference.setTitle("Enable languages and layouts");//TODO: tokenize
        subtypeEnablerPreference.setSummary("Keyboard languages and layouts need to be enabled in the system before they can be used");//TODO: tokenize
        group.addPreference(subtypeEnablerPreference);

        final PreferenceCategory languageCategory = new PreferenceCategory(context);
        languageCategory.setTitle("Your keyboard languages");//TODO: tokenize
        group.addPreference(languageCategory);

        setLanguageInfo(group, context);
    }

    private void setLanguageInfo(final PreferenceGroup group, final Context context) {
        final InputMethodInfo imi = mRichImm.getInputMethodInfoOfThisIme();
        final HashSet<InputMethodSubtype> enabledSubtypes =
                SingleLanguageSettingsFragment.loadEnabledSubtypes(mRichImm, context);

        final Locale currentLocale = getResources().getConfiguration().locale;
        final Comparator<Locale> comparator = new LocaleComparator(currentLocale);

        final HashSet<InputMethodSubtype> prefSubtypes =
                SingleLanguageSettingsFragment.loadPrefSubtypes(mPrefs, getResources());

        final TreeSet<Locale> usedLocales =
                getUsedLocales(enabledSubtypes, prefSubtypes, comparator);
        final TreeSet<Locale> unusedLocales = getUnusedLocales(imi, usedLocales, comparator);

        createLanguagePreferences(usedLocales, prefSubtypes, enabledSubtypes, group, context);
        setAdditionalLocaleEntries(unusedLocales);
    }

    private TreeSet<Locale> getUsedLocales(final HashSet<InputMethodSubtype> subtypes,
                                           final HashSet<InputMethodSubtype> prefSubtypes,
                                           final Comparator<Locale> comparator) {
        TreeSet<Locale> locales = new TreeSet<>(comparator);

        for (final InputMethodSubtype subtype: subtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Used default subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            locales.add(getLocale(subtype.getLocale()));
        }

        for (final InputMethodSubtype subtype: prefSubtypes) {
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Used additional subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            locales.add(getLocale(subtype.getLocale()));
        }

        return locales;
    }

    private TreeSet<Locale> getUnusedLocales(final InputMethodInfo imi,
                                             final Set<Locale> usedLocales,
                                             final Comparator<Locale> comparator) {
        final TreeSet<Locale> locales = new TreeSet<>(comparator);
        final int count = imi.getSubtypeCount();
        for (int i = 0; i < count; i++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(i);
            final Locale locale = getLocale(subtype.getLocale());
            if (usedLocales.contains(locale)) {
                continue;
            }
            if (DEBUG_SUBTYPE_ID) {
                Log.d(TAG, String.format("Unused subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            if (subtype.isAsciiCapable()) {
                locales.add(locale);
            }
        }
        return locales;
    }

    private void createLanguagePreferences(final Set<Locale> locales,
                                           final Set<InputMethodSubtype> prefSubtypes,
                                           final Set<InputMethodSubtype> enabledSubtypes,
                                           final PreferenceGroup group, final Context context) {
        for (Locale locale : locales) {
            final SingleLanguagePreference pref =
                    new SingleLanguagePreference(context, getLocaleString(locale));
            if (subtypesNeedEnabling(getLocaleString(locale), prefSubtypes, enabledSubtypes)) {
                pref.setSummary("Some layouts still need to be enabled");
            }
            group.addPreference(pref);
        }
    }

    private boolean subtypesNeedEnabling(final String locale,
                                         final Collection<InputMethodSubtype> prefSubtypes,
                                         final Collection<InputMethodSubtype> enabledSubtypes) {
        for (InputMethodSubtype subtype : prefSubtypes) {
            if (!locale.equals(subtype.getLocale())) {
                continue;
            }
            if (!enabledSubtypes.contains(subtype)) {
                return true;
            }
        }
        return false;
    }

    private void setAdditionalLocaleEntries(final Set<Locale> locales) {
        mEntries = new CharSequence[locales.size()];
        mEntryValues = new String[locales.size()];
        int i = 0;
        for (Locale locale : locales) {
            final String localeString = getLocaleString(locale);
            mEntryValues[i] = localeString;
            mEntries[i] =
                    SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(localeString);
            i++;
        }
    }

    //TODO: move somewhere common
    public static Locale getLocale(final String localeString) {
        final String[] localeParts = localeString.split("_");
        final Locale locale;
        if (localeParts.length < 2) {
            locale = new Locale(localeParts[0]);
        } else {
            locale = new Locale(localeParts[0], localeParts[1]);
        }
        return locale;
    }

    //TODO: move somewhere common
    public static String getLocaleString(final Locale locale) {
        if (TextUtils.isEmpty(locale.getCountry())) {
            return locale.getLanguage();
        }
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    private void showLanguagePopup() {
        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Choose a language")//TODO: tokenize
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

    private static class LocaleComparator implements Comparator<Locale> {
        private final Locale mCurrentLocale;

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
                    getLocaleString(a));
            final String bDisplay = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(
                    getLocaleString(b));
            return aDisplay.compareToIgnoreCase(bDisplay);
        }
    }

    private static class SingleLanguagePreference extends Preference {
        private final String mLocale;
        private Bundle mExtras;

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
