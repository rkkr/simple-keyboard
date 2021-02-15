/*
 * Copyright (C) 2011 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceFragment;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;

/**
 * This is a helper class for an IME's settings preference fragment. It's recommended for every
 * IME to have its own settings preference fragment which inherits this class.
 */
public abstract class InputMethodSettingsFragment extends PreferenceFragment
        implements InputMethodSettingsInterface {
    private final InputMethodSettingsImpl mSettings = new InputMethodSettingsImpl();
    private final Handler handler =new Handler();
    private final Runnable refreshSubtypeRunnable = new Runnable() {
        @Override
        public void run() {
            mSettings.updateSubtypeEnabler();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getActivity();
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(PreferenceManagerCompat.getDeviceContext(context)));
        mSettings.init(context, getPreferenceScreen());
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputMethodSettingsCategoryTitle(int resId) {
        mSettings.setInputMethodSettingsCategoryTitle(resId);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setInputMethodSettingsCategoryTitle(CharSequence title) {
        mSettings.setInputMethodSettingsCategoryTitle(title);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerTitle(int resId) {
        mSettings.setSubtypeEnablerTitle(resId);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerTitle(CharSequence title) {
        mSettings.setSubtypeEnablerTitle(title);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerIcon(int resId) {
        mSettings.setSubtypeEnablerIcon(resId);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSubtypeEnablerIcon(Drawable drawable) {
        mSettings.setSubtypeEnablerIcon(drawable);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(refreshSubtypeRunnable, 1000);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }
}
