/*
 * Copyright (C) 2012 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.os.Build;
import android.os.IBinder;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

/**
 * Enrichment class for InputMethodManager to simplify interaction and add functionality.
 */
// non final for easy mocking.
public class RichInputMethodManager {
    private static final String TAG = RichInputMethodManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    private RichInputMethodManager() {
        // This utility class is not publicly instantiable.
    }

    private static final RichInputMethodManager sInstance = new RichInputMethodManager();

    private InputMethodManager mImmService;

    private VirtualSubtypeManager mVirtualSubtypeManager;

    public static RichInputMethodManager getInstance() {
        sInstance.checkInitialized();
        return sInstance;
    }

    public static void init(final Context context) {
        sInstance.initInternal(context);
    }

    private boolean isInitialized() {
        return mImmService != null;
    }

    private void checkInitialized() {
        if (!isInitialized()) {
            throw new RuntimeException(TAG + " is used before initialization");
        }
    }

    private void initInternal(final Context context) {
        if (isInitialized()) {
            return;
        }
        mImmService = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        // Initialize additional subtypes.
        SubtypeLocaleUtils.init(context);

        mVirtualSubtypeManager = new VirtualSubtypeManager(context);
    }

    public HashSet<MySubtype> getEnabledSubtypesOfThisIme() {
        return mVirtualSubtypeManager.getAll();
    }

    public boolean addSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.addSubtype(subtype);
    }

    public boolean removeSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.removeSubtype(subtype);
    }

    public void resetSubtypeCycleOrder() {
        mVirtualSubtypeManager.resetSubtypeCycleOrder();
    }

    public MySubtype findSubtypeByLocale(final Locale locale) {
        final HashSet<MySubtype> subtypes = getEnabledSubtypesOfThisIme();
        final ArrayList<Locale> enabledLocales = new ArrayList<>(subtypes.size());
        for (final MySubtype subtype : subtypes) {
            enabledLocales.add(subtype.getLocaleObject());
        }
        final Locale bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales);
        if (bestLocale != null) {
            for (final MySubtype subtype : subtypes) {
                if (bestLocale.equals(subtype.getLocaleObject())) {
                    return subtype;
                }
            }
        }
        return null;
    }

    public boolean setCurrentSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.setCurrentSubtype(subtype);
    }

    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (onlyCurrentIme) {
            if (!mVirtualSubtypeManager.hasMultiple()) {
                return false;
            }
            return mVirtualSubtypeManager.switchToNextSubtype(true);
        }
        if (mVirtualSubtypeManager.switchToNextSubtype(false)) {
            return true;
        }
        // switch to a different IME
        return mImmService.switchToNextInputMethod(token, false);
    }

    public Locale getCurrentSubtypeLocale() {
        return getCurrentSubtype().getLocaleObject();
    }

    public MySubtype getCurrentSubtype() {
        return mVirtualSubtypeManager.getCurrentSubtype();
    }

    public boolean hasMultipleEnabledImesOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        return hasMultipleEnabledSubtypesInThisIme()
                || hasMultipleEnabledImes(shouldIncludeAuxiliarySubtypes);
    }

    public boolean hasMultipleEnabledSubtypesInThisIme() {
        return mVirtualSubtypeManager.hasMultiple();
    }

    private boolean hasMultipleEnabledImes(final boolean shouldIncludeAuxiliarySubtypes) {
        final List<InputMethodInfo> imiList = mImmService.getEnabledInputMethodList();

        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) {
                return true;
            }
            final List<InputMethodSubtype> subtypes =
                    mImmService.getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should be counted.
            if (subtypes.isEmpty()) {
                ++filteredImisCount;
                continue;
            }

            int auxCount = 0;
            for (InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    ++auxCount;
                }
            }
            final int nonAuxCount = subtypes.size() - auxCount;

            // IMEs that have one or more non-auxiliary subtypes should be counted.
            // If shouldIncludeAuxiliarySubtypes is true, IMEs that have two or more auxiliary
            // subtypes should be counted as well.
            if (nonAuxCount > 0 || (shouldIncludeAuxiliarySubtypes && auxCount > 1)) {
                ++filteredImisCount;
            }
        }

        return filteredImisCount > 1;
    }

    public boolean shouldOfferSwitchingToOtherInputMethods(final IBinder binder) {
        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return false;
        }
        return mImmService.shouldOfferSwitchingToNextInputMethod(binder);
    }

    public void showInputMethodPicker() {
        //TODO: implement a virtual subtype picker
        mImmService.showInputMethodPicker();
    }
}
