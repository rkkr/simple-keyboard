package rkr.simplekeyboard.inputmethod.latin.utils;

import android.content.res.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.MySubtype;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;

public class SubtypeUtils {
    public static Resources mResources;

    private SubtypeUtils() {
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
    //hi_ZZ: Hinglish/qwerty  # This is a preliminary keyboard layout.
    private static final String LOCALE_CROATIAN = "hr";
    private static final String LOCALE_HUNGARIAN = "hu";
    private static final String LOCALE_ARMENIAN_ARMENIA = "hy_AM";
    private static final String LOCALE_INDONESIAN = "in"; // "id" is the official language code of Indonesian. // Java uses the deprecated "in" code instead of the standard "id" code for Indonesian.
    private static final String LOCALE_ICELANDIC = "is";
    private static final String LOCALE_ITALIAN = "it";
    private static final String LOCALE_ITALIAN_SWITZERLAND = "it_CH";
    private static final String LOCALE_HEBREW = "iw"; // "he" is the official language code of Hebrew. // Java uses the deprecated "iw" code instead of the standard "he" code for Hebrew.
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
    //si_LK: Sinhala (Sri Lanka)/sinhala # This is a preliminary keyboard layout.
    private static final String LOCALE_SLOVAK = "sk";
    private static final String LOCALE_SLOVENIAN = "sl";
    private static final String LOCALE_SERBIAN = "sr";
    private static final String LOCALE_SERBIAN_LATIN = "sr_ZZ";
    private static final String LOCALE_SWEDISH = "sv";
    private static final String LOCALE_SWAHILI = "sw";
    private static final String LOCALE_TAMIL_INDIA = "ta_IN";
    //ta_LK: Tamil (Sri Lanka)/tamil # Disabled in conjunction with si_LK.
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

    //TODO: this probably should be private
    public static final String[] sSupportedLocales = new String[] {
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

    public static List<MySubtype> getSubtypes(final String locale, final Resources resources) {
        //TODO: don't do this - temporary
        mResources = resources;

        List<MySubtype> subtypes = new ArrayList<>();
        switch (locale) {
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
                subtypes.add(createSubtype(locale, LAYOUT_QWERTY));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_CZECH:
            case LOCALE_GERMAN:
            case LOCALE_CROATIAN:
            case LOCALE_HUNGARIAN:
            case LOCALE_SLOVENIAN:
                subtypes.add(createSubtype(locale, LAYOUT_QWERTZ));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_FRENCH:
            case LOCALE_DUTCH_BELGIUM:
                subtypes.add(createSubtype(locale, LAYOUT_AZERTY));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_CATALAN:
            case LOCALE_SPANISH:
            case LOCALE_SPANISH_UNITED_STATES:
            case LOCALE_SPANISH_LATIN_AMERICA:
            case LOCALE_BASQUE_SPAIN:
            case LOCALE_GALICIAN_SPAIN:
            case LOCALE_TAGALOG:
                subtypes.add(createSubtype(locale, LAYOUT_SPANISH));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_ESPERANTO:
                subtypes.add(createSubtype(locale, LAYOUT_SPANISH));
                break;
            case LOCALE_DANISH:
            case LOCALE_ESTONIAN_ESTONIA:
            case LOCALE_FINNISH:
            case LOCALE_NORWEGIAN_BOKMAL:
            case LOCALE_SWEDISH:
                subtypes.add(createSubtype(locale, LAYOUT_NORDIC));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_GERMAN_SWITZERLAND:
            case LOCALE_FRENCH_SWITZERLAND:
            case LOCALE_ITALIAN_SWITZERLAND:
                subtypes.add(createSubtype(locale, LAYOUT_SWISS));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_TURKISH:
                subtypes.add(createSubtype(locale, LAYOUT_QWERTY));
                subtypes.add(createSubtype(locale, LAYOUT_TURKISH_F,
                        R.string.subtype_generic_f, R.string.subtype_f));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_UZBEK_UZBEKISTAN:
                subtypes.add(createSubtype(locale, LAYOUT_UZBEK));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_ARABIC:
                subtypes.add(createSubtype(locale, LAYOUT_ARABIC));
                break;
            case LOCALE_BELARUSIAN_BELARUS:
            case LOCALE_KAZAKH:
            case LOCALE_KYRGYZ:
            case LOCALE_RUSSIAN:
            case LOCALE_UKRAINIAN:
                subtypes.add(createSubtype(locale, LAYOUT_EAST_SLAVIC));
                break;
            case LOCALE_BULGARIAN:
                subtypes.add(createSubtype(locale, LAYOUT_BULGARIAN));
                subtypes.add(createSubtype(locale, LAYOUT_BULGARIAN_BDS, R.string.subtype_bds));
                break;
            case LOCALE_BENGALI_BANGLADESH:
                subtypes.add(createSubtype(locale, LAYOUT_BENGALI_AKKHOR));
                break;
            case LOCALE_BENGALI_INDIA:
                subtypes.add(createSubtype(locale, LAYOUT_BENGALI));
                break;
            case LOCALE_GREEK:
                subtypes.add(createSubtype(locale, LAYOUT_GREEK));
                break;
            case LOCALE_PERSIAN:
                subtypes.add(createSubtype(locale, LAYOUT_FARSI));
                break;
            case LOCALE_HINDI:
                subtypes.add(createSubtype(locale, LAYOUT_HINDI));
                subtypes.add(createSubtype(locale, LAYOUT_HINDI_COMPACT, R.string.subtype_compact));
                break;
            case LOCALE_ARMENIAN_ARMENIA:
                subtypes.add(createSubtype(locale, LAYOUT_ARMENIAN_PHONETIC));
                break;
            case LOCALE_HEBREW:
                subtypes.add(createSubtype(locale, LAYOUT_HEBREW));
                break;
            case LOCALE_GEORGIAN_GEORGIA:
                subtypes.add(createSubtype(locale, LAYOUT_GEORGIAN));
                break;
            case LOCALE_KHMER_CAMBODIA:
                subtypes.add(createSubtype(locale, LAYOUT_KHMER));
                break;
            case LOCALE_KANNADA_INDIA:
                subtypes.add(createSubtype(locale, LAYOUT_KANNADA));
                break;
            case LOCALE_LAO_LAOS:
                subtypes.add(createSubtype(locale, LAYOUT_LAO));
                break;
            case LOCALE_MACEDONIAN:
                subtypes.add(createSubtype(locale, LAYOUT_MACEDONIAN));
                break;
            case LOCALE_MALAYALAM_INDIA:
                subtypes.add(createSubtype(locale, LAYOUT_MALAYALAM));
                break;
            case LOCALE_MONGOLIAN_MONGOLIA:
                subtypes.add(createSubtype(locale, LAYOUT_MONGOLIAN));
                break;
            case LOCALE_MARATHI_INDIA:
                subtypes.add(createSubtype(locale, LAYOUT_MARATHI));
                break;
            case LOCALE_NEPALI_NEPAL:
                subtypes.add(createSubtype(locale, LAYOUT_NEPALI_ROMANIZED));
                subtypes.add(createSubtype(locale, LAYOUT_NEPALI_TRADITIONAL,
                        R.string.subtype_traditional));
                break;
            case LOCALE_SERBIAN:
                subtypes.add(createSubtype(locale, LAYOUT_SERBIAN));
                break;
            case LOCALE_SERBIAN_LATIN:
                subtypes.add(createSubtype(locale, LAYOUT_SERBIAN_QWERTZ));
                addGenericLayouts(subtypes, locale);
                break;
            case LOCALE_TAMIL_INDIA:
            case LOCALE_TAMIL_SINGAPORE:
                subtypes.add(createSubtype(locale, LAYOUT_TAMIL));
                break;
            case LOCALE_TELUGU_INDIA:
                subtypes.add(createSubtype(locale, LAYOUT_TELUGU));
                break;
            case LOCALE_THAI:
                subtypes.add(createSubtype(locale, LAYOUT_THAI));
                break;
            case LOCALE_URDU:
                subtypes.add(createSubtype(locale, LAYOUT_URDU));
                break;
        }
        return subtypes;
    }

    private static MySubtype createSubtype(final String locale, final String keyboardLayoutSet) {
        final String[] predefinedLayouts = mResources.getStringArray(R.array.predefined_layouts);
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
        return new MySubtype(locale, keyboardLayoutSet, layoutNameStr, false, mResources);
    }

    private static MySubtype createSubtype(final String locale, final String keyboardLayoutSet,
                                           final int layoutRes) {
        return new MySubtype(locale, keyboardLayoutSet, layoutRes, true, mResources);
    }

    private static void addGenericLayouts(final List<MySubtype> subtypes, final String locale) {
        final int initialSize = subtypes.size();
        final String[] predefinedKeyboardLayoutSets = mResources.getStringArray(
                R.array.predefined_layouts);
        final String[] predefinedKeyboardLayoutSetDisplayNames = mResources.getStringArray(
                R.array.predefined_layout_display_names);
        for (int i = 0; i < predefinedKeyboardLayoutSets.length; i++) {
            final String predefinedLayout = predefinedKeyboardLayoutSets[i];
            boolean alreadyExists = false;
            for (int j = 0; j < initialSize; j++) {
                if (subtypes.get(j).getLayoutSet().equals(predefinedLayout)) {
                    alreadyExists = true;
                    break;
                }
            }
            if (alreadyExists) {
                continue;
            }

            final MySubtype mySubtype = new MySubtype(locale,
                    predefinedLayout, predefinedKeyboardLayoutSetDisplayNames[i], true, mResources);
            subtypes.add(mySubtype);
        }
    }

    public static List<MySubtype> getDefaultSubtypes(final Resources resources) {
        final ArrayList<Locale> supportedLocales = new ArrayList<>(sSupportedLocales.length);
        for (final String localeString : sSupportedLocales) {
            supportedLocales.add(LocaleUtils.constructLocaleFromString(localeString));
        }
        final List<Locale> systemLocales = LocaleUtils.getSystemLocales();
        final ArrayList<MySubtype> subtypes = new ArrayList<>();
        for (final Locale systemLocale : systemLocales) {
            final Locale bestLocale = LocaleUtils.findBestLocale(systemLocale, supportedLocales);
            if (bestLocale != null) {
                final String bestLocaleString = LocaleUtils.getLocaleString(bestLocale);
                final MySubtype subtype = getSubtypes(bestLocaleString, resources).get(0);
                if (!subtypes.contains(subtype)) {
                    subtypes.add(subtype);
                }
            }
        }
        if (subtypes.size() == 0) {
            subtypes.add(getSubtypes(LOCALE_ENGLISH_UNITED_STATES, resources).get(0));
        }
        return subtypes;
    }
}
