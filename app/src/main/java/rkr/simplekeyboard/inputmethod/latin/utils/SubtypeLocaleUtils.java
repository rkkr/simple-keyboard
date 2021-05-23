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

package rkr.simplekeyboard.inputmethod.latin.utils;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.Subtype;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;

public class SubtypeLocaleUtils {

    private SubtypeLocaleUtils() {
        // This utility class is not publicly instantiable.
    }

    private static final String LOCALE_ENGLISH_UNITED_STATES = "en_US";
    private static final String LOCALE_ENGLISH_GREAT_BRITAIN = "en_GB";
    private static final String LOCALE_AFRIKAANS = "af";
    private static final String LOCALE_ARABIC = "ar";
    private static final String LOCALE_AZERBAIJANI_AZERBAIJAN = "az_AZ";
    private static final String LOCALE_BELARUSIAN_BELARUS = "be_BY";
    private static final String LOCALE_BULGARIAN = "bg";
    private static final String LOCALE_BENGALI_BANGLADESH = "bn_BD";
    private static final String LOCALE_BENGALI_INDIA = "bn_IN";
    private static final String LOCALE_CATALAN = "ca";
    private static final String LOCALE_CZECH = "cs";
    private static final String LOCALE_DANISH = "da";
    private static final String LOCALE_GERMAN = "de";
    private static final String LOCALE_GERMAN_SWITZERLAND = "de_CH";
    private static final String LOCALE_GREEK = "el";
    private static final String LOCALE_ENGLISH_INDIA = "en_IN";
    private static final String LOCALE_ESPERANTO = "eo";
    private static final String LOCALE_SPANISH = "es";
    private static final String LOCALE_SPANISH_UNITED_STATES = "es_US";
    private static final String LOCALE_SPANISH_LATIN_AMERICA = "es_419";
    private static final String LOCALE_ESTONIAN_ESTONIA = "et_EE";
    private static final String LOCALE_BASQUE_SPAIN = "eu_ES";
    private static final String LOCALE_PERSIAN = "fa";
    private static final String LOCALE_FINNISH = "fi";
    private static final String LOCALE_FRENCH = "fr";
    private static final String LOCALE_FRENCH_CANADA = "fr_CA";
    private static final String LOCALE_FRENCH_SWITZERLAND = "fr_CH";
    private static final String LOCALE_GALICIAN_SPAIN = "gl_ES";
    private static final String LOCALE_HINDI = "hi";
    private static final String LOCALE_CROATIAN = "hr";
    private static final String LOCALE_HUNGARIAN = "hu";
    private static final String LOCALE_ARMENIAN_ARMENIA = "hy_AM";
    // Java uses the deprecated "in" code instead of the standard "id" code for Indonesian.
    private static final String LOCALE_INDONESIAN = "in";
    private static final String LOCALE_ICELANDIC = "is";
    private static final String LOCALE_ITALIAN = "it";
    private static final String LOCALE_ITALIAN_SWITZERLAND = "it_CH";
    // Java uses the deprecated "iw" code instead of the standard "he" code for Hebrew.
    private static final String LOCALE_HEBREW = "iw";
    private static final String LOCALE_GEORGIAN_GEORGIA = "ka_GE";
    private static final String LOCALE_KAZAKH = "kk";
    private static final String LOCALE_KHMER_CAMBODIA = "km_KH";
    private static final String LOCALE_KANNADA_INDIA = "kn_IN";
    private static final String LOCALE_KYRGYZ = "ky";
    private static final String LOCALE_LAO_LAOS = "lo_LA";
    private static final String LOCALE_LITHUANIAN = "lt";
    private static final String LOCALE_LATVIAN = "lv";
    private static final String LOCALE_MACEDONIAN = "mk";
    private static final String LOCALE_MALAYALAM_INDIA = "ml_IN";
    private static final String LOCALE_MONGOLIAN_MONGOLIA = "mn_MN";
    private static final String LOCALE_MARATHI_INDIA = "mr_IN";
    private static final String LOCALE_MALAY_MALAYSIA = "ms_MY";
    private static final String LOCALE_NORWEGIAN_BOKMAL = "nb"; // Norwegian Bokmål
    private static final String LOCALE_NEPALI_NEPAL = "ne_NP";
    private static final String LOCALE_DUTCH = "nl";
    private static final String LOCALE_DUTCH_BELGIUM = "nl_BE";
    private static final String LOCALE_POLISH = "pl";
    private static final String LOCALE_PORTUGUESE_BRAZIL = "pt_BR";
    private static final String LOCALE_PORTUGUESE_PORTUGAL = "pt_PT";
    private static final String LOCALE_ROMANIAN = "ro";
    private static final String LOCALE_RUSSIAN = "ru";
    private static final String LOCALE_SLOVAK = "sk";
    private static final String LOCALE_SLOVENIAN = "sl";
    private static final String LOCALE_SERBIAN = "sr";
    private static final String LOCALE_SERBIAN_LATIN = "sr_ZZ";
    private static final String LOCALE_SWEDISH = "sv";
    private static final String LOCALE_SWAHILI = "sw";
    private static final String LOCALE_TAMIL_INDIA = "ta_IN";
    private static final String LOCALE_TAMIL_SINGAPORE = "ta_SG";
    private static final String LOCALE_TELUGU_INDIA = "te_IN";
    private static final String LOCALE_THAI = "th";
    private static final String LOCALE_TAGALOG = "tl";
    private static final String LOCALE_TURKISH = "tr";
    private static final String LOCALE_UKRAINIAN = "uk";
    private static final String LOCALE_URDU = "ur";
    private static final String LOCALE_UZBEK_UZBEKISTAN = "uz_UZ";
    private static final String LOCALE_VIETNAMESE = "vi";
    private static final String LOCALE_ZULU = "zu";

    private static final String[] sSupportedLocales = new String[] {
            LOCALE_ENGLISH_UNITED_STATES,
            LOCALE_ENGLISH_GREAT_BRITAIN,
            LOCALE_AFRIKAANS,
            LOCALE_ARABIC,
            LOCALE_AZERBAIJANI_AZERBAIJAN,
            LOCALE_BELARUSIAN_BELARUS,
            LOCALE_BULGARIAN,
            LOCALE_BENGALI_BANGLADESH,
            LOCALE_BENGALI_INDIA,
            LOCALE_CATALAN,
            LOCALE_CZECH,
            LOCALE_DANISH,
            LOCALE_GERMAN,
            LOCALE_GERMAN_SWITZERLAND,
            LOCALE_GREEK,
            LOCALE_ENGLISH_INDIA,
            LOCALE_ESPERANTO,
            LOCALE_SPANISH,
            LOCALE_SPANISH_UNITED_STATES,
            LOCALE_SPANISH_LATIN_AMERICA,
            LOCALE_ESTONIAN_ESTONIA,
            LOCALE_BASQUE_SPAIN,
            LOCALE_PERSIAN,
            LOCALE_FINNISH,
            LOCALE_FRENCH,
            LOCALE_FRENCH_CANADA,
            LOCALE_FRENCH_SWITZERLAND,
            LOCALE_GALICIAN_SPAIN,
            LOCALE_HINDI,
            LOCALE_CROATIAN,
            LOCALE_HUNGARIAN,
            LOCALE_ARMENIAN_ARMENIA,
            LOCALE_INDONESIAN,
            LOCALE_ICELANDIC,
            LOCALE_ITALIAN,
            LOCALE_ITALIAN_SWITZERLAND,
            LOCALE_HEBREW,
            LOCALE_GEORGIAN_GEORGIA,
            LOCALE_KAZAKH,
            LOCALE_KHMER_CAMBODIA,
            LOCALE_KANNADA_INDIA,
            LOCALE_KYRGYZ,
            LOCALE_LAO_LAOS,
            LOCALE_LITHUANIAN,
            LOCALE_LATVIAN,
            LOCALE_MACEDONIAN,
            LOCALE_MALAYALAM_INDIA,
            LOCALE_MONGOLIAN_MONGOLIA,
            LOCALE_MARATHI_INDIA,
            LOCALE_MALAY_MALAYSIA,
            LOCALE_NORWEGIAN_BOKMAL,
            LOCALE_NEPALI_NEPAL,
            LOCALE_DUTCH,
            LOCALE_DUTCH_BELGIUM,
            LOCALE_POLISH,
            LOCALE_PORTUGUESE_BRAZIL,
            LOCALE_PORTUGUESE_PORTUGAL,
            LOCALE_ROMANIAN,
            LOCALE_RUSSIAN,
            LOCALE_SLOVAK,
            LOCALE_SLOVENIAN,
            LOCALE_SERBIAN,
            LOCALE_SERBIAN_LATIN,
            LOCALE_SWEDISH,
            LOCALE_SWAHILI,
            LOCALE_TAMIL_INDIA,
            LOCALE_TAMIL_SINGAPORE,
            LOCALE_TELUGU_INDIA,
            LOCALE_THAI,
            LOCALE_TAGALOG,
            LOCALE_TURKISH,
            LOCALE_UKRAINIAN,
            LOCALE_URDU,
            LOCALE_UZBEK_UZBEKISTAN,
            LOCALE_VIETNAMESE,
            LOCALE_ZULU
    };

    public static List<String> getSupportedLocales() {
        return Arrays.asList(sSupportedLocales);
    }

    private static final String LAYOUT_QWERTY = "qwerty";
    private static final String LAYOUT_AZERTY = "azerty";
    private static final String LAYOUT_QWERTZ = "qwertz";
    private static final String LAYOUT_ARABIC = "arabic";
    private static final String LAYOUT_ARMENIAN_PHONETIC = "armenian_phonetic";
    private static final String LAYOUT_BENGALI = "bengali";
    private static final String LAYOUT_BENGALI_AKKHOR = "bengali_akkhor";
    private static final String LAYOUT_BULGARIAN = "bulgarian";
    private static final String LAYOUT_BULGARIAN_BDS = "bulgarian_bds";
    private static final String LAYOUT_EAST_SLAVIC = "east_slavic";
    private static final String LAYOUT_FARSI = "farsi";
    private static final String LAYOUT_GEORGIAN = "georgian";
    private static final String LAYOUT_GREEK = "greek";
    private static final String LAYOUT_HEBREW = "hebrew";
    private static final String LAYOUT_HINDI = "hindi";
    private static final String LAYOUT_HINDI_COMPACT = "hindi_compact";
    private static final String LAYOUT_KANNADA = "kannada";
    private static final String LAYOUT_KHMER = "khmer";
    private static final String LAYOUT_LAO = "lao";
    private static final String LAYOUT_MACEDONIAN = "macedonian";
    private static final String LAYOUT_MALAYALAM = "malayalam";
    private static final String LAYOUT_MARATHI = "marathi";
    private static final String LAYOUT_MONGOLIAN = "mongolian";
    private static final String LAYOUT_NEPALI_ROMANIZED = "nepali_romanized";
    private static final String LAYOUT_NEPALI_TRADITIONAL = "nepali_traditional";
    private static final String LAYOUT_NORDIC = "nordic";
    private static final String LAYOUT_SERBIAN = "serbian";
    private static final String LAYOUT_SERBIAN_QWERTZ = "serbian_qwertz";
    private static final String LAYOUT_SPANISH = "spanish";
    private static final String LAYOUT_SWISS = "swiss";
    private static final String LAYOUT_TAMIL = "tamil";
    private static final String LAYOUT_TELUGU = "telugu";
    private static final String LAYOUT_THAI = "thai";
    private static final String LAYOUT_TURKISH_F = "turkish_f";
    private static final String LAYOUT_URDU = "urdu";
    private static final String LAYOUT_UZBEK = "uzbek";

    public static List<Subtype> getSubtypes(final String locale, final Resources resources) {
        return new SubtypeBuilder(locale, true, resources).getSubtypes();
    }

    public static Subtype getDefaultSubtype(final String locale, final Resources resources) {
        List<Subtype> subtypes = new SubtypeBuilder(locale, true, resources).getSubtypes();
        return subtypes.size() == 0 ? null : subtypes.get(0);
    }

    public static Subtype getSubtype(final String locale, final String layoutSet,
                                     final Resources resources) {
        List<Subtype> subtypes = new SubtypeBuilder(locale, layoutSet, resources).getSubtypes();
        return subtypes.size() == 0 ? null : subtypes.get(0);
    }

    public static List<Subtype> getDefaultSubtypes(final Resources resources) {
        final ArrayList<Locale> supportedLocales = new ArrayList<>(sSupportedLocales.length);
        for (final String localeString : sSupportedLocales) {
            supportedLocales.add(LocaleUtils.constructLocaleFromString(localeString));
        }
        final List<Locale> systemLocales = LocaleUtils.getSystemLocales();
        final ArrayList<Subtype> subtypes = new ArrayList<>();
        final HashSet<Locale> addedLocales = new HashSet<>();
        for (final Locale systemLocale : systemLocales) {
            final Locale bestLocale = LocaleUtils.findBestLocale(systemLocale, supportedLocales);
            if (bestLocale != null && !addedLocales.contains(bestLocale)) {
                addedLocales.add(bestLocale);
                final String bestLocaleString = LocaleUtils.getLocaleString(bestLocale);
                subtypes.add(getDefaultSubtype(bestLocaleString, resources));
            }
        }
        if (subtypes.size() == 0) {
            subtypes.add(getSubtypes(LOCALE_ENGLISH_UNITED_STATES, resources).get(0));
        }
        return subtypes;
    }

    private static class SubtypeBuilder {
        private final Resources mResources;
        private final boolean mAllowMultiple;
        private final String mLocale;
        private final String mExpectedLayoutSet;
        private List<Subtype> mSubtypes;

        public SubtypeBuilder(final String locale, final String expectedLayoutSet,
                              final Resources resources) {
            mLocale = locale;
            mExpectedLayoutSet = expectedLayoutSet;
            mAllowMultiple = false;
            mResources = resources;
        }
        public SubtypeBuilder(final String locale, final boolean allowMultiple,
                              final Resources resources) {
            mLocale = locale;
            mExpectedLayoutSet = null;
            mAllowMultiple = allowMultiple;
            mResources = resources;
        }

        public List<Subtype> getSubtypes() {
            if (mSubtypes != null) {
                return mSubtypes;
            }
            mSubtypes = new ArrayList<>();
            switch (mLocale) {
                case LOCALE_AFRIKAANS:
                case LOCALE_AZERBAIJANI_AZERBAIJAN:
                case LOCALE_ENGLISH_INDIA:
                case LOCALE_ENGLISH_GREAT_BRITAIN:
                case LOCALE_ENGLISH_UNITED_STATES:
                case LOCALE_FRENCH_CANADA:
                case LOCALE_INDONESIAN:
                case LOCALE_ICELANDIC:
                case LOCALE_ITALIAN:
                case LOCALE_LITHUANIAN:
                case LOCALE_LATVIAN:
                case LOCALE_MALAY_MALAYSIA:
                case LOCALE_DUTCH:
                case LOCALE_POLISH:
                case LOCALE_PORTUGUESE_BRAZIL:
                case LOCALE_PORTUGUESE_PORTUGAL:
                case LOCALE_ROMANIAN:
                case LOCALE_SLOVAK:
                case LOCALE_SWAHILI:
                case LOCALE_VIETNAMESE:
                case LOCALE_ZULU:
                    addLayout(LAYOUT_QWERTY);
                    addGenericLayouts();
                    break;
                case LOCALE_CZECH:
                case LOCALE_GERMAN:
                case LOCALE_CROATIAN:
                case LOCALE_HUNGARIAN:
                case LOCALE_SLOVENIAN:
                    addLayout(LAYOUT_QWERTZ);
                    addGenericLayouts();
                    break;
                case LOCALE_FRENCH:
                case LOCALE_DUTCH_BELGIUM:
                    addLayout(LAYOUT_AZERTY);
                    addGenericLayouts();
                    break;
                case LOCALE_CATALAN:
                case LOCALE_SPANISH:
                case LOCALE_SPANISH_UNITED_STATES:
                case LOCALE_SPANISH_LATIN_AMERICA:
                case LOCALE_BASQUE_SPAIN:
                case LOCALE_GALICIAN_SPAIN:
                case LOCALE_TAGALOG:
                    addLayout(LAYOUT_SPANISH);
                    addGenericLayouts();
                    break;
                case LOCALE_ESPERANTO:
                    addLayout(LAYOUT_SPANISH);
                    break;
                case LOCALE_DANISH:
                case LOCALE_ESTONIAN_ESTONIA:
                case LOCALE_FINNISH:
                case LOCALE_NORWEGIAN_BOKMAL:
                case LOCALE_SWEDISH:
                    addLayout(LAYOUT_NORDIC);
                    addGenericLayouts();
                    break;
                case LOCALE_GERMAN_SWITZERLAND:
                case LOCALE_FRENCH_SWITZERLAND:
                case LOCALE_ITALIAN_SWITZERLAND:
                    addLayout(LAYOUT_SWISS);
                    addGenericLayouts();
                    break;
                case LOCALE_TURKISH:
                    addLayout(LAYOUT_QWERTY);
                    addLayout(LAYOUT_TURKISH_F, R.string.subtype_f);
                    addGenericLayouts();
                    break;
                case LOCALE_UZBEK_UZBEKISTAN:
                    addLayout(LAYOUT_UZBEK);
                    addGenericLayouts();
                    break;
                case LOCALE_ARABIC:
                    addLayout(LAYOUT_ARABIC);
                    break;
                case LOCALE_BELARUSIAN_BELARUS:
                case LOCALE_KAZAKH:
                case LOCALE_KYRGYZ:
                case LOCALE_RUSSIAN:
                case LOCALE_UKRAINIAN:
                    addLayout(LAYOUT_EAST_SLAVIC);
                    break;
                case LOCALE_BULGARIAN:
                    addLayout(LAYOUT_BULGARIAN);
                    addLayout(LAYOUT_BULGARIAN_BDS, R.string.subtype_bds);
                    break;
                case LOCALE_BENGALI_BANGLADESH:
                    addLayout(LAYOUT_BENGALI_AKKHOR);
                    break;
                case LOCALE_BENGALI_INDIA:
                    addLayout(LAYOUT_BENGALI);
                    break;
                case LOCALE_GREEK:
                    addLayout(LAYOUT_GREEK);
                    break;
                case LOCALE_PERSIAN:
                    addLayout(LAYOUT_FARSI);
                    break;
                case LOCALE_HINDI:
                    addLayout(LAYOUT_HINDI);
                    addLayout(LAYOUT_HINDI_COMPACT, R.string.subtype_compact);
                    break;
                case LOCALE_ARMENIAN_ARMENIA:
                    addLayout(LAYOUT_ARMENIAN_PHONETIC);
                    break;
                case LOCALE_HEBREW:
                    addLayout(LAYOUT_HEBREW);
                    break;
                case LOCALE_GEORGIAN_GEORGIA:
                    addLayout(LAYOUT_GEORGIAN);
                    break;
                case LOCALE_KHMER_CAMBODIA:
                    addLayout(LAYOUT_KHMER);
                    break;
                case LOCALE_KANNADA_INDIA:
                    addLayout(LAYOUT_KANNADA);
                    break;
                case LOCALE_LAO_LAOS:
                    addLayout(LAYOUT_LAO);
                    break;
                case LOCALE_MACEDONIAN:
                    addLayout(LAYOUT_MACEDONIAN);
                    break;
                case LOCALE_MALAYALAM_INDIA:
                    addLayout(LAYOUT_MALAYALAM);
                    break;
                case LOCALE_MONGOLIAN_MONGOLIA:
                    addLayout(LAYOUT_MONGOLIAN);
                    break;
                case LOCALE_MARATHI_INDIA:
                    addLayout(LAYOUT_MARATHI);
                    break;
                case LOCALE_NEPALI_NEPAL:
                    addLayout(LAYOUT_NEPALI_ROMANIZED);
                    addLayout(LAYOUT_NEPALI_TRADITIONAL, R.string.subtype_traditional);
                    break;
                case LOCALE_SERBIAN:
                    addLayout(LAYOUT_SERBIAN);
                    break;
                case LOCALE_SERBIAN_LATIN:
                    addLayout(LAYOUT_SERBIAN_QWERTZ);
                    addGenericLayouts();
                    break;
                case LOCALE_TAMIL_INDIA:
                case LOCALE_TAMIL_SINGAPORE:
                    addLayout(LAYOUT_TAMIL);
                    break;
                case LOCALE_TELUGU_INDIA:
                    addLayout(LAYOUT_TELUGU);
                    break;
                case LOCALE_THAI:
                    addLayout(LAYOUT_THAI);
                    break;
                case LOCALE_URDU:
                    addLayout(LAYOUT_URDU);
                    break;
            }
            return mSubtypes;
        }

        private boolean shouldSkipLayout(final String keyboardLayoutSet) {
            if (mAllowMultiple) {
                return false;
            }
            if (mSubtypes.size() > 0) {
                return true;
            }
            if (mExpectedLayoutSet != null) {
                return !mExpectedLayoutSet.equals(keyboardLayoutSet);
            }
            return false;
        }

        private void addLayout(final String keyboardLayoutSet) {
            if (shouldSkipLayout(keyboardLayoutSet)) {
                return;
            }
            final String[] predefinedLayouts =
                    mResources.getStringArray(R.array.predefined_layouts);
            final int predefinedLayoutIndex =
                    Arrays.asList(predefinedLayouts).indexOf(keyboardLayoutSet);
            final String layoutNameStr;
            if (predefinedLayoutIndex >= 0) {
                final String[] predefinedLayoutDisplayNames = mResources.getStringArray(
                        R.array.predefined_layout_display_names);
                layoutNameStr = predefinedLayoutDisplayNames[predefinedLayoutIndex];
            } else {
                layoutNameStr = null;
            }
            mSubtypes.add(
                    new Subtype(mLocale, keyboardLayoutSet, layoutNameStr, false, mResources));
        }

        private void addLayout(final String keyboardLayoutSet, final int layoutRes) {
            if (shouldSkipLayout(keyboardLayoutSet)) {
                return;
            }
            mSubtypes.add(
                    new Subtype(mLocale, keyboardLayoutSet, layoutRes, true, mResources));
        }

        private void addGenericLayouts() {
            if (mSubtypes.size() > 0 && !mAllowMultiple) {
                return;
            }
            final int initialSize = mSubtypes.size();
            final String[] predefinedKeyboardLayoutSets = mResources.getStringArray(
                    R.array.predefined_layouts);
            final String[] predefinedKeyboardLayoutSetDisplayNames = mResources.getStringArray(
                    R.array.predefined_layout_display_names);
            for (int i = 0; i < predefinedKeyboardLayoutSets.length; i++) {
                final String predefinedLayout = predefinedKeyboardLayoutSets[i];
                if (shouldSkipLayout(predefinedLayout)) {
                    continue;
                }
                boolean alreadyExists = false;
                for (int j = 0; j < initialSize; j++) {
                    if (mSubtypes.get(j).getLayoutSet().equals(predefinedLayout)) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (alreadyExists) {
                    continue;
                }

                mSubtypes.add(new Subtype(mLocale, predefinedLayout,
                        predefinedKeyboardLayoutSetDisplayNames[i], true, mResources));
            }
        }
    }
}
