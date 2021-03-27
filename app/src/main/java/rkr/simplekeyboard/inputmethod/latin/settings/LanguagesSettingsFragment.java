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
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
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
        inflater.inflate(R.menu.add_language, menu);

        // make the icon match the color of the text in the action bar
        TextView textView = findActionBarTitleView();
        if (textView != null) {
            setIconColor(menu, textView.getCurrentTextColor());
        }
    }

    private TextView findActionBarTitleView() {
        ArrayList<View> views = new ArrayList<>();
        mView.getRootView().findViewsWithText(views, getActivity().getActionBar().getTitle(),
                View.FIND_VIEWS_WITH_TEXT);
        if (views.size() == 1 && views.get(0) instanceof TextView) {
            return (TextView)views.get(0);
        }
        return null;
    }

    private void setIconColor(final Menu menu, final int color) {
        MenuItem menuItem = menu.findItem(R.id.action_add_language);
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

    private void setContent(final Context context) {
        final PreferenceGroup group = getPreferenceScreen();
        group.setTitle(R.string.select_language);
        group.removeAll();

        final Preference subtypeEnablerPreference = createSubtypeSettingLinkPreference(context);
        subtypeEnablerPreference.setTitle(R.string.enable_subtypes);
        subtypeEnablerPreference.setSummary(R.string.enable_subtypes_details);
        group.addPreference(subtypeEnablerPreference);

        final PreferenceCategory languageCategory = new PreferenceCategory(context);
        languageCategory.setTitle(R.string.user_languages);
        group.addPreference(languageCategory);

        setLanguageInfo(group, context);
    }

    private void setLanguageInfo(final PreferenceGroup group, final Context context) {
        final InputMethodInfo imi = mRichImm.getInputMethodInfoOfThisIme();
        final HashSet<InputMethodSubtype> enabledSubtypes =
                new HashSet<>(mRichImm.getMyEnabledInputMethodSubtypeList(true));

        final Locale currentLocale = getResources().getConfiguration().locale;
        final Comparator<Locale> comparator = new LocaleComparator(currentLocale);

        final HashSet<InputMethodSubtype> prefSubtypes =
                new HashSet<>(Arrays.asList(mRichImm.getAdditionalSubtypes()));

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
                Log.d(TAG, String.format("Enabled subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            locales.add(LocaleUtils.constructLocaleFromString(subtype.getLocale()));
        }

        for (final InputMethodSubtype subtype: prefSubtypes) {
            if (DEBUG_SUBTYPE_ID && !subtypes.contains(subtype)) {
                Log.d(TAG, String.format("Not enabled additional subtype: %-6s 0x%08x %11d %s",
                        subtype.getLocale(), subtype.hashCode(), subtype.hashCode(),
                        SubtypeLocaleUtils.getSubtypeDisplayNameInSystemLocale(subtype)));
            }
            locales.add(LocaleUtils.constructLocaleFromString(subtype.getLocale()));
        }

        return locales;
    }

    private TreeSet<Locale> getUnusedLocales(final InputMethodInfo imi,
                                             final TreeSet<Locale> usedLocales,
                                             final Comparator<Locale> comparator) {
        final TreeSet<Locale> locales = new TreeSet<>(comparator);
        final int count = imi.getSubtypeCount();
        for (int i = 0; i < count; i++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(i);
            final Locale locale = LocaleUtils.constructLocaleFromString(subtype.getLocale());
            if (usedLocales.contains(locale)) {
                continue;
            }
            if (subtype.isAsciiCapable()) {
                locales.add(locale);
            }
        }
        return locales;
    }

    private void createLanguagePreferences(final TreeSet<Locale> locales,
                                           final HashSet<InputMethodSubtype> prefSubtypes,
                                           final HashSet<InputMethodSubtype> enabledSubtypes,
                                           final PreferenceGroup group, final Context context) {
        for (Locale locale : locales) {
            final String localeString = LocaleUtils.getLocaleString(locale);
            final SingleLanguagePreference pref =
                    new SingleLanguagePreference(context, localeString);
            if (subtypesNeedEnabling(localeString, prefSubtypes, enabledSubtypes)) {
                pref.setSummary(R.string.subtypes_need_enabling);
            }
            group.addPreference(pref);
        }
    }

    private boolean subtypesNeedEnabling(final String locale,
                                         final HashSet<InputMethodSubtype> prefSubtypes,
                                         final HashSet<InputMethodSubtype> enabledSubtypes) {
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
                    LocaleUtils.getLocaleString(a));
            final String bDisplay = SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(
                    LocaleUtils.getLocaleString(b));
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
