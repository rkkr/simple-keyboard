package rkr.simplekeyboard.inputmethod.latin;

import java.util.Locale;

import rkr.simplekeyboard.inputmethod.latin.common.LocaleUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.SubtypeLocaleUtils;

public class MySubtype {
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

    // Get the RichInputMethodSubtype's full display name in its locale.
    //TODO: rename or remove
    public String getFullDisplayName() {
        return SubtypeLocaleUtils.getSubtypeLocaleDisplayName(getLocale());
    }

    // Get the RichInputMethodSubtype's middle display name in its locale.
    //TODO: rename or remove
    public String getMiddleDisplayName() {
        return SubtypeLocaleUtils.getSubtypeLanguageDisplayName(getLocale());
    }
}
