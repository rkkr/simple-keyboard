package rkr.simplekeyboard.inputmethod.latin;

import android.content.res.Resources;
import android.util.Log;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

public class MySubtype {
    private static final String TAG = MySubtype.class.getSimpleName();

    private static final int NO_RESOURCE = 0;

    private final String mLocale;
    private final String mLayoutSet;
    private final int mLayoutNameRes;
    private final String mLayoutNameStr;
    private final boolean mShowLayoutInName;
    private final Resources mResources;

    public MySubtype(final String locale, final String layoutSet, final int layoutNameRes, final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = layoutNameRes;
        mLayoutNameStr = null;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    public MySubtype(final String locale, final String layoutSet, final String layoutNameStr, final boolean showLayoutInName, final Resources resources) {
        mLocale = locale;
        mLayoutSet = layoutSet;
        mLayoutNameRes = NO_RESOURCE;
        mLayoutNameStr = layoutNameStr;
        mShowLayoutInName = showLayoutInName;
        mResources = resources;
    }

    public String getLocale() {
        return mLocale;
    }

    public Locale getLocaleObject() {
        return LocaleUtils.constructLocaleFromString(mLocale);
    }

    public String getName() {
        final String localeDisplayName =
                SubtypeLocaleUtils.getSubtypeLocaleDisplayNameInSystemLocale(mLocale);
        if (mShowLayoutInName) {
            if (mLayoutNameRes != NO_RESOURCE) {
                return mResources.getString(R.string.subtype_generic_layout, localeDisplayName, mResources.getString(mLayoutNameRes));
            }
            if (mLayoutNameStr != null) {
                return mResources.getString(R.string.subtype_generic_layout, localeDisplayName, mLayoutNameStr);
            }
        }
        return localeDisplayName;
    }

    public String getLayoutSet() {
        return mLayoutSet;
    }

    public String getLayoutDisplayName() {
        final String displayName;
        if (mLayoutNameRes != NO_RESOURCE) {
            displayName = mResources.getString(mLayoutNameRes);
        } else if (mLayoutNameStr != null) {
            displayName = mLayoutNameStr;
        } else {
            displayName = SubtypeLocaleUtils.getSubtypeLanguageDisplayNameInSystemLocale(mLocale);
        }
        return displayName;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof MySubtype)) {
            return false;
        }
        final MySubtype other = (MySubtype)o;
        return mLocale.equals(other.mLocale) && mLayoutSet.equals(other.mLayoutSet);
    }

    @Override
    public int hashCode() {
        int hashCode = 31 + mLocale.hashCode();
        hashCode = hashCode * 31 + mLayoutSet.hashCode();
        return hashCode;
    }

    @Override
    public String toString() {
        return "subtype " + mLocale + ":" + mLayoutSet;
    }

    /**
     * Get the full display name of the locale in its locale.
     * @return
     */
    public String getLocaleDisplayNameInLocale() {
        return SubtypeLocaleUtils.getSubtypeLocaleDisplayName(getLocale());
    }

    /**
     * Get the display name of the language in its locale.
     * @return
     */
    public String getLanguageDisplayNameInLocale() {
        return SubtypeLocaleUtils.getSubtypeLanguageDisplayName(getLocale());
    }
}
