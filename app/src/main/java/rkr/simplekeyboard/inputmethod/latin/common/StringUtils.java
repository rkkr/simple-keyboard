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

package rkr.simplekeyboard.inputmethod.latin.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public final class StringUtils {
    private static final String EMPTY_STRING = "";

    private StringUtils() {
        // This utility class is not publicly instantiable.
    }

    // Taken from android.text.TextUtils. We are extensively using this method in many places,
    // some of which don't have the android libraries available.
    /**
     * Returns true if the string is null or 0-length.
     * @param str the string to be examined
     * @return true if str is null or zero length
     */
    public static boolean isEmpty(final CharSequence str) {
        return (str == null || str.length() == 0);
    }

    // Taken from android.text.TextUtils to cut the dependency to the Android framework.
    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param delimiter the delimiter
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     */
    public static String join(final CharSequence delimiter,
            final Iterable<?> tokens) {
        final StringBuilder sb = new StringBuilder();
        boolean firstTime = true;
        for (final Object token: tokens) {
            if (firstTime) {
                firstTime = false;
            } else {
                sb.append(delimiter);
            }
            sb.append(token);
        }
        return sb.toString();
    }

    public static int codePointCount(final CharSequence text) {
        if (isEmpty(text)) {
            return 0;
        }
        return Character.codePointCount(text, 0, text.length());
    }

    public static String newSingleCodePointString(final int codePoint) {
        if (Character.charCount(codePoint) == 1) {
            // Optimization: avoid creating a temporary array for characters that are
            // represented by a single char value
            return String.valueOf((char) codePoint);
        }
        // For surrogate pair
        return new String(Character.toChars(codePoint));
    }

    public static boolean containsInArray(final String text,
            final String[] array) {
        for (final String element : array) {
            if (text.equals(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Comma-Splittable Text is similar to Comma-Separated Values (CSV) but has much simpler syntax.
     * Unlike CSV, Comma-Splittable Text has no escaping mechanism, so that the text can't contain
     * a comma character in it.
     */
    private static final String SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT = ",";

    public static boolean containsInCommaSplittableText(final String text,
            final String extraValues) {
        if (isEmpty(extraValues)) {
            return false;
        }
        return containsInArray(text, extraValues.split(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT));
    }

    public static String removeFromCommaSplittableTextIfExists(final String text,
            final String extraValues) {
        if (isEmpty(extraValues)) {
            return EMPTY_STRING;
        }
        final String[] elements = extraValues.split(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT);
        if (!containsInArray(text, elements)) {
            return extraValues;
        }
        final ArrayList<String> result = new ArrayList<>(elements.length - 1);
        for (final String element : elements) {
            if (!text.equals(element)) {
                result.add(element);
            }
        }
        return join(SEPARATOR_FOR_COMMA_SPLITTABLE_TEXT, result);
    }

    public static String capitalizeFirstCodePoint(final String s,
            final Locale locale) {
        if (s.length() <= 1) {
            return s.toUpperCase(getLocaleUsedForToTitleCase(locale));
        }
        // Please refer to the comment below in
        // {@link #capitalizeFirstAndDowncaseRest(String,Locale)} as this has the same shortcomings
        final int cutoff = s.offsetByCodePoints(0, 1);
        return s.substring(0, cutoff).toUpperCase(getLocaleUsedForToTitleCase(locale))
                + s.substring(cutoff);
    }

    public static int[] toCodePointArray(final CharSequence charSequence) {
        return toCodePointArray(charSequence, 0, charSequence.length());
    }

    private static final int[] EMPTY_CODEPOINTS = {};

    /**
     * Converts a range of a string to an array of code points.
     * @param charSequence the source string.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @return a new array of code points. At most endIndex - startIndex, but possibly less.
     */
    public static int[] toCodePointArray(final CharSequence charSequence,
            final int startIndex, final int endIndex) {
        final int length = charSequence.length();
        if (length <= 0) {
            return EMPTY_CODEPOINTS;
        }
        final int[] codePoints =
                new int[Character.codePointCount(charSequence, startIndex, endIndex)];
        copyCodePointsAndReturnCodePointCount(codePoints, charSequence, startIndex, endIndex,
                false /* downCase */);
        return codePoints;
    }

    /**
     * Copies the codepoints in a CharSequence to an int array.
     *
     * This method assumes there is enough space in the array to store the code points. The size
     * can be measured with Character#codePointCount(CharSequence, int, int) before passing to this
     * method. If the int array is too small, an ArrayIndexOutOfBoundsException will be thrown.
     * Also, this method makes no effort to be thread-safe. Do not modify the CharSequence while
     * this method is running, or the behavior is undefined.
     * This method can optionally downcase code points before copying them, but it pays no attention
     * to locale while doing so.
     *
     * @param destination the int array.
     * @param charSequence the CharSequence.
     * @param startIndex the start index inside the string in java chars, inclusive.
     * @param endIndex the end index inside the string in java chars, exclusive.
     * @param downCase if this is true, code points will be downcased before being copied.
     * @return the number of copied code points.
     */
    public static int copyCodePointsAndReturnCodePointCount(final int[] destination,
            final CharSequence charSequence, final int startIndex, final int endIndex,
            final boolean downCase) {
        int destIndex = 0;
        for (int index = startIndex; index < endIndex;
                index = Character.offsetByCodePoints(charSequence, index, 1)) {
            final int codePoint = Character.codePointAt(charSequence, index);
            // TODO: stop using this, as it's not aware of the locale and does not always do
            // the right thing.
            destination[destIndex] = downCase ? Character.toLowerCase(codePoint) : codePoint;
            destIndex++;
        }
        return destIndex;
    }

    public static int[] toSortedCodePointArray(final String string) {
        final int[] codePoints = toCodePointArray(string);
        Arrays.sort(codePoints);
        return codePoints;
    }

    public static boolean isIdenticalAfterUpcase(final String text) {
        final int length = text.length();
        int i = 0;
        while (i < length) {
            final int codePoint = text.codePointAt(i);
            if (Character.isLetter(codePoint) && !Character.isUpperCase(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    public static boolean isIdenticalAfterDowncase(final String text) {
        final int length = text.length();
        int i = 0;
        while (i < length) {
            final int codePoint = text.codePointAt(i);
            if (Character.isLetter(codePoint) && !Character.isLowerCase(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    public static boolean isIdenticalAfterCapitalizeEachWord(final String text,
            final int[] sortedSeparators) {
        boolean needsCapsNext = true;
        final int len = text.length();
        for (int i = 0; i < len; i = text.offsetByCodePoints(i, 1)) {
            final int codePoint = text.codePointAt(i);
            if (Character.isLetter(codePoint)) {
                if ((needsCapsNext && !Character.isUpperCase(codePoint))
                        || (!needsCapsNext && !Character.isLowerCase(codePoint))) {
                    return false;
                }
            }
            // We need a capital letter next if this is a separator.
            needsCapsNext = (Arrays.binarySearch(sortedSeparators, codePoint) >= 0);
        }
        return true;
    }

    // TODO: like capitalizeFirst*, this does not work perfectly for Dutch because of the IJ digraph
    // which should be capitalized together in *some* cases.
    public static String capitalizeEachWord(final String text,
            final int[] sortedSeparators, final Locale locale) {
        final StringBuilder builder = new StringBuilder();
        boolean needsCapsNext = true;
        final int len = text.length();
        for (int i = 0; i < len; i = text.offsetByCodePoints(i, 1)) {
            final String nextChar = text.substring(i, text.offsetByCodePoints(i, 1));
            if (needsCapsNext) {
                builder.append(nextChar.toUpperCase(locale));
            } else {
                builder.append(nextChar.toLowerCase(locale));
            }
            // We need a capital letter next if this is a separator.
            needsCapsNext = (Arrays.binarySearch(sortedSeparators, nextChar.codePointAt(0)) >= 0);
        }
        return builder.toString();
    }

    private static final String LANGUAGE_GREEK = "el";

    private static Locale getLocaleUsedForToTitleCase(final Locale locale) {
        // In Greek locale {@link String#toUpperCase(Locale)} eliminates accents from its result.
        // In order to get accented upper case letter, {@link Locale#ROOT} should be used.
        if (LANGUAGE_GREEK.equals(locale.getLanguage())) {
            return Locale.ROOT;
        }
        return locale;
    }

    public static String toTitleCaseOfKeyLabel(final String label,
            final Locale locale) {
        if (label == null) {
            return label;
        }
        return label.toUpperCase(getLocaleUsedForToTitleCase(locale));
    }

    public static int toTitleCaseOfKeyCode(final int code, final Locale locale) {
        if (!Constants.isLetterCode(code)) {
            return code;
        }
        final String label = newSingleCodePointString(code);
        final String titleCaseLabel = toTitleCaseOfKeyLabel(label, locale);
        return codePointCount(titleCaseLabel) == 1
                ? titleCaseLabel.codePointAt(0) : Constants.CODE_UNSPECIFIED;
    }
}
