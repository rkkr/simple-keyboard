/*
 * Copyright (C) 2011 The Android Open Source Project
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

package rkr.simplekeyboard.inputmethod.keyboard;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.InputType;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.inputmethod.EditorInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.HashMap;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardBuilder;
import rkr.simplekeyboard.inputmethod.keyboard.internal.KeyboardParams;
import rkr.simplekeyboard.inputmethod.keyboard.internal.UniqueKeysCache;
import rkr.simplekeyboard.inputmethod.latin.RichInputMethodSubtype;
import rkr.simplekeyboard.inputmethod.latin.utils.InputTypeUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils;

/**
 * This class represents a set of keyboard layouts. Each of them represents a different keyboard
 * specific to a keyboard state, such as alphabet, symbols, and so on.  Layouts in the same
 * {@link KeyboardLayoutSet} are related to each other.
 * A {@link KeyboardLayoutSet} needs to be created for each
 * {@link android.view.inputmethod.EditorInfo}.
 */
public final class KeyboardLayoutSet {
    private static final String TAG = KeyboardLayoutSet.class.getSimpleName();
    private static final boolean DEBUG_CACHE = false;

    private static final String TAG_KEYBOARD_SET = "KeyboardLayoutSet";
    private static final String TAG_ELEMENT = "Element";

    private static final String KEYBOARD_LAYOUT_SET_RESOURCE_PREFIX = "keyboard_layout_set_";

    private final Context mContext;
    private final Params mParams;

    // How many layouts we forcibly keep in cache. This only includes ALPHABET (default) and
    // ALPHABET_AUTOMATIC_SHIFTED layouts - other layouts may stay in memory in the map of
    // soft-references, but we forcibly cache this many alphabetic/auto-shifted layouts.
    private static final int FORCIBLE_CACHE_SIZE = 4;
    // By construction of soft references, anything that is also referenced somewhere else
    // will stay in the cache. So we forcibly keep some references in an array to prevent
    // them from disappearing from sKeyboardCache.
    private static final Keyboard[] sForcibleKeyboardCache = new Keyboard[FORCIBLE_CACHE_SIZE];
    private static final HashMap<KeyboardId, SoftReference<Keyboard>> sKeyboardCache =
            new HashMap<>();
    private static final UniqueKeysCache sUniqueKeysCache = UniqueKeysCache.newInstance();

    @SuppressWarnings("serial")
    public static final class KeyboardLayoutSetException extends RuntimeException {
        public final KeyboardId mKeyboardId;

        public KeyboardLayoutSetException(final Throwable cause, final KeyboardId keyboardId) {
            super(cause);
            mKeyboardId = keyboardId;
        }
    }

    private static final class ElementParams {
        int mKeyboardXmlId;
        boolean mAllowRedundantMoreKeys;
        public ElementParams() {}
    }

    public static final class Params {
        String mKeyboardLayoutSetName;
        int mMode;
        // TODO: Use {@link InputAttributes} instead of these variables.
        EditorInfo mEditorInfo;
        boolean mLanguageSwitchKeyEnabled;
        RichInputMethodSubtype mSubtype;
        int mKeyboardWidth;
        int mKeyboardHeight;
        boolean mShowMoreKeys;
        boolean mShowNumberRow;
        // Sparse array of KeyboardLayoutSet element parameters indexed by element's id.
        final SparseArray<ElementParams> mKeyboardLayoutSetElementIdToParamsMap =
                new SparseArray<>();
    }

    public static void onSystemLocaleChanged() {
        clearKeyboardCache();
    }

    public static void onKeyboardThemeChanged() {
        clearKeyboardCache();
    }

    private static void clearKeyboardCache() {
        sKeyboardCache.clear();
        sUniqueKeysCache.clear();
    }

    KeyboardLayoutSet(final Context context, final Params params) {
        mContext = context;
        mParams = params;
    }

    public Keyboard getKeyboard(final int baseKeyboardLayoutSetElementId) {
        final int keyboardLayoutSetElementId;
        switch (mParams.mMode) {
        case KeyboardId.MODE_PHONE:
            if (baseKeyboardLayoutSetElementId == KeyboardId.ELEMENT_SYMBOLS) {
                keyboardLayoutSetElementId = KeyboardId.ELEMENT_PHONE_SYMBOLS;
            } else {
                keyboardLayoutSetElementId = KeyboardId.ELEMENT_PHONE;
            }
            break;
        case KeyboardId.MODE_NUMBER:
        case KeyboardId.MODE_DATE:
        case KeyboardId.MODE_TIME:
        case KeyboardId.MODE_DATETIME:
            keyboardLayoutSetElementId = KeyboardId.ELEMENT_NUMBER;
            break;
        default:
            keyboardLayoutSetElementId = baseKeyboardLayoutSetElementId;
            break;
        }

        ElementParams elementParams = mParams.mKeyboardLayoutSetElementIdToParamsMap.get(
                keyboardLayoutSetElementId);
        if (elementParams == null) {
            elementParams = mParams.mKeyboardLayoutSetElementIdToParamsMap.get(
                    KeyboardId.ELEMENT_ALPHABET);
        }
        // Note: The keyboard for each shift state, and mode are represented as an elementName
        // attribute in a keyboard_layout_set XML file.  Also each keyboard layout XML resource is
        // specified as an elementKeyboard attribute in the file.
        // The KeyboardId is an internal key for a Keyboard object.

        final KeyboardId id = new KeyboardId(keyboardLayoutSetElementId, mParams);
        return getKeyboard(elementParams, id);
    }

    private Keyboard getKeyboard(final ElementParams elementParams, final KeyboardId id) {
        final SoftReference<Keyboard> ref = sKeyboardCache.get(id);
        final Keyboard cachedKeyboard = (ref == null) ? null : ref.get();
        if (cachedKeyboard != null) {
            if (DEBUG_CACHE) {
                Log.d(TAG, "keyboard cache size=" + sKeyboardCache.size() + ": HIT  id=" + id);
            }
            return cachedKeyboard;
        }

        final KeyboardBuilder<KeyboardParams> builder =
                new KeyboardBuilder<>(mContext, new KeyboardParams(sUniqueKeysCache));
        sUniqueKeysCache.setEnabled(id.isAlphabetKeyboard());
        builder.setAllowRedundantMoreKes(elementParams.mAllowRedundantMoreKeys);
        final int keyboardXmlId = elementParams.mKeyboardXmlId;
        builder.load(keyboardXmlId, id);
        final Keyboard keyboard = builder.build();
        sKeyboardCache.put(id, new SoftReference<>(keyboard));
        if ((id.mElementId == KeyboardId.ELEMENT_ALPHABET
                || id.mElementId == KeyboardId.ELEMENT_ALPHABET_AUTOMATIC_SHIFTED)) {
            // We only forcibly cache the primary, "ALPHABET", layouts.
            for (int i = sForcibleKeyboardCache.length - 1; i >= 1; --i) {
                sForcibleKeyboardCache[i] = sForcibleKeyboardCache[i - 1];
            }
            sForcibleKeyboardCache[0] = keyboard;
            if (DEBUG_CACHE) {
                Log.d(TAG, "forcing caching of keyboard with id=" + id);
            }
        }
        if (DEBUG_CACHE) {
            Log.d(TAG, "keyboard cache size=" + sKeyboardCache.size() + ": "
                    + ((ref == null) ? "LOAD" : "GCed") + " id=" + id);
        }
        return keyboard;
    }

    public static final class Builder {
        private final Context mContext;
        private final Resources mResources;

        private final Params mParams = new Params();

        private static final EditorInfo EMPTY_EDITOR_INFO = new EditorInfo();

        public Builder(final Context context, final EditorInfo ei) {
            mContext = context;
            mResources = context.getResources();
            final Params params = mParams;

            final EditorInfo editorInfo = (ei != null) ? ei : EMPTY_EDITOR_INFO;
            params.mMode = getKeyboardMode(editorInfo);
            // TODO: Consolidate those with {@link InputAttributes}.
            params.mEditorInfo = editorInfo;
        }

        public Builder setKeyboardGeometry(final int keyboardWidth, final int keyboardHeight) {
            mParams.mKeyboardWidth = keyboardWidth;
            mParams.mKeyboardHeight = keyboardHeight;
            return this;
        }

        public Builder setSubtype(final RichInputMethodSubtype subtype) {
            // TODO: Consolidate with {@link InputAttributes}.
            mParams.mSubtype = subtype;
            mParams.mKeyboardLayoutSetName = KEYBOARD_LAYOUT_SET_RESOURCE_PREFIX
                    + subtype.getKeyboardLayoutSetName();
            return this;
        }

        public Builder setLanguageSwitchKeyEnabled(final boolean enabled) {
            mParams.mLanguageSwitchKeyEnabled = enabled;
            return this;
        }

        public Builder setShowSpecialChars(final boolean enabled) {
            mParams.mShowMoreKeys = enabled;
            return this;
        }

        public Builder setShowNumberRow(final boolean enabled) {
            mParams.mShowNumberRow = enabled;
            return this;
        }

        public KeyboardLayoutSet build() {
            if (mParams.mSubtype == null)
                throw new RuntimeException("KeyboardLayoutSet subtype is not specified");
            final int xmlId = getXmlId(mResources, mParams.mKeyboardLayoutSetName);
            try {
                parseKeyboardLayoutSet(mResources, xmlId);
            } catch (final IOException | XmlPullParserException e) {
                throw new RuntimeException(e.getMessage() + " in " + mParams.mKeyboardLayoutSetName,
                        e);
            }
            return new KeyboardLayoutSet(mContext, mParams);
        }

        private static int getXmlId(final Resources resources, final String keyboardLayoutSetName) {
            final String packageName = resources.getResourcePackageName(
                    R.xml.keyboard_layout_set_qwerty);
            return resources.getIdentifier(keyboardLayoutSetName, "xml", packageName);
        }

        private void parseKeyboardLayoutSet(final Resources res, final int resId)
                throws XmlPullParserException, IOException {
            final XmlResourceParser parser = res.getXml(resId);
            try {
                while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                    final int event = parser.next();
                    if (event == XmlPullParser.START_TAG) {
                        final String tag = parser.getName();
                        if (TAG_KEYBOARD_SET.equals(tag)) {
                            parseKeyboardLayoutSetContent(parser);
                        } else {
                            throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD_SET);
                        }
                    }
                }
            } finally {
                parser.close();
            }
        }

        private void parseKeyboardLayoutSetContent(final XmlPullParser parser)
                throws XmlPullParserException, IOException {
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                final int event = parser.next();
                if (event == XmlPullParser.START_TAG) {
                    final String tag = parser.getName();
                    if (TAG_ELEMENT.equals(tag)) {
                        parseKeyboardLayoutSetElement(parser);
                    } else {
                        throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD_SET);
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    final String tag = parser.getName();
                    if (TAG_KEYBOARD_SET.equals(tag)) {
                        break;
                    }
                    throw new XmlParseUtils.IllegalEndTag(parser, tag, TAG_KEYBOARD_SET);
                }
            }
        }

        private void parseKeyboardLayoutSetElement(final XmlPullParser parser)
                throws XmlPullParserException, IOException {
            final TypedArray a = mResources.obtainAttributes(Xml.asAttributeSet(parser),
                    R.styleable.KeyboardLayoutSet_Element);
            try {
                XmlParseUtils.checkAttributeExists(a,
                        R.styleable.KeyboardLayoutSet_Element_elementName, "elementName",
                        TAG_ELEMENT, parser);
                XmlParseUtils.checkAttributeExists(a,
                        R.styleable.KeyboardLayoutSet_Element_elementKeyboard, "elementKeyboard",
                        TAG_ELEMENT, parser);
                XmlParseUtils.checkEndTag(TAG_ELEMENT, parser);

                final ElementParams elementParams = new ElementParams();
                final int elementName = a.getInt(
                        R.styleable.KeyboardLayoutSet_Element_elementName, 0);
                elementParams.mKeyboardXmlId = a.getResourceId(
                        R.styleable.KeyboardLayoutSet_Element_elementKeyboard, 0);
                elementParams.mAllowRedundantMoreKeys = a.getBoolean(
                        R.styleable.KeyboardLayoutSet_Element_allowRedundantMoreKeys, true);
                mParams.mKeyboardLayoutSetElementIdToParamsMap.put(elementName, elementParams);
            } finally {
                a.recycle();
            }
        }

        private static int getKeyboardMode(final EditorInfo editorInfo) {
            final int inputType = editorInfo.inputType;
            final int variation = inputType & InputType.TYPE_MASK_VARIATION;

            switch (inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
                return KeyboardId.MODE_NUMBER;
            case InputType.TYPE_CLASS_DATETIME:
                switch (variation) {
                case InputType.TYPE_DATETIME_VARIATION_DATE:
                    return KeyboardId.MODE_DATE;
                case InputType.TYPE_DATETIME_VARIATION_TIME:
                    return KeyboardId.MODE_TIME;
                default: // InputType.TYPE_DATETIME_VARIATION_NORMAL
                    return KeyboardId.MODE_DATETIME;
                }
            case InputType.TYPE_CLASS_PHONE:
                return KeyboardId.MODE_PHONE;
            case InputType.TYPE_CLASS_TEXT:
                if (InputTypeUtils.isEmailVariation(variation)) {
                    return KeyboardId.MODE_EMAIL;
                } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                    return KeyboardId.MODE_URL;
                } else if (variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE) {
                    return KeyboardId.MODE_IM;
                } else if (variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    return KeyboardId.MODE_TEXT;
                } else {
                    return KeyboardId.MODE_TEXT;
                }
            default:
                return KeyboardId.MODE_TEXT;
            }
        }
    }
}
