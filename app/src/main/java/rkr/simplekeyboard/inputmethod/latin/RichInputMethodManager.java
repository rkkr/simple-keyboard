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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.settings.Settings;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypePreferenceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.DialogUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.LocaleResourceUtils;
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

    private SubtypeList mSubtypeList;
    private SubtypeChangedHandler mSubtypeChangedHandler;

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
        LocaleResourceUtils.init(context);

        // Initialize the virtual subtypes
        mSubtypeList = new SubtypeList(context);
    }

    public void setSubtypeChangeHandler(final SubtypeChangedHandler handler) {
        mSubtypeChangedHandler = handler;
    }

    private void notifySubtypeChanged() {
        if (mSubtypeChangedHandler != null) {
            mSubtypeChangedHandler.onCurrentSubtypeChanged();
        }
    }

    public interface SubtypeChangedHandler {
        void onCurrentSubtypeChanged();
    }

    private static class SubtypeList {
        private final List<Subtype> mSubtypes;
        private int mCurrentSubtypeIndex = 0;
        private final SharedPreferences mPrefs;

        public SubtypeList(final Context context) {
            mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);

            final String prefSubtypes = Settings.readPrefSubtypes(mPrefs);
            final Subtype[] subtypes = SubtypePreferenceUtils.createSubtypesFromPref(prefSubtypes,
                    context.getResources());
            if (subtypes == null || subtypes.length < 1) {
                mSubtypes = SubtypeLocaleUtils.getDefaultSubtypes(context.getResources());
                mCurrentSubtypeIndex = 0;
            } else {
                mSubtypes = new ArrayList<>(Arrays.asList(subtypes));
                mCurrentSubtypeIndex = Settings.readPrefCurrentSubtypeIndex(mPrefs);
                if (mCurrentSubtypeIndex > mSubtypes.size()) {
                    mCurrentSubtypeIndex = 0;
                }
            }
        }

        public synchronized Set<Subtype> getAll(final boolean sortForDisplay) {
            final Set<Subtype> subtypes;
            if (sortForDisplay) {
                subtypes = new TreeSet<>(new Comparator<Subtype>() {
                    @Override
                    public int compare(Subtype a, Subtype b) {
                        return a.getName().compareToIgnoreCase(b.getName());
                    }
                });
            } else {
                subtypes = new HashSet<>();
            }
            subtypes.addAll(mSubtypes);
            return subtypes;
        }

        public synchronized int size() {
            return mSubtypes.size();
        }

        private void saveSubtypeListPref() {
            final String prefSubtypes = SubtypePreferenceUtils.createPrefSubtypes(mSubtypes);
            Settings.writePrefSubtypes(mPrefs, prefSubtypes);
        }

        private void saveSubtypeIndexPref() {
            Settings.writePrefCurrentSubtypeIndex(mPrefs, mCurrentSubtypeIndex);
        }

        public synchronized boolean addSubtype(final Subtype subtype) {
            if (mSubtypes.contains(subtype)) {
                return false;
            }
            if (!mSubtypes.add(subtype)) {
                return false;
            }
            saveSubtypeListPref();
            return true;
        }

        public synchronized boolean removeSubtype(final Subtype subtype) {
            if (mSubtypes.size() == 1) {
                return false;
            }

            final int index = mSubtypes.indexOf(subtype);
            if (index < 0) {
                // nothing to remove
                return true;
            }

            final boolean indexUpdated;
            if (mCurrentSubtypeIndex == index) {
                mCurrentSubtypeIndex = 0;
                indexUpdated = true;
            } else if (mCurrentSubtypeIndex > index) {
                mCurrentSubtypeIndex--;
                indexUpdated = true;
            } else {
                indexUpdated = false;
            }

            mSubtypes.remove(index);
            saveSubtypeListPref();
            if (indexUpdated) {
                saveSubtypeIndexPref();
            }
            return true;
        }

        public synchronized void resetSubtypeCycleOrder() {
            if (mCurrentSubtypeIndex == 0) {
                return;
            }

            // move the current subtype to the top of the list and shift everything above it down
            Collections.rotate(mSubtypes.subList(0, mCurrentSubtypeIndex + 1), 1);
            mCurrentSubtypeIndex = 0;
            saveSubtypeListPref();
            saveSubtypeIndexPref();
        }

        public synchronized boolean setCurrentSubtype(final Subtype subtype) {
            for (int i = 0; i < mSubtypes.size(); i++) {
                if (mSubtypes.get(i).equals(subtype)) {
                    mCurrentSubtypeIndex = i;
                    setCurrentSubtype(i);
                    return true;
                }
            }
            return false;
        }

        public synchronized boolean setCurrentSubtype(final Locale locale) {
            final ArrayList<Locale> enabledLocales = new ArrayList<>(mSubtypes.size());
            for (final Subtype subtype : mSubtypes) {
                enabledLocales.add(subtype.getLocaleObject());
            }
            final Locale bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales);
            if (bestLocale != null) {
                for (int i = 0; i < mSubtypes.size(); i++) {
                    final Subtype subtype = mSubtypes.get(i);
                    if (bestLocale.equals(subtype.getLocaleObject())) {
                        setCurrentSubtype(i);
                        return true;
                    }
                }
            }
            return false;
        }

        private void setCurrentSubtype(final int index) {
            mCurrentSubtypeIndex = index;
            if (index == 0) {
                saveSubtypeIndexPref();
            } else {
                // since the subtype was selected directly, the cycle should be reset so switching
                // to the next subtype can iterate through all of the rest of the subtypes
                resetSubtypeCycleOrder();
            }
        }

        public synchronized boolean switchToNextSubtype() {
            if (mSubtypes.size() < 2) {
                // reached the end of the loop
                return false;
            }
            mCurrentSubtypeIndex = (mCurrentSubtypeIndex + 1) % mSubtypes.size();
            saveSubtypeIndexPref();
            // loop is only reset if the index is put back at 0
            return mCurrentSubtypeIndex > 0;
        }

        public synchronized Subtype getCurrentSubtype() {
            return mSubtypes.get(mCurrentSubtypeIndex);
        }
    }

    public Set<Subtype> getEnabledSubtypes(final boolean sortForDisplay) {
        return mSubtypeList.getAll(sortForDisplay);
    }

    public boolean hasMultipleEnabledSubtypes() {
        return mSubtypeList.size() > 1;
    }

    public boolean addSubtype(final Subtype subtype) {
        return mSubtypeList.addSubtype(subtype);
    }

    public boolean removeSubtype(final Subtype subtype) {
        final Subtype curSubtype = mSubtypeList.getCurrentSubtype();
        final boolean result = mSubtypeList.removeSubtype(subtype);
        if (curSubtype != mSubtypeList.getCurrentSubtype()) {
            notifySubtypeChanged();
        }
        return result;
    }

    public void resetSubtypeCycleOrder() {
        mSubtypeList.resetSubtypeCycleOrder();
    }

    public boolean setCurrentSubtype(final Subtype subtype) {
        if (mSubtypeList.setCurrentSubtype(subtype)) {
            notifySubtypeChanged();
            return true;
        }
        return false;
    }

    public boolean setCurrentSubtype(final Locale locale) {
        if (mSubtypeList.setCurrentSubtype(locale)) {
            notifySubtypeChanged();
            return true;
        }
        return false;
    }

    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (onlyCurrentIme) {
            if (!hasMultipleEnabledSubtypes()) {
                return false;
            }
            return switchToNextSubtype(true);
        }
        if (switchToNextSubtype(false)) {
            return true;
        }
        // switch to a different IME
        return mImmService.switchToNextInputMethod(token, false);
    }

    private boolean switchToNextSubtype(final boolean cycle) {
        if (mSubtypeList.switchToNextSubtype() || cycle) {
            notifySubtypeChanged();
            return true;
        } else {
            return false;
        }
    }

    public Locale getCurrentSubtypeLocale() {
        return getCurrentSubtype().getLocaleObject();
    }

    public Subtype getCurrentSubtype() {
        return mSubtypeList.getCurrentSubtype();
    }

    public boolean hasMultipleEnabledImesOrSubtypes(final boolean shouldIncludeAuxiliarySubtypes) {
        return hasMultipleEnabledSubtypes()
                || hasMultipleEnabledImes(shouldIncludeAuxiliarySubtypes);
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
        mImmService.showInputMethodPicker();
    }

    public AlertDialog showSubtypePicker(final Context context, final IBinder windowToken) {
        if (windowToken == null) {
            return null;
        }
        final CharSequence title = context.getString(R.string.change_keyboard);

        final Set<Subtype> subtypes = mSubtypeList.getAll(true);

        final CharSequence[] items = new CharSequence[subtypes.size()];
        final Subtype currentSubtype = getCurrentSubtype();
        int currentSubtypeIndex = 0;
        int i = 0;
        for (final Subtype subtype : subtypes) {
            if (subtype.equals(currentSubtype)) {
                currentSubtypeIndex = i;
            }
            items[i++] = subtype.getName();
        }
        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int position) {
                di.dismiss();
                int i = 0;
                for (final Subtype subtype : subtypes) {
                    if (i == position) {
                        setCurrentSubtype(subtype);
                        break;
                    }
                    i++;
                }
            }
        };
        final AlertDialog.Builder builder = new AlertDialog.Builder(
                DialogUtils.getPlatformDialogThemeContext(context));
        builder.setSingleChoiceItems(items, currentSubtypeIndex, listener).setTitle(title);
        final AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        final Window window = dialog.getWindow();
        final WindowManager.LayoutParams lp = window.getAttributes();
        lp.token = windowToken;
        lp.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
        window.setAttributes(lp);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

        dialog.show();
        return dialog;
    }
}
