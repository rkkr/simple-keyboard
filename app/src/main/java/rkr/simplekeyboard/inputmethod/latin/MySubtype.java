package rkr.simplekeyboard.inputmethod.latin;

import android.util.Log;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

public class MySubtype {
    private static final String TAG = MySubtype.class.getSimpleName();

    private final String mLocale;
    private final String mName;
    private final String mLayoutSet;
    private final String mLayoutName;

    public MySubtype(final String locale, final String name, final String layoutSet, final String layoutName) {
        mLocale = locale;
        mName = name;
        mLayoutSet = layoutSet;
        mLayoutName = layoutName;
    }

    public String getLocale() {
        return mLocale;
    }

    public Locale getLocaleObject() {
        return LocaleUtils.constructLocaleFromString(mLocale);
    }

    public String getName() {
        return mName;
    }

    public String getLayoutSet() {
        return mLayoutSet;
    }

    public String getLayoutDisplayName() {
        return mLayoutName;
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
