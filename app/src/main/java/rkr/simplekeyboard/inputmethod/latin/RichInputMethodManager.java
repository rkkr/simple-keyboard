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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;

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

    private RichInputMethodManager() {
        // This utility class is not publicly instantiable.
    }

    private static final RichInputMethodManager sInstance = new RichInputMethodManager();

    private InputMethodManager mImmService;

    private SubtypeList mSubtypeList;

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

        LocaleResourceUtils.init(context);

        // Initialize the virtual subtypes
        mSubtypeList = new SubtypeList(context);
    }

    /**
     * Add a listener to be called when the virtual subtype changes.
     * @param listener the listener to call when the subtype changes.
     */
    public void setSubtypeChangeHandler(final SubtypeChangedListener listener) {
        mSubtypeList.setSubtypeChangeHandler(listener);
    }

    /**
     * Interface used to allow some code to run when the virtual subtype changes.
     */
    public interface SubtypeChangedListener {
        void onCurrentSubtypeChanged();
    }

    /**
     * Manager for the list of enabled subtypes that also handles which one is currently in use.
     * Only one of these should be created to avoid conflicts.
     */
    private static class SubtypeList {
        /** The list of enabled subtypes ordered by how they should be cycled through when moving to
         *  the next subtype. When a subtype is actually in use, it should be moved to the beginning
         *  of the list so that the next time the user uses the switch to next subtype button, all
         *  of the subtypes can be iterated through before potentially switching to a different
         *  input method. */
        private final List<Subtype> mSubtypes;
        /** The index of the currently selected subtype. This is used for tracking the status of
         *  cycling through subtypes. When actually using the keyboard, the subtype should be moved
         *  to the beginning of the list, so this should normally be 0. */
        private int mCurrentSubtypeIndex;

        private final SharedPreferences mPrefs;
        private SubtypeChangedListener mSubtypeChangedListener;

        /**
         * Create the manager for the virtual subtypes.
         * @param context the context for this application.
         */
        public SubtypeList(final Context context) {
            mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);

            final String prefSubtypes = Settings.readPrefSubtypes(mPrefs);
            final List<Subtype> subtypes = SubtypePreferenceUtils.createSubtypesFromPref(
                    prefSubtypes, context.getResources());
            if (subtypes == null || subtypes.size() < 1) {
                mSubtypes = SubtypeLocaleUtils.getDefaultSubtypes(context.getResources());
            } else {
                mSubtypes = subtypes;
            }
            mCurrentSubtypeIndex = 0;
        }

        /**
         * Add a listener to be called when the virtual subtype changes.
         * @param listener the listener to call when the subtype changes.
         */
        public void setSubtypeChangeHandler(final SubtypeChangedListener listener) {
            mSubtypeChangedListener = listener;
        }

        /**
         * Call the subtype changed handler to indicate that the virtual subtype has changed.
         */
        public void notifySubtypeChanged() {
            if (mSubtypeChangedListener != null) {
                mSubtypeChangedListener.onCurrentSubtypeChanged();
            }
        }

        /**
         * Get all of the enabled languages.
         * @return the enabled languages.
         */
        public synchronized Set<Locale> getAllLocales() {
            final Set<Locale> locales = new HashSet<>();
            for (final Subtype subtype: mSubtypes) {
                locales.add(subtype.getLocaleObject());
            }
            return locales;
        }

        /**
         * Get all of the enabled subtypes for language.
         * @param locale filter by Locale.
         * @return the enabled subtypes.
         */
        public synchronized Set<Subtype> getAllForLocale(final String locale) {
            final Set<Subtype> subtypes = new HashSet<>();
            for (final Subtype subtype: mSubtypes) {
                if (subtype.getLocale().equals(locale))
                    subtypes.add(subtype);
            }
            return subtypes;
        }

        /**
         * Get all of the enabled subtypes.
         * @param sortForDisplay whether the subtypes should be sorted alphabetically by the display
         *                      name as opposed to having no particular order.
         * @return the enabled subtypes.
         */
        public synchronized Set<Subtype> getAll(final boolean sortForDisplay) {
            final Set<Subtype> subtypes;
            if (sortForDisplay) {
                subtypes = new TreeSet<>(new Comparator<Subtype>() {
                    @Override
                    public int compare(Subtype a, Subtype b) {
                        if (a.equals(b)) {
                            // ensure that this is consistent with equals
                            return 0;
                        }
                        final int result = a.getName().compareToIgnoreCase(b.getName());
                        if (result != 0) {
                            return result;
                        }
                        // ensure that non-equal objects are distinguished to be consistent with
                        // equals
                        return a.hashCode() > b.hashCode() ? 1 : -1;
                    }
                });
            } else {
                subtypes = new HashSet<>();
            }
            subtypes.addAll(mSubtypes);
            return subtypes;
        }

        /**
         * Get the number of enabled subtypes.
         * @return the number of enabled subtypes.
         */
        public synchronized int size() {
            return mSubtypes.size();
        }

        /**
         * Update the preference for the list of enabled subtypes.
         */
        private void saveSubtypeListPref() {
            final String prefSubtypes = SubtypePreferenceUtils.createPrefSubtypes(mSubtypes);
            Settings.writePrefSubtypes(mPrefs, prefSubtypes);
        }

        /**
         * Add a subtype to the list.
         * @param subtype the subtype to add.
         * @return whether the subtype was added to the list (or already existed in the list).
         */
        public synchronized boolean addSubtype(final Subtype subtype) {
            if (mSubtypes.contains(subtype)) {
                // don't allow duplicates, but since it's already in the list this can be considered
                // successful
                return true;
            }
            if (!mSubtypes.add(subtype)) {
                return false;
            }
            saveSubtypeListPref();
            return true;
        }

        /**
         * Remove a subtype from the list.
         * @param subtype the subtype to remove.
         * @return whether the subtype was removed (or wasn't even in the list).
         */
        public synchronized boolean removeSubtype(final Subtype subtype) {
            if (mSubtypes.size() == 1) {
                // there needs to be at least one subtype
                return false;
            }

            final int index = mSubtypes.indexOf(subtype);
            if (index < 0) {
                // nothing to remove
                return true;
            }

            final boolean subtypeChanged;
            if (mCurrentSubtypeIndex == index) {
                mCurrentSubtypeIndex = 0;
                subtypeChanged = true;
            } else {
                if (mCurrentSubtypeIndex > index) {
                    // make sure the current subtype is still pointed to when the other subtype is
                    // removed
                    mCurrentSubtypeIndex--;
                }
                subtypeChanged = false;
            }

            mSubtypes.remove(index);
            saveSubtypeListPref();
            if (subtypeChanged) {
                notifySubtypeChanged();
            }
            return true;
        }

        /**
         * Move the current subtype to the beginning of the list to allow the rest of the subtypes
         * to be cycled through before possibly switching to a separate input method. This should be
         * called whenever the user is done cycling through subtypes (eg: when a subtype is actually
         * used or the keyboard is closed).
         */
        public synchronized void resetSubtypeCycleOrder() {
            if (mCurrentSubtypeIndex == 0) {
                return;
            }

            // move the current subtype to the top of the list and shift everything above it down
            Collections.rotate(mSubtypes.subList(0, mCurrentSubtypeIndex + 1), 1);
            mCurrentSubtypeIndex = 0;
            saveSubtypeListPref();
        }

        /**
         * Set the current subtype to a specific subtype.
         * @param subtype the subtype to set as current.
         * @return whether the current subtype was set to the requested subtype.
         */
        public synchronized boolean setCurrentSubtype(final Subtype subtype) {
            if (getCurrentSubtype().equals(subtype)) {
                // nothing to do
                return true;
            }
            for (int i = 0; i < mSubtypes.size(); i++) {
                if (mSubtypes.get(i).equals(subtype)) {
                    setCurrentSubtype(i);
                    return true;
                }
            }
            return false;
        }

        /**
         * Set the current subtype to match a specified locale.
         * @param locale the locale to use.
         * @return whether the current subtype was set to the requested locale.
         */
        public synchronized boolean setCurrentSubtype(final Locale locale) {
            final ArrayList<Locale> enabledLocales = new ArrayList<>(mSubtypes.size());
            for (final Subtype subtype : mSubtypes) {
                enabledLocales.add(subtype.getLocaleObject());
            }
            final Locale bestLocale = LocaleUtils.findBestLocale(locale, enabledLocales);
            if (bestLocale != null) {
                // get the first subtype (most recently used) with a matching locale
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

        /**
         * Set the current subtype to a specified index. This should only be used when setting the
         * subtype to something specific (not when just iterating through the subtypes).
         * @param index the index of the subtype to set as current.
         */
        private void setCurrentSubtype(final int index) {
            if (mCurrentSubtypeIndex == index)
            {
                // nothing to do
                return;
            }
            mCurrentSubtypeIndex = index;
            if (index != 0) {
                // since the subtype was selected directly, the cycle should be reset so switching
                // to the next subtype can iterate through all of the rest of the subtypes
                resetSubtypeCycleOrder();
            }
            notifySubtypeChanged();
        }

        /**
         * Switch to the next subtype in the list.
         * @param notifyChangeOnCycle whether the subtype changed handler should be notified if the
         *                           end of the list is passed and the next subtype would go back to
         *                           the first in the list.
         * @return whether the subtype changed listener was called.
         */
        public synchronized boolean switchToNextSubtype(final boolean notifyChangeOnCycle) {
            final int nextIndex = mCurrentSubtypeIndex + 1;
            if (nextIndex >= mSubtypes.size()) {
                mCurrentSubtypeIndex = 0;
                if (!notifyChangeOnCycle) {
                    return false;
                }
            } else {
                mCurrentSubtypeIndex = nextIndex;
            }
            notifySubtypeChanged();
            return true;
        }

        /**
         * Get the subtype that is currently in use (or will be once the keyboard is opened).
         * @return the current subtype.
         */
        public synchronized Subtype getCurrentSubtype() {
            return mSubtypes.get(mCurrentSubtypeIndex);
        }
    }

    /**
     * Get all of the enabled subtypes.
     * @param sortForDisplay whether the subtypes should be sorted alphabetically by the display
     *                      name as opposed to having no particular order.
     * @return the enabled subtypes.
     */
    public Set<Subtype> getEnabledSubtypes(final boolean sortForDisplay) {
        return mSubtypeList.getAll(sortForDisplay);
    }

    /**
     * Get all of the enabled languages.
     * @return the enabled languages.
     */
    public Set<Locale> getEnabledLocales() {
        return mSubtypeList.getAllLocales();
    }

    /**
     * Get all of the enabled subtypes for language.
     * @param locale filter by Locale.
     * @return the enabled subtypes.
     */
    public Set<Subtype> getEnabledSubtypesForLocale(final String locale) {
        return mSubtypeList.getAllForLocale(locale);
    }

    /**
     * Check if there are multiple enabled subtypes.
     * @return whether there are multiple subtypes.
     */
    public boolean hasMultipleEnabledSubtypes() {
        return mSubtypeList.size() > 1;
    }

    /**
     * Enable a new subtype.
     * @param subtype the subtype to add.
     * @return whether the subtype was added.
     */
    public boolean addSubtype(final Subtype subtype) {
        return mSubtypeList.addSubtype(subtype);
    }

    /**
     * Disable a subtype.
     * @param subtype the subtype to remove.
     * @return whether the subtype was removed.
     */
    public boolean removeSubtype(final Subtype subtype) {
        return mSubtypeList.removeSubtype(subtype);
    }

    /**
     * Move the current subtype to the beginning of the list to allow the rest of the subtypes
     * to be cycled through before possibly switching to a separate input method.
     */
    public void resetSubtypeCycleOrder() {
        mSubtypeList.resetSubtypeCycleOrder();
    }

    /**
     * Set the current subtype to a specific subtype.
     * @param subtype the subtype to set as current.
     * @return whether the current subtype was set to the requested subtype.
     */
    public boolean setCurrentSubtype(final Subtype subtype) {
        return mSubtypeList.setCurrentSubtype(subtype);
    }

    /**
     * Set the current subtype to match a specified locale.
     * @param locale the locale to use.
     * @return whether the current subtype was set to the requested locale.
     */
    public boolean setCurrentSubtype(final Locale locale) {
        return mSubtypeList.setCurrentSubtype(locale);
    }

    /**
     * Switch to the next subtype of this IME or optionally to another IME if all of the subtypes of
     * this IME have already been iterated through.
     * @param token supplies the identifying token given to an input method when it was started,
     *             which allows it to perform this operation on itself.
     * @param onlyCurrentIme whether to only switch virtual subtypes or also switch to other input
     *                      methods.
     * @return whether the switch was successful.
     */
    public boolean switchToNextInputMethod(final IBinder token, final boolean onlyCurrentIme) {
        if (onlyCurrentIme) {
            if (!hasMultipleEnabledSubtypes()) {
                return false;
            }
            return mSubtypeList.switchToNextSubtype(true);
        }
        if (mSubtypeList.switchToNextSubtype(false)) {
            return true;
        }
        // switch to a different IME
        if (mImmService.switchToNextInputMethod(token, false)) {
            return true;
        }
        if (hasMultipleEnabledSubtypes()) {
            // the virtual subtype should have been reset to the first item to prepare for switching
            // back to this IME, but we skipped notifying the change because we expected to switch
            // to a different IME, but since that failed, we just need to notify the listener
            mSubtypeList.notifySubtypeChanged();
            return true;
        }
        return false;
    }

    /**
     * Get the subtype that is currently in use.
     * @return the current subtype.
     */
    public Subtype getCurrentSubtype() {
        return mSubtypeList.getCurrentSubtype();
    }

    /**
     * Check if the IME should offer ways to switch to a next input method (eg: a globe key).
     * @param binder supplies the identifying token given to an input method when it was started,
     *              which allows it to perform this operation on itself.
     * @return whether the IME should offer ways to switch to a next input method.
     */
    public boolean shouldOfferSwitchingToOtherInputMethods(final IBinder binder) {
        // Use the default value instead on Jelly Bean MR2 and previous where
        // {@link InputMethodManager#shouldOfferSwitchingToNextInputMethod} isn't yet available
        // and on KitKat where the API is still just a stub to return true always.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return false;
        }
        return mImmService.shouldOfferSwitchingToNextInputMethod(binder);
    }

    /**
     * Show a popup to pick the current subtype.
     * @param context the context for this application.
     * @param windowToken identifier for the window.
     * @param inputMethodService the input method service for this IME.
     * @return the dialog that was created.
     */
    public AlertDialog showSubtypePicker(final Context context, final IBinder windowToken,
                                         final InputMethodService inputMethodService) {
        if (windowToken == null) {
            return null;
        }
        final CharSequence title = context.getString(R.string.change_keyboard);

        final List<SubtypeInfo> subtypeInfoList = getEnabledSubtypeInfoOfAllImes(context);
        if (subtypeInfoList.size() < 2) {
            // if there aren't multiple options, there is no reason to show the picker
            return null;
        }

        final CharSequence[] items = new CharSequence[subtypeInfoList.size()];
        final Subtype currentSubtype = getCurrentSubtype();
        int currentSubtypeIndex = 0;
        int i = 0;
        for (final SubtypeInfo subtypeInfo : subtypeInfoList) {
            if (subtypeInfo.virtualSubtype != null
                    && subtypeInfo.virtualSubtype.equals(currentSubtype)) {
                currentSubtypeIndex = i;
            }

            final SpannableString itemTitle;
            final SpannableString itemSubtitle;
            if (!TextUtils.isEmpty(subtypeInfo.subtypeName)) {
                itemTitle = new SpannableString(subtypeInfo.subtypeName);
                itemSubtitle = new SpannableString("\n" + subtypeInfo.imeName);
            } else {
                itemTitle = new SpannableString(subtypeInfo.imeName);
                itemSubtitle = new SpannableString("");
            }
            itemTitle.setSpan(new RelativeSizeSpan(0.9f), 0,itemTitle.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            itemSubtitle.setSpan(new RelativeSizeSpan(0.85f), 0,itemSubtitle.length(),
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

            items[i++] = new SpannableStringBuilder().append(itemTitle).append(itemSubtitle);
        }
        final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface di, int position) {
                di.dismiss();
                int i = 0;
                for (final SubtypeInfo subtypeInfo : subtypeInfoList) {
                    if (i == position) {
                        if (subtypeInfo.virtualSubtype != null) {
                            setCurrentSubtype(subtypeInfo.virtualSubtype);
                        } else {
                            switchToTargetIme(subtypeInfo.imiId, subtypeInfo.systemSubtype,
                                    inputMethodService);
                        }
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

    /**
     * Get info for all of virtual subtypes of this IME and system subtypes of all other IMEs.
     * @param context the context for this application.
     * @return a list with info for all of the subtypes.
     */
    private List<SubtypeInfo> getEnabledSubtypeInfoOfAllImes(final Context context) {
        final List<SubtypeInfo> subtypeInfoList = new ArrayList<>();
        final PackageManager packageManager = context.getPackageManager();

        final Set<InputMethodInfo> imiList = new TreeSet<>(new Comparator<InputMethodInfo>() {
            @Override
            public int compare(InputMethodInfo a, InputMethodInfo b) {
                if (a.equals(b)) {
                    // ensure that this is consistent with equals
                    return 0;
                }
                final String labelA = a.loadLabel(packageManager).toString();
                final String labelB = b.loadLabel(packageManager).toString();
                final int result = labelA.compareToIgnoreCase(labelB);
                if (result != 0) {
                    return result;
                }
                // ensure that non-equal objects are distinguished to be consistent with
                // equals
                return a.hashCode() > b.hashCode() ? 1 : -1;
            }
        });
        imiList.addAll(mImmService.getEnabledInputMethodList());

        for (final InputMethodInfo imi : imiList) {
            final CharSequence imeName = imi.loadLabel(packageManager);
            final String imiId = imi.getId();
            final String packageName = imi.getPackageName();

            if (packageName.equals(context.getPackageName())) {
                for (final Subtype subtype : getEnabledSubtypes(true)) {
                    final SubtypeInfo subtypeInfo = new SubtypeInfo();
                    subtypeInfo.virtualSubtype = subtype;
                    subtypeInfo.subtypeName = subtype.getName();
                    subtypeInfo.imeName = imeName;
                    subtypeInfo.imiId = imiId;
                    subtypeInfoList.add(subtypeInfo);
                }
                continue;
            }

            final List<InputMethodSubtype> subtypes =
                    mImmService.getEnabledInputMethodSubtypeList(imi, true);
            // IMEs that have no subtypes should still be returned
            if (subtypes.isEmpty()) {
                final SubtypeInfo subtypeInfo = new SubtypeInfo();
                subtypeInfo.imeName = imeName;
                subtypeInfo.imiId = imiId;
                subtypeInfoList.add(subtypeInfo);
                continue;
            }

            final ApplicationInfo applicationInfo = imi.getServiceInfo().applicationInfo;
            for (final InputMethodSubtype subtype : subtypes) {
                if (subtype.isAuxiliary()) {
                    continue;
                }
                final SubtypeInfo subtypeInfo = new SubtypeInfo();
                subtypeInfo.systemSubtype = subtype;
                if (!subtype.overridesImplicitlyEnabledSubtype()) {
                    subtypeInfo.subtypeName = subtype.getDisplayName(context, packageName,
                            applicationInfo);
                }
                subtypeInfo.imeName = imeName;
                subtypeInfo.imiId = imiId;
                subtypeInfoList.add(subtypeInfo);
            }
        }

        return subtypeInfoList;
    }

    /**
     * Info for a virtual or system subtype.
     */
    private static class SubtypeInfo {
        public InputMethodSubtype systemSubtype;
        public Subtype virtualSubtype;
        public CharSequence subtypeName;
        public CharSequence imeName;
        public String imiId;
    }

    /**
     * Switch to a different input method.
     * @param imiId the ID for the input method to be switched to.
     * @param subtype the subtype for the input method to be switched to.
     * @param context the input method service for this IME.
     */
    private void switchToTargetIme(final String imiId, final InputMethodSubtype subtype,
                                   final InputMethodService context) {
        final IBinder token = context.getWindow().getWindow().getAttributes().token;
        if (token == null) {
            return;
        }
        final InputMethodManager imm = mImmService;
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                imm.setInputMethodAndSubtype(token, imiId, subtype);
            }
        });
    }
}
