package rkr.simplekeyboard.inputmethod.latin;

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

    public String getName() {
        return mName;
    }

    public String getLayoutSet() {
        return mLayoutSet;
    }

    public String getLayoutDisplayName() {
        return mLayoutName;
    }
}
