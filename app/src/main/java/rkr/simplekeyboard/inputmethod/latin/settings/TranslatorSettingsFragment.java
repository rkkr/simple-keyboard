package rkr.simplekeyboard.inputmethod.latin.settings;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import rkr.simplekeyboard.inputmethod.R;

public class TranslatorSettingsFragment extends SubScreenFragment {
    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs_screen_translator);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            removePreference(Settings.PREF_MATCHING_NAVBAR_COLOR);
        }
        setupTemperatureSettings();
    }


    private void setupTemperatureSettings(){
        final SeekBarDialogPreference pref = (SeekBarDialogPreference)findPreference(
                Settings.PREF_OPENAI_TEMPERATURE);
        if (pref == null) {
            return;
        }
        final SharedPreferences prefs = getSharedPreferences();
        pref.setInterface(new SeekBarDialogPreference.ValueProxy() {
            @Override
            public void writeValue(final int value, final String key) {
                prefs.edit().putInt(key, value).apply();
            }

            @Override
            public void writeDefaultValue(final String key) {
                prefs.edit().remove(key).apply();
            }
            private static final float PERCENTAGE_FLOAT = 100.0f;
            private int getPercentageFromValue(final float floatValue) {
                return Math.round(floatValue * PERCENTAGE_FLOAT);
            }

            @Override
            public int readValue(final String key) {
                return prefs.getInt(key,50);
            }

            @Override
            public int readDefaultValue(final String key) {
                return prefs.getInt(key,50);
            }

            @Override
            public void feedbackValue(final int value) {}

            @Override
            public String getValueText(final int value) {
                return String.valueOf(value);
            }
        });
    }
}
