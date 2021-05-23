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
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeUtils;

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

    private List<Subtype> mSubtypes;
    private int mCurrentSubtypeIndex = 0;
    private SharedPreferences mPrefs;
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

        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);

        // Initialize the virtual subtypes
        final String prefSubtypes = Settings.readPrefSubtypes(mPrefs);
        final Subtype[] subtypes = SubtypePreferenceUtils.createSubtypesFromPref(prefSubtypes,
                context.getResources());
        if (subtypes == null || subtypes.length < 1) {
            mSubtypes = SubtypeUtils.getDefaultSubtypes(context.getResources());
            mCurrentSubtypeIndex = 0;
        } else {
            mSubtypes = new ArrayList<>(Arrays.asList(subtypes));
            mCurrentSubtypeIndex = Settings.readPrefCurrentSubtypeIndex(mPrefs);
            if (mCurrentSubtypeIndex > mSubtypes.size()) {
                mCurrentSubtypeIndex = 0;
            }
        }
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

    private void saveSubtypePref(final boolean subtypesUpdated, final boolean indexUpdated) {
        //TODO: make atomic?
        if (indexUpdated) {
            Settings.writePrefCurrentSubtypeIndex(mPrefs, mCurrentSubtypeIndex);
        }
        if (subtypesUpdated) {
            final String prefSubtypes = SubtypePreferenceUtils.createPrefSubtypes(mSubtypes);
            Settings.writePrefSubtypes(mPrefs, prefSubtypes);
        }
    }

    public Set<Subtype> getEnabledSubtypesOfThisIme(final boolean sortForDisplay) {
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

    public boolean hasMultipleEnabledSubtypes() {
        return mSubtypes.size() > 1;
    }

    public boolean addSubtype(final Subtype subtype) {
        if (mSubtypes.contains(subtype)) {
            return false;
        }
        if (!mSubtypes.add(subtype)) {
            return false;
        }
        saveSubtypePref(true, false);
        return true;
    }

    public boolean removeSubtype(final Subtype subtype) {
        if (mSubtypes.size() == 1) {
            //TODO: is this how this should be handled?
            return false;
        }

        final int index = mSubtypes.indexOf(subtype);
        if (index < 0) {
            return false;
        }

        final boolean indexUpdated = mCurrentSubtypeIndex == index;
        if (indexUpdated) {
            mCurrentSubtypeIndex = 0;
            //TODO: maybe notify LatinIME, but possibly not since the keyboard won't already be open (double check split screen)
        }

        mSubtypes.remove(index);
        saveSubtypePref(true, indexUpdated);
        return true;
    }

    public void resetSubtypeCycleOrder() {
        if (mCurrentSubtypeIndex == 0) {
            return;
        }
        //TODO: maybe consider multiple threads

        // move the current subtype to the top of the list and shift everything above it down one
        Collections.rotate(mSubtypes.subList(0, mCurrentSubtypeIndex + 1), 1);
        mCurrentSubtypeIndex = 0;
        saveSubtypePref(true, true);
    }

    public Subtype findSubtypeByLocale(final Locale locale) {
        final Collection<Subtype> subtypes = mSubtypes;
        final ArrayList<Locale> enabledLocales = new ArrayList<>(subtypes.size());
        for (final Subtype subtype : subtypes) {
            enabledLocales.add(subtype.getLocaleObject());
        }
        final Locale bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales);
        if (bestLocale != null) {
            for (final Subtype subtype : subtypes) {
                if (bestLocale.equals(subtype.getLocaleObject())) {
                    return subtype;
                }
            }
        }
        return null;
    }

    public boolean setCurrentSubtype(final Subtype subtype) {
        for (int i = 0; i < mSubtypes.size(); i++) {
            if (mSubtypes.get(i).equals(subtype)) {
                mCurrentSubtypeIndex = i;
                if (i == 0) {
                    saveSubtypePref(false, true);
                } else {
                    resetSubtypeCycleOrder();
                }
                notifySubtypeChanged();
                return true;
            }
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
        if (mSubtypes.size() < 2) {
            return false;
        }
        mCurrentSubtypeIndex = (mCurrentSubtypeIndex + 1) % mSubtypes.size();
        saveSubtypePref(false, true);
        if (mCurrentSubtypeIndex > 0 || cycle) {
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
        return mSubtypes.get(mCurrentSubtypeIndex);
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

        final Set<Subtype> subtypes = getEnabledSubtypesOfThisIme(true);

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
