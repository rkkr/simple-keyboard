package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import rkr.simplekeyboard.inputmethod.compat.PreferenceManagerCompat;
import rkr.simplekeyboard.inputmethod.latin.settings.Settings;
import rkr.simplekeyboard.inputmethod.latin.utils.AdditionalSubtypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeUtils;

public class VirtualSubtypeManager {
    private static final String TAG = VirtualSubtypeManager.class.getSimpleName();

    private final List<MySubtype> mSubtypes;
    private int mCurrentSubtypeIndex = 0;
    private final SharedPreferences mPrefs;

    public VirtualSubtypeManager(final Context context) {
        mPrefs = PreferenceManagerCompat.getDeviceSharedPreferences(context);

        final MySubtype[] subtypes = loadSubtypes(context);
        if (subtypes == null || subtypes.length < 1) {
            mSubtypes = SubtypeUtils.getDefaultSubtypes(context.getResources());
            mCurrentSubtypeIndex = 0;
        } else {
            mSubtypes = new ArrayList<>(Arrays.asList(subtypes));
            mCurrentSubtypeIndex = loadSubtypeIndex();
            if (mCurrentSubtypeIndex > mSubtypes.size()) {
                mCurrentSubtypeIndex = 0;
            }
        }
    }

    private MySubtype[] loadSubtypes(final Context context) {
        final String prefAdditionalSubtypes = Settings.readPrefAdditionalSubtypes(mPrefs);
        return AdditionalSubtypeUtils.createSubtypesFromPref(prefAdditionalSubtypes,
                context.getResources());
    }

    private int loadSubtypeIndex() {
        return Settings.readPrefCurrentSubtypeIndex(mPrefs);
    }

    private void saveSubtypePref(final boolean subtypesUpdated, final boolean indexUpdated) {
        //TODO: make atomic?
        if (indexUpdated) {
            Settings.writePrefCurrentSubtypeIndex(mPrefs, mCurrentSubtypeIndex);
        }
        if (subtypesUpdated) {
            final String prefSubtypes = AdditionalSubtypeUtils.createPrefSubtypes(mSubtypes);
            Settings.writePrefAdditionalSubtypes(mPrefs, prefSubtypes);
        }
    }

    public MySubtype getCurrentSubtype() {
        return mSubtypes.get(mCurrentSubtypeIndex);
    }

    public boolean hasMultiple() {
        return mSubtypes.size() > 1;
    }

    //TODO: maybe rename
    public HashSet<MySubtype> getAll() {
        return new HashSet<>(mSubtypes);
    }

    public boolean addSubtype(final MySubtype subtype) {
        if (mSubtypes.contains(subtype)) {
            return false;
        }
        if (!mSubtypes.add(subtype)) {
            return false;
        }
        saveSubtypePref(true, false);
        return true;
    }

    public boolean removeSubtype(final MySubtype subtype) {
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

    public boolean switchToNextSubtype(final boolean cycle) {
        if (mSubtypes.size() < 2) {
            return false;
        }
        mCurrentSubtypeIndex = (mCurrentSubtypeIndex + 1) % mSubtypes.size();
        saveSubtypePref(false, true);
        return mCurrentSubtypeIndex > 0 || cycle;
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

    public boolean setCurrentSubtype(final MySubtype subtype) {
        for (int i = 0; i < mSubtypes.size(); i++) {
            if (mSubtypes.get(i).equals(subtype)) {
                mCurrentSubtypeIndex = i;
                saveSubtypePref(false, true);
                return true;
            }
        }
        return false;
    }
}
