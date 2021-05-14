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
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.HashMap;
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
    private InputMethodInfoCache mInputMethodInfoCache;
    private VirtualSubtypeManager mVirtualSubtypeManager;

    private static final int INDEX_NOT_FOUND = -1;

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
        mInputMethodInfoCache = new InputMethodInfoCache(
                mImmService, context.getPackageName());

        // Initialize additional subtypes.
        SubtypeLocaleUtils.init(context);

        // Initialize the current input method subtype.
        refreshSubtypeCaches();

        mVirtualSubtypeManager = new VirtualSubtypeManager(context);
    }

    public InputMethodManager getInputMethodManager() {
        checkInitialized();
        return mImmService;
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
        return switchToNextOtherInputMethod(token);
    }

    private boolean switchToNextOtherInputMethod(final IBinder token) {
        if (mImmService.switchToNextInputMethod(token, false)) {
            return true;
        }
        return switchToNextInputMethodAndSubtype(token);
    }

    private boolean switchToNextInputMethodAndSubtype(final IBinder token) {
        final List<InputMethodInfo> enabledImis = mImmService.getEnabledInputMethodList();
        final int currentIndex = getImiIndexInList(getInputMethodInfoOfThisIme(), enabledImis);
        if (currentIndex == INDEX_NOT_FOUND) {
            Log.w(TAG, "Can't find current IME in enabled IMEs: IME package="
                    + getInputMethodInfoOfThisIme().getPackageName());
            return false;
        }
        final InputMethodInfo nextImi = getNextNonAuxiliaryIme(currentIndex, enabledImis);
        final List<InputMethodSubtype> enabledSubtypes = getEnabledInputMethodSubtypeList(nextImi,
                true /* allowsImplicitlySelectedSubtypes */);
        if (enabledSubtypes.isEmpty()) {
            // The next IME has no subtype.
            mImmService.setInputMethod(token, nextImi.getId());
            return true;
        }
        final InputMethodSubtype firstSubtype = enabledSubtypes.get(0);
        mImmService.setInputMethodAndSubtype(token, nextImi.getId(), firstSubtype);
        return true;
    }

    private static int getImiIndexInList(final InputMethodInfo inputMethodInfo,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int index = 0; index < count; index++) {
            final InputMethodInfo imi = imiList.get(index);
            if (imi.equals(inputMethodInfo)) {
                return index;
            }
        }
        return INDEX_NOT_FOUND;
    }

    // This method mimics {@link InputMethodManager#switchToNextInputMethod(IBinder,boolean)}.
    private static InputMethodInfo getNextNonAuxiliaryIme(final int currentIndex,
            final List<InputMethodInfo> imiList) {
        final int count = imiList.size();
        for (int i = 1; i < count; i++) {
            final int nextIndex = (currentIndex + i) % count;
            final InputMethodInfo nextImi = imiList.get(nextIndex);
            if (!isAuxiliaryIme(nextImi)) {
                return nextImi;
            }
        }
        return imiList.get(currentIndex);
    }

    // Copied from {@link InputMethodInfo}. See how auxiliary of IME is determined.
    private static boolean isAuxiliaryIme(final InputMethodInfo imi) {
        final int count = imi.getSubtypeCount();
        if (count == 0) {
            return false;
        }
        for (int index = 0; index < count; index++) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(index);
            if (!subtype.isAuxiliary()) {
                return false;
            }
        }
        return true;
    }

    public static class InputMethodInfoCache {
        private final InputMethodManager mImm;
        private final String mImePackageName;

        private InputMethodInfo mCachedThisImeInfo;
        private final HashMap<InputMethodInfo, List<InputMethodSubtype>>
                mCachedSubtypeListWithImplicitlySelected;
        private final HashMap<InputMethodInfo, List<InputMethodSubtype>>
                mCachedSubtypeListOnlyExplicitlySelected;

        public InputMethodInfoCache(final InputMethodManager imm, final String imePackageName) {
            mImm = imm;
            mImePackageName = imePackageName;
            mCachedSubtypeListWithImplicitlySelected = new HashMap<>();
            mCachedSubtypeListOnlyExplicitlySelected = new HashMap<>();
        }

        public synchronized boolean isInputMethodOfThisImeEnabled() {
            for (final InputMethodInfo imi : mImm.getEnabledInputMethodList()) {
                if (imi.getPackageName().equals(mImePackageName)) {
                    return true;
                }
            }
            return false;
        }

        public synchronized InputMethodInfo getInputMethodOfThisIme() {
            if (mCachedThisImeInfo != null) {
                return mCachedThisImeInfo;
            }
            for (final InputMethodInfo imi : mImm.getInputMethodList()) {
                if (imi.getPackageName().equals(mImePackageName)) {
                    mCachedThisImeInfo = imi;
                    return imi;
                }
            }
            throw new RuntimeException("Input method id for " + mImePackageName + " not found.");
        }

        public synchronized List<InputMethodSubtype> getEnabledInputMethodSubtypeList(
                final InputMethodInfo imi, final boolean allowsImplicitlySelectedSubtypes) {
            final HashMap<InputMethodInfo, List<InputMethodSubtype>> cache =
                    allowsImplicitlySelectedSubtypes
                    ? mCachedSubtypeListWithImplicitlySelected
                    : mCachedSubtypeListOnlyExplicitlySelected;
            final List<InputMethodSubtype> cachedList = cache.get(imi);
            if (cachedList != null) {
                return cachedList;
            }
            final List<InputMethodSubtype> result = mImm.getEnabledInputMethodSubtypeList(
                    imi, allowsImplicitlySelectedSubtypes);
            cache.put(imi, result);
            return result;
        }

        public synchronized void clear() {
            mCachedThisImeInfo = null;
            mCachedSubtypeListWithImplicitlySelected.clear();
            mCachedSubtypeListOnlyExplicitlySelected.clear();
        }
    }

    public InputMethodInfo getInputMethodInfoOfThisIme() {
        return mInputMethodInfoCache.getInputMethodOfThisIme();
    }

    public String getInputMethodIdOfThisIme() {
        return getInputMethodInfoOfThisIme().getId();
    }

    public Locale getCurrentSubtypeLocale() {
        return getCurrentSubtype().getLocaleObject();
    }

    public MySubtype getCurrentSubtype() {
        return mVirtualSubtypeManager.getCurrentSubtype();
    }

    public boolean hasMultipleEnabledImesOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        if (hasMultipleEnabledSubtypesInThisIme()) {
            return true;
        }
        final List<InputMethodInfo> enabledImis = mImmService.getEnabledInputMethodList();
        return hasMultipleEnabledImes(shouldIncludeAuxiliarySubtypes, enabledImis);
    }

    public boolean hasMultipleEnabledSubtypesInThisIme() {
        return mVirtualSubtypeManager.hasMultiple();
    }

    private boolean hasMultipleEnabledImes(final boolean shouldIncludeAuxiliarySubtypes,
                                           final List<InputMethodInfo> imiList) {
        // Number of the filtered IMEs
        int filteredImisCount = 0;

        for (InputMethodInfo imi : imiList) {
            // We can return true immediately after we find two or more filtered IMEs.
            if (filteredImisCount > 1) {
                return true;
            }
            final List<InputMethodSubtype> subtypes = getEnabledInputMethodSubtypeList(imi, true);
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

    private List<InputMethodSubtype> getEnabledInputMethodSubtypeList(final InputMethodInfo imi,
            final boolean allowsImplicitlySelectedSubtypes) {
        return mInputMethodInfoCache.getEnabledInputMethodSubtypeList(
                imi, allowsImplicitlySelectedSubtypes);
    }

    public void refreshSubtypeCaches() {
        mInputMethodInfoCache.clear();
    }

    public boolean shouldOfferSwitchingToNextInputMethod(final IBinder binder) {
        //TODO: fix this - this is only used for switching to other IMEs

        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return hasMultipleEnabledSubtypesInThisIme();
        }
        return mImmService.shouldOfferSwitchingToNextInputMethod(binder);
    }

    public boolean setCurrentSubtype(final MySubtype subtype) {
        return mVirtualSubtypeManager.setCurrentSubtype(subtype);
    }
}
