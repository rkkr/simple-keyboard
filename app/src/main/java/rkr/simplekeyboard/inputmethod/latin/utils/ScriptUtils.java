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

package rkr.simplekeyboard.inputmethod.latin.utils;

import java.util.TreeMap;

/**
 * A class to help with handling different writing scripts.
 */
public class ScriptUtils {

    // Used for hardware keyboards
    public static final int SCRIPT_UNKNOWN = -1;

    public static final int SCRIPT_ARABIC = 0;
    public static final int SCRIPT_ARMENIAN = 1;
    public static final int SCRIPT_BENGALI = 2;
    public static final int SCRIPT_CYRILLIC = 3;
    public static final int SCRIPT_DEVANAGARI = 4;
    public static final int SCRIPT_GEORGIAN = 5;
    public static final int SCRIPT_GREEK = 6;
    public static final int SCRIPT_HEBREW = 7;
    public static final int SCRIPT_KANNADA = 8;
    public static final int SCRIPT_KHMER = 9;
    public static final int SCRIPT_LAO = 10;
    public static final int SCRIPT_LATIN = 11;
    public static final int SCRIPT_MALAYALAM = 12;
    public static final int SCRIPT_MYANMAR = 13;
    public static final int SCRIPT_SINHALA = 14;
    public static final int SCRIPT_TAMIL = 15;
    public static final int SCRIPT_TELUGU = 16;
    public static final int SCRIPT_THAI = 17;

    private static final TreeMap<String, Integer> mLanguageCodeToScriptCode;

    static {
        mLanguageCodeToScriptCode = new TreeMap<>();
        mLanguageCodeToScriptCode.put("", SCRIPT_LATIN); // default
        mLanguageCodeToScriptCode.put("ar", SCRIPT_ARABIC);
        mLanguageCodeToScriptCode.put("hy", SCRIPT_ARMENIAN);
        mLanguageCodeToScriptCode.put("bn", SCRIPT_BENGALI);
        mLanguageCodeToScriptCode.put("bg", SCRIPT_CYRILLIC);
        mLanguageCodeToScriptCode.put("sr", SCRIPT_CYRILLIC);
        mLanguageCodeToScriptCode.put("ru", SCRIPT_CYRILLIC);
        mLanguageCodeToScriptCode.put("ka", SCRIPT_GEORGIAN);
        mLanguageCodeToScriptCode.put("el", SCRIPT_GREEK);
        mLanguageCodeToScriptCode.put("iw", SCRIPT_HEBREW);
        mLanguageCodeToScriptCode.put("km", SCRIPT_KHMER);
        mLanguageCodeToScriptCode.put("lo", SCRIPT_LAO);
        mLanguageCodeToScriptCode.put("ml", SCRIPT_MALAYALAM);
        mLanguageCodeToScriptCode.put("my", SCRIPT_MYANMAR);
        mLanguageCodeToScriptCode.put("si", SCRIPT_SINHALA);
        mLanguageCodeToScriptCode.put("ta", SCRIPT_TAMIL);
        mLanguageCodeToScriptCode.put("te", SCRIPT_TELUGU);
        mLanguageCodeToScriptCode.put("th", SCRIPT_THAI);
    }
}
