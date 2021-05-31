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

package rkr.simplekeyboard.inputmethod.keyboard.internal;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import rkr.simplekeyboard.inputmethod.R;
import rkr.simplekeyboard.inputmethod.keyboard.Key;
import rkr.simplekeyboard.inputmethod.keyboard.Keyboard;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardId;
import rkr.simplekeyboard.inputmethod.keyboard.KeyboardTheme;
import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.ResourceUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils;
import rkr.simplekeyboard.inputmethod.latin.utils.XmlParseUtils.ParseException;

/**
 * Keyboard Building helper.
 *
 * This class parses Keyboard XML file and eventually build a Keyboard.
 * The Keyboard XML file looks like:
 * <pre>
 *   &lt;!-- xml/keyboard.xml --&gt;
 *   &lt;Keyboard keyboard_attributes*&gt;
 *     &lt;!-- Keyboard Content --&gt;
 *     &lt;Row row_attributes*&gt;
 *       &lt;!-- Row Content --&gt;
 *       &lt;Key key_attributes* /&gt;
 *       &lt;Spacer horizontalGap="32.0dp" /&gt;
 *       &lt;include keyboardLayout="@xml/other_keys"&gt;
 *       ...
 *     &lt;/Row&gt;
 *     &lt;include keyboardLayout="@xml/other_rows"&gt;
 *     ...
 *   &lt;/Keyboard&gt;
 * </pre>
 * The XML file which is included in other file must have &lt;merge&gt; as root element,
 * such as:
 * <pre>
 *   &lt;!-- xml/other_keys.xml --&gt;
 *   &lt;merge&gt;
 *     &lt;Key key_attributes* /&gt;
 *     ...
 *   &lt;/merge&gt;
 * </pre>
 * and
 * <pre>
 *   &lt;!-- xml/other_rows.xml --&gt;
 *   &lt;merge&gt;
 *     &lt;Row row_attributes*&gt;
 *       &lt;Key key_attributes* /&gt;
 *     &lt;/Row&gt;
 *     ...
 *   &lt;/merge&gt;
 * </pre>
 * You can also use switch-case-default tags to select Rows and Keys.
 * <pre>
 *   &lt;switch&gt;
 *     &lt;case case_attribute*&gt;
 *       &lt;!-- Any valid tags at switch position --&gt;
 *     &lt;/case&gt;
 *     ...
 *     &lt;default&gt;
 *       &lt;!-- Any valid tags at switch position --&gt;
 *     &lt;/default&gt;
 *   &lt;/switch&gt;
 * </pre>
 * You can declare Key style and specify styles within Key tags.
 * <pre>
 *     &lt;switch&gt;
 *       &lt;case mode="email"&gt;
 *         &lt;key-style styleName="f1-key" parentStyle="modifier-key"
 *           keyLabel=".com"
 *         /&gt;
 *       &lt;/case&gt;
 *       &lt;case mode="url"&gt;
 *         &lt;key-style styleName="f1-key" parentStyle="modifier-key"
 *           keyLabel="http://"
 *         /&gt;
 *       &lt;/case&gt;
 *     &lt;/switch&gt;
 *     ...
 *     &lt;Key keyStyle="shift-key" ... /&gt;
 * </pre>
 */

// TODO: Write unit tests for this class.
public class KeyboardBuilder<KP extends KeyboardParams> {
    private static final String BUILDER_TAG = "Keyboard.Builder";
    private static final boolean DEBUG = false;

    // Keyboard XML Tags
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private static final String TAG_KEY = "Key";
    private static final String TAG_SPACER = "Spacer";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_MERGE = "merge";
    private static final String TAG_SWITCH = "switch";
    private static final String TAG_CASE = "case";
    private static final String TAG_DEFAULT = "default";
    public static final String TAG_KEY_STYLE = "key-style";

    private static final int DEFAULT_KEYBOARD_COLUMNS = 10;
    private static final int DEFAULT_KEYBOARD_ROWS = 4;

    protected final KP mParams;
    protected final Context mContext;
    protected final Resources mResources;

    private float mCurrentY = 0;
    private KeyboardRow mCurrentRow = null;
    private Key mPreviousKeyInRow = null;
    private boolean mKeyboardDefined = false;

    public KeyboardBuilder(final Context context, final KP params) {
        mContext = context;
        final Resources res = context.getResources();
        mResources = res;

        mParams = params;

        params.mGridWidth = res.getInteger(R.integer.config_keyboard_grid_width);
        params.mGridHeight = res.getInteger(R.integer.config_keyboard_grid_height);
    }

    public void setAllowRedundantMoreKes(final boolean enabled) {
        mParams.mAllowRedundantMoreKeys = enabled;
    }

    public KeyboardBuilder<KP> load(final int xmlId, final KeyboardId id) {
        mParams.mId = id;
        final XmlResourceParser parser = mResources.getXml(xmlId);
        try {
            parseKeyboard(parser, false);
            if (!mKeyboardDefined) {
                throw new XmlParseUtils.ParseException("No " + TAG_KEYBOARD + " tag was found");
            }
        } catch (XmlPullParserException e) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e);
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IOException e) {
            Log.w(BUILDER_TAG, "keyboard XML parse error", e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            parser.close();
        }
        return this;
    }

    public Keyboard build() {
        return new Keyboard(mParams);
    }

    private int mIndent;
    private static final String SPACES = "                                             ";

    private static String spaces(final int count) {
        return (count < SPACES.length()) ? SPACES.substring(0, count) : SPACES;
    }

    private void startTag(final String format, final Object ... args) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, args));
    }

    private void endTag(final String format, final Object ... args) {
        Log.d(BUILDER_TAG, String.format(spaces(mIndent-- * 2) + format, args));
    }

    private void startEndTag(final String format, final Object ... args) {
        Log.d(BUILDER_TAG, String.format(spaces(++mIndent * 2) + format, args));
        mIndent--;
    }

    private void parseKeyboard(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                final String tag = parser.getName();
                if (TAG_KEYBOARD.equals(tag)) {
                    if (DEBUG) startTag("<%s> %s%s", TAG_KEYBOARD, mParams.mId,
                            skip ? " skipped" : "");
                    if (!skip) {
                        if (mKeyboardDefined) {
                            throw new XmlParseUtils.ParseException("Only one " + TAG_KEYBOARD
                                    + " tag can be defined", parser);
                        }
                        mKeyboardDefined = true;
                        parseKeyboardAttributes(parser);
                        startKeyboard();
                    }
                    parseKeyboardContent(parser, skip);
                } else if (TAG_SWITCH.equals(tag)) {
                    parseSwitchKeyboard(parser, skip);
                } else {
                    throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_KEYBOARD);
                }
            } else if (event == XmlPullParser.END_TAG) {
                final String tag = parser.getName();
                if (DEBUG) endTag("</%s>", tag);
                if (TAG_CASE.equals(tag) || TAG_DEFAULT.equals(tag)) {
                    return;
                }
                throw new XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW);
            }
        }
    }

    private void parseKeyboardAttributes(final XmlPullParser parser) {
        final AttributeSet attr = Xml.asAttributeSet(parser);
        final TypedArray keyboardAttr = mContext.obtainStyledAttributes(
                attr, R.styleable.Keyboard, R.attr.keyboardStyle, R.style.Keyboard);
        final TypedArray keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key);
        try {
            final KeyboardParams params = mParams;
            final int height = params.mId.mHeight;
            final int width = params.mId.mWidth;
            // The bonus height isn't used to determine the other dimensions (gap/padding) to allow
            // those to stay consistent between layouts with and without the bonus height added.
            final int bonusHeight = (int)keyboardAttr.getFraction(R.styleable.Keyboard_bonusHeight,
                    height, height, 0);
            params.mOccupiedHeight = height + bonusHeight;
            params.mOccupiedWidth = width;
            params.mTopPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                    R.styleable.Keyboard_keyboardTopPadding, height, 0);
            params.mBottomPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                    R.styleable.Keyboard_keyboardBottomPadding, height, 0);
            params.mLeftPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                    R.styleable.Keyboard_keyboardLeftPadding, width, 0);
            params.mRightPadding = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                    R.styleable.Keyboard_keyboardRightPadding, width, 0);

            params.mHorizontalGap = keyboardAttr.getFraction(
                    R.styleable.Keyboard_horizontalGap, width, width, 0);
            final float baseWidth = params.mOccupiedWidth - params.mLeftPadding
                    - params.mRightPadding + params.mHorizontalGap;
            params.mBaseWidth = baseWidth;
            params.mDefaultKeyPaddedWidth = ResourceUtils.getFraction(keyAttr,
                    R.styleable.Keyboard_Key_keyWidth, baseWidth,
                    baseWidth / DEFAULT_KEYBOARD_COLUMNS);
            // TODO: Fix keyboard geometry calculation clearer. Historically vertical gap between
            // rows are determined based on the entire keyboard height including top and bottom
            // paddings.
            params.mVerticalGap = keyboardAttr.getFraction(
                    R.styleable.Keyboard_verticalGap, height, height, 0);
            final float baseHeight = params.mOccupiedHeight - params.mTopPadding
                    - params.mBottomPadding + params.mVerticalGap;
            params.mBaseHeight = baseHeight;
            params.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(keyboardAttr,
                    R.styleable.Keyboard_rowHeight, baseHeight, baseHeight / DEFAULT_KEYBOARD_ROWS);

            params.mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr);

            params.mMoreKeysTemplate = keyboardAttr.getResourceId(
                    R.styleable.Keyboard_moreKeysTemplate, 0);
            params.mMaxMoreKeysKeyboardColumn = keyAttr.getInt(
                    R.styleable.Keyboard_Key_maxMoreKeysColumn, 5);

            params.mIconsSet.loadIcons(keyboardAttr);
            params.mTextsSet.setLocale(params.mId.getLocale(), mContext);
        } finally {
            keyAttr.recycle();
            keyboardAttr.recycle();
        }
    }

    private void parseKeyboardContent(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                final String tag = parser.getName();
                if (TAG_ROW.equals(tag)) {
                    final KeyboardRow row = parseRowAttributes(parser);
                    if (DEBUG) startTag("<%s>%s", TAG_ROW, skip ? " skipped" : "");
                    if (!skip) {
                        startRow(row);
                    }
                    parseRowContent(parser, row, skip);
                } else if (TAG_INCLUDE.equals(tag)) {
                    parseIncludeKeyboardContent(parser, skip);
                } else if (TAG_SWITCH.equals(tag)) {
                    parseSwitchKeyboardContent(parser, skip);
                } else if (TAG_KEY_STYLE.equals(tag)) {
                    parseKeyStyle(parser, skip);
                } else {
                    throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_ROW);
                }
            } else if (event == XmlPullParser.END_TAG) {
                final String tag = parser.getName();
                if (DEBUG) endTag("</%s>", tag);
                if (TAG_KEYBOARD.equals(tag)) {
                    endKeyboard();
                    return;
                }
                if (TAG_CASE.equals(tag) || TAG_DEFAULT.equals(tag) || TAG_MERGE.equals(tag)) {
                    return;
                }
                throw new XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW);
            }
        }
    }

    private KeyboardRow parseRowAttributes(final XmlPullParser parser)
            throws XmlPullParserException {
        final AttributeSet attr = Xml.asAttributeSet(parser);
        final TypedArray keyboardAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard);
        try {
            if (keyboardAttr.hasValue(R.styleable.Keyboard_horizontalGap)) {
                throw new XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "horizontalGap");
            }
            if (keyboardAttr.hasValue(R.styleable.Keyboard_verticalGap)) {
                throw new XmlParseUtils.IllegalAttribute(parser, TAG_ROW, "verticalGap");
            }
            return new KeyboardRow(mResources, mParams, parser, mCurrentY);
        } finally {
            keyboardAttr.recycle();
        }
    }

    private void parseRowContent(final XmlPullParser parser, final KeyboardRow row,
            final boolean skip) throws XmlPullParserException, IOException {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                final String tag = parser.getName();
                if (TAG_KEY.equals(tag)) {
                    parseKey(parser, row, skip);
                } else if (TAG_SPACER.equals(tag)) {
                    parseSpacer(parser, row, skip);
                } else if (TAG_INCLUDE.equals(tag)) {
                    parseIncludeRowContent(parser, row, skip);
                } else if (TAG_SWITCH.equals(tag)) {
                    parseSwitchRowContent(parser, row, skip);
                } else if (TAG_KEY_STYLE.equals(tag)) {
                    parseKeyStyle(parser, skip);
                } else {
                    throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_ROW);
                }
            } else if (event == XmlPullParser.END_TAG) {
                final String tag = parser.getName();
                if (DEBUG) endTag("</%s>", tag);
                if (TAG_ROW.equals(tag)) {
                    if (!skip) {
                        endRow(row);
                    }
                    return;
                }
                if (TAG_CASE.equals(tag) || TAG_DEFAULT.equals(tag) || TAG_MERGE.equals(tag)) {
                    return;
                }
                throw new XmlParseUtils.IllegalEndTag(parser, tag, TAG_ROW);
            }
        }
    }

    private void parseKey(final XmlPullParser parser, final KeyboardRow row, final boolean skip)
            throws XmlPullParserException, IOException {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_KEY, parser);
            if (DEBUG) startEndTag("<%s /> skipped", TAG_KEY);
            return;
        }
        final TypedArray keyAttr = mResources.obtainAttributes(
                Xml.asAttributeSet(parser), R.styleable.Keyboard_Key);
        final KeyStyle keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser);
        final String keySpec = keyStyle.getString(keyAttr, R.styleable.Keyboard_Key_keySpec);
        if (TextUtils.isEmpty(keySpec)) {
            throw new ParseException("Empty keySpec", parser);
        }
        final Key key = new Key(keySpec, keyAttr, keyStyle, mParams, row);
        keyAttr.recycle();
        if (DEBUG) {
            startEndTag("<%s %s moreKeys=%s />", TAG_KEY, key, Arrays.toString(key.getMoreKeys()));
        }
        XmlParseUtils.checkEndTag(TAG_KEY, parser);
        endKey(key, row);
    }

    private void parseSpacer(final XmlPullParser parser, final KeyboardRow row, final boolean skip)
            throws XmlPullParserException, IOException {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_SPACER, parser);
            if (DEBUG) startEndTag("<%s /> skipped", TAG_SPACER);
            return;
        }
        final TypedArray keyAttr = mResources.obtainAttributes(
                Xml.asAttributeSet(parser), R.styleable.Keyboard_Key);
        final KeyStyle keyStyle = mParams.mKeyStyles.getKeyStyle(keyAttr, parser);
        final Key spacer = new Key.Spacer(keyAttr, keyStyle, mParams, row);
        keyAttr.recycle();
        if (DEBUG) startEndTag("<%s />", TAG_SPACER);
        XmlParseUtils.checkEndTag(TAG_SPACER, parser);
        endKey(spacer, row);
    }

    private void parseIncludeKeyboardContent(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        parseIncludeInternal(parser, null, skip);
    }

    private void parseIncludeRowContent(final XmlPullParser parser, final KeyboardRow row,
            final boolean skip) throws XmlPullParserException, IOException {
        parseIncludeInternal(parser, row, skip);
    }

    private void parseIncludeInternal(final XmlPullParser parser, final KeyboardRow row,
            final boolean skip) throws XmlPullParserException, IOException {
        if (skip) {
            XmlParseUtils.checkEndTag(TAG_INCLUDE, parser);
            if (DEBUG) startEndTag("</%s> skipped", TAG_INCLUDE);
            return;
        }
        final AttributeSet attr = Xml.asAttributeSet(parser);
        final TypedArray keyboardAttr = mResources.obtainAttributes(
                attr, R.styleable.Keyboard_Include);
        final TypedArray includeAttr = mResources.obtainAttributes(
                attr, R.styleable.Keyboard);
        mParams.mDefaultRowHeight = ResourceUtils.getDimensionOrFraction(includeAttr,
                R.styleable.Keyboard_rowHeight, mParams.mBaseHeight, mParams.mDefaultRowHeight);

        final TypedArray keyAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key);
        int keyboardLayout = 0;
        try {
            XmlParseUtils.checkAttributeExists(
                    keyboardAttr, R.styleable.Keyboard_Include_keyboardLayout, "keyboardLayout",
                    TAG_INCLUDE, parser);
            keyboardLayout = keyboardAttr.getResourceId(
                    R.styleable.Keyboard_Include_keyboardLayout, 0);
            if (row != null) {
                // Override current x coordinate.
                row.updateXPos(keyAttr);
                // Push current Row attributes and update with new attributes.
                row.pushRowAttributes(keyAttr);
            }
        } finally {
            keyboardAttr.recycle();
            keyAttr.recycle();
            includeAttr.recycle();
        }

        XmlParseUtils.checkEndTag(TAG_INCLUDE, parser);
        if (DEBUG) {
            startEndTag("<%s keyboardLayout=%s />",TAG_INCLUDE,
                    mResources.getResourceEntryName(keyboardLayout));
        }
        final XmlResourceParser parserForInclude = mResources.getXml(keyboardLayout);
        try {
            parseMerge(parserForInclude, row, skip);
        } finally {
            if (row != null) {
                // Restore Row attributes.
                row.popRowAttributes();
            }
            parserForInclude.close();
        }
    }

    private void parseMerge(final XmlPullParser parser, final KeyboardRow row, final boolean skip)
            throws XmlPullParserException, IOException {
        if (DEBUG) startTag("<%s>", TAG_MERGE);
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                final String tag = parser.getName();
                if (TAG_MERGE.equals(tag)) {
                    if (row == null) {
                        parseKeyboardContent(parser, skip);
                    } else {
                        parseRowContent(parser, row, skip);
                    }
                    return;
                }
                throw new XmlParseUtils.ParseException(
                        "Included keyboard layout must have <merge> root element", parser);
            }
        }
    }

    private void parseSwitchKeyboard(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        parseSwitchInternal(parser, true, null, skip);
    }

    private void parseSwitchKeyboardContent(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        parseSwitchInternal(parser, false, null, skip);
    }

    private void parseSwitchRowContent(final XmlPullParser parser, final KeyboardRow row,
            final boolean skip) throws XmlPullParserException, IOException {
        parseSwitchInternal(parser, false, row, skip);
    }

    private void parseSwitchInternal(final XmlPullParser parser, final boolean parseKeyboard,
            final KeyboardRow row, final boolean skip) throws XmlPullParserException, IOException {
        if (DEBUG) startTag("<%s> %s", TAG_SWITCH, mParams.mId);
        boolean selected = false;
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
            final int event = parser.next();
            if (event == XmlPullParser.START_TAG) {
                final String tag = parser.getName();
                if (TAG_CASE.equals(tag)) {
                    selected |= parseCase(parser, parseKeyboard, row, selected || skip);
                } else if (TAG_DEFAULT.equals(tag)) {
                    selected |= parseDefault(parser, parseKeyboard, row, selected || skip);
                } else {
                    throw new XmlParseUtils.IllegalStartTag(parser, tag, TAG_SWITCH);
                }
            } else if (event == XmlPullParser.END_TAG) {
                final String tag = parser.getName();
                if (TAG_SWITCH.equals(tag)) {
                    if (DEBUG) endTag("</%s>", TAG_SWITCH);
                    return;
                }
                throw new XmlParseUtils.IllegalEndTag(parser, tag, TAG_SWITCH);
            }
        }
    }

    private boolean parseCase(final XmlPullParser parser, final boolean parseKeyboard,
            final KeyboardRow row, final boolean skip) throws XmlPullParserException, IOException {
        final boolean selected = parseCaseCondition(parser);
        if (parseKeyboard) {
            // Processing Keyboard root.
            parseKeyboard(parser, !selected || skip);
        } else if (row == null) {
            // Processing Rows.
            parseKeyboardContent(parser, !selected || skip);
        } else {
            // Processing Keys.
            parseRowContent(parser, row, !selected || skip);
        }
        return selected;
    }

    private boolean parseCaseCondition(final XmlPullParser parser) {
        final KeyboardId id = mParams.mId;
        if (id == null) {
            return true;
        }
        final AttributeSet attr = Xml.asAttributeSet(parser);
        final TypedArray caseAttr = mResources.obtainAttributes(attr, R.styleable.Keyboard_Case);
        if (DEBUG) startTag("<%s>", TAG_CASE);
        try {
            final boolean keyboardLayoutSetMatched = matchString(caseAttr,
                    R.styleable.Keyboard_Case_keyboardLayoutSet,
                    id.mSubtype.getKeyboardLayoutSet());
            final boolean keyboardLayoutSetElementMatched = matchTypedValue(caseAttr,
                    R.styleable.Keyboard_Case_keyboardLayoutSetElement, id.mElementId,
                    KeyboardId.elementIdToName(id.mElementId));
            final boolean keyboardThemeMatched = matchTypedValue(caseAttr,
                    R.styleable.Keyboard_Case_keyboardTheme, id.mThemeId,
                    KeyboardTheme.getKeyboardThemeName(id.mThemeId));
            final boolean modeMatched = matchTypedValue(caseAttr,
                    R.styleable.Keyboard_Case_mode, id.mMode, KeyboardId.modeName(id.mMode));
            final boolean navigateNextMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_navigateNext, id.navigateNext());
            final boolean navigatePreviousMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_navigatePrevious, id.navigatePrevious());
            final boolean passwordInputMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_passwordInput, id.passwordInput());
            final boolean languageSwitchKeyEnabledMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_languageSwitchKeyEnabled,
                    id.mLanguageSwitchKeyEnabled);
            final boolean isMultiLineMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_isMultiLine, id.isMultiLine());
            final boolean imeActionMatched = matchInteger(caseAttr,
                    R.styleable.Keyboard_Case_imeAction, id.imeAction());
            final boolean isIconDefinedMatched = isIconDefined(caseAttr,
                    R.styleable.Keyboard_Case_isIconDefined, mParams.mIconsSet);
            final Locale locale = id.getLocale();
            final boolean localeCodeMatched = matchLocaleCodes(caseAttr, locale);
            final boolean languageCodeMatched = matchLanguageCodes(caseAttr, locale);
            final boolean countryCodeMatched = matchCountryCodes(caseAttr, locale);
            final boolean showMoreKeysMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_showExtraChars, id.mShowMoreKeys);
            final boolean showNumberRowMatched = matchBoolean(caseAttr,
                    R.styleable.Keyboard_Case_showNumberRow, id.mShowNumberRow);
            final boolean selected = keyboardLayoutSetMatched && keyboardLayoutSetElementMatched
                    && keyboardThemeMatched && modeMatched && navigateNextMatched
                    && navigatePreviousMatched && passwordInputMatched
                    && languageSwitchKeyEnabledMatched
                    && isMultiLineMatched && imeActionMatched && isIconDefinedMatched
                    && localeCodeMatched && languageCodeMatched && countryCodeMatched
                    && showMoreKeysMatched && showNumberRowMatched;

            return selected;
        } finally {
            caseAttr.recycle();
        }
    }

    private static boolean matchLocaleCodes(TypedArray caseAttr, final Locale locale) {
        return matchString(caseAttr, R.styleable.Keyboard_Case_localeCode, locale.toString());
    }

    private static boolean matchLanguageCodes(TypedArray caseAttr, Locale locale) {
        return matchString(caseAttr, R.styleable.Keyboard_Case_languageCode, locale.getLanguage());
    }

    private static boolean matchCountryCodes(TypedArray caseAttr, Locale locale) {
        return matchString(caseAttr, R.styleable.Keyboard_Case_countryCode, locale.getCountry());
    }

    private static boolean matchInteger(final TypedArray a, final int index, final int value) {
        // If <case> does not have "index" attribute, that means this <case> is wild-card for
        // the attribute.
        return !a.hasValue(index) || a.getInt(index, 0) == value;
    }

    private static boolean matchBoolean(final TypedArray a, final int index, final boolean value) {
        // If <case> does not have "index" attribute, that means this <case> is wild-card for
        // the attribute.
        return !a.hasValue(index) || a.getBoolean(index, false) == value;
    }

    private static boolean matchString(final TypedArray a, final int index, final String value) {
        // If <case> does not have "index" attribute, that means this <case> is wild-card for
        // the attribute.
        return !a.hasValue(index)
                || StringUtils.containsInArray(value, a.getString(index).split("\\|"));
    }

    private static boolean matchTypedValue(final TypedArray a, final int index, final int intValue,
            final String strValue) {
        // If <case> does not have "index" attribute, that means this <case> is wild-card for
        // the attribute.
        final TypedValue v = a.peekValue(index);
        if (v == null) {
            return true;
        }
        if (ResourceUtils.isIntegerValue(v)) {
            return intValue == a.getInt(index, 0);
        }
        if (ResourceUtils.isStringValue(v)) {
            return StringUtils.containsInArray(strValue, a.getString(index).split("\\|"));
        }
        return false;
    }

    private static boolean isIconDefined(final TypedArray a, final int index,
            final KeyboardIconsSet iconsSet) {
        if (!a.hasValue(index)) {
            return true;
        }
        final String iconName = a.getString(index);
        final int iconId = KeyboardIconsSet.getIconId(iconName);
        return iconsSet.getIconDrawable(iconId) != null;
    }

    private boolean parseDefault(final XmlPullParser parser, final boolean parseKeyboard,
            final KeyboardRow row, final boolean skip) throws XmlPullParserException, IOException {
        if (DEBUG) startTag("<%s>", TAG_DEFAULT);
        if (parseKeyboard) {
            parseKeyboard(parser, skip);
        } else if (row == null) {
            parseKeyboardContent(parser, skip);
        } else {
            parseRowContent(parser, row, skip);
        }
        return true;
    }

    private void parseKeyStyle(final XmlPullParser parser, final boolean skip)
            throws XmlPullParserException, IOException {
        final AttributeSet attr = Xml.asAttributeSet(parser);
        final TypedArray keyStyleAttr = mResources.obtainAttributes(
                attr, R.styleable.Keyboard_KeyStyle);
        final TypedArray keyAttrs = mResources.obtainAttributes(attr, R.styleable.Keyboard_Key);
        try {
            if (!keyStyleAttr.hasValue(R.styleable.Keyboard_KeyStyle_styleName)) {
                throw new XmlParseUtils.ParseException("<" + TAG_KEY_STYLE
                        + "/> needs styleName attribute", parser);
            }
            if (DEBUG) {
                startEndTag("<%s styleName=%s />%s", TAG_KEY_STYLE,
                        keyStyleAttr.getString(R.styleable.Keyboard_KeyStyle_styleName),
                        skip ? " skipped" : "");
            }
            if (!skip) {
                mParams.mKeyStyles.parseKeyStyleAttributes(keyStyleAttr, keyAttrs, parser);
            }
        } finally {
            keyStyleAttr.recycle();
            keyAttrs.recycle();
        }
        XmlParseUtils.checkEndTag(TAG_KEY_STYLE, parser);
    }

    private void startKeyboard() {

    }

    private void startRow(final KeyboardRow row) {
        mCurrentRow = row;
        mPreviousKeyInRow = null;
    }

    private void endRow(final KeyboardRow row) {
        if (mCurrentRow == null) {
            throw new RuntimeException("orphan end row tag");
        }
        if (mPreviousKeyInRow != null && !mPreviousKeyInRow.isSpacer()) {
            setKeyHitboxRightEdge(mPreviousKeyInRow, mParams.mOccupiedWidth);
            mPreviousKeyInRow = null;
        }
        mCurrentY += row.getRowHeight();
        mCurrentRow = null;
    }

    private void endKey(final Key key, final KeyboardRow row) {
        mParams.onAddKey(key);
        if (mPreviousKeyInRow != null && !mPreviousKeyInRow.isSpacer()) {
            // Make the last key span the gap so there isn't un-clickable space. The current key's
            // hitbox left edge is based on the previous key, so this will make the gap between
            // them split evenly.
            setKeyHitboxRightEdge(mPreviousKeyInRow, row.getKeyX() - row.getKeyLeftPadding());
        }
        mPreviousKeyInRow = key;
    }

    private void setKeyHitboxRightEdge(final Key key, final float xPos) {
        final int keyRight = key.getX() + key.getWidth();
        final float padding = xPos - keyRight;
        key.setHitboxRightEdge(Math.round(padding) + keyRight);
    }

    private void endKeyboard() {
        mParams.removeRedundantMoreKeys();
    }
}
