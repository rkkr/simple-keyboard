/*
 * Copyright (C) 2026 The Simple Keyboard contributors
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bangla Phonetic: an original, self-contained transliterator written for Simple Keyboard.
 *
 * <p>Type Bangla the way it sounds. The behaviour is defined by the published cheat sheet:
 * capitals select a different letter (t/ত, T/ট), "o" after a consonant is the inherent vowel,
 * "O" writes ো, "rr" is the reph, and a backtick keeps two consonants apart. No dictionary,
 * no suggestions, no Android dependencies.
 */
public final class BanglaPhoneticEngine {

    private static final String HASANTA = "\u09CD";
    private static final String ZWJ = "\u200D";
    private static final String REPH_NEXT = "m";

    private enum Before { START, CONSONANT, VOWEL, PHOLA, MARK, OTHER }

    private static final Map<String, String> VOWEL_LETTER = new HashMap<>();
    private static final Map<String, String> VOWEL_SIGN = new HashMap<>();
    private static final Map<String, String> CONSONANT = new HashMap<>();
    private static final Map<String, String> SPECIAL = new HashMap<>();
    private static final Map<String, String> ALIAS = new HashMap<>();
    private static final Map<String, String> WORDS = new HashMap<>();
    private static final Map<String, String> PAIR = new HashMap<>();
    private static final Set<String> NO_JOIN = new HashSet<>();
    private static final Set<String> KEYS = new HashSet<>();
    private static int maxKey = 1;

    static {
        String[][] vowels = {
            {"a", "আ", "া"}, {"A", "আ", "া"}, {"i", "ই", "ি"}, {"I", "ঈ", "ী"},
            {"ee", "ঈ", "ী"}, {"u", "উ", "ু"}, {"U", "ঊ", "ূ"}, {"oo", "উ", "ু"},
            {"rri", "ঋ", "ৃ"}, {"e", "এ", "ে"}, {"o", "অ", ""}, {"O", "ও", "ো"},
            {"OI", "ঐ", "ৈ"}, {"OU", "ঔ", "ৌ"}
        };
        for (String[] v : vowels) { VOWEL_LETTER.put(v[0], v[1]); VOWEL_SIGN.put(v[0], v[2]); }

        String[][] consonants = {
            {"k", "ক"}, {"kh", "খ"}, {"g", "গ"}, {"G", "গ"}, {"Ng", "ঙ"},
            {"c", "চ"}, {"ch", "ছ"}, {"j", "জ"}, {"J", "জ়"}, {"jh", "ঝ"}, {"NG", "ঞ"},
            {"T", "ট"}, {"Th", "ঠ"}, {"D", "ড"}, {"Dh", "ঢ"}, {"N", "ণ"},
            {"t", "ত"}, {"th", "থ"}, {"d", "দ"}, {"dh", "ধ"}, {"n", "ন"},
            {"p", "প"}, {"ph", "ফ"}, {"f", "ফ"}, {"b", "ব"}, {"bh", "ভ"}, {"v", "ভ"},
            {"m", "ম"}, {"r", "র"}, {"R", "ড়"}, {"Rh", "ঢ়"},
            {"l", "ল"}, {"sh", "শ"}, {"S", "শ"}, {"Sh", "ষ"}, {"SH", "ষ"}, {"s", "স"},
            {"h", "হ"}, {"H", "হ"}, {"q", "ক"}
        };
        for (String[] c : consonants) CONSONANT.put(c[0], c[1]);
        for (String[] c : consonants) {
            String up = c[0].toUpperCase(java.util.Locale.ROOT);
            if (up.length() == 1 && !CONSONANT.containsKey(up)) CONSONANT.put(up, c[1]);
        }

        SPECIAL.put(",,", HASANTA);
        SPECIAL.put(";;", "\u09BC");
        SPECIAL.put("^", "\u0981");
        SPECIAL.put(":", "\u0983");
        SPECIAL.put(".", "।");
        SPECIAL.put("`", "");
        SPECIAL.put("$", "৳");
        SPECIAL.put("$`", "₹");
        SPECIAL.put(".`", ".");
        SPECIAL.put("?", "?");
        SPECIAL.put("!", "!");
        SPECIAL.put(",", ",");
        SPECIAL.put(";", ";");
        SPECIAL.put("t``", "ৎ");
        String[] bd = {"০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯"};
        for (int i = 0; i < 10; i++) SPECIAL.put(String.valueOf(i), bd[i]);

        WORDS.put("amra", "আমরা");
        WORDS.put("tOmra", "তোমরা");
        WORDS.put("ekTa", "একটা");
        WORDS.put("ekTu", "একটু");
        WORDS.put("apni", "আপনি");
        WORDS.put("apnar", "আপনার");
        WORDS.put("aZDmin", "অ্যাডমিন");

        ALIAS.put("chh", "ছ");
        ALIAS.put("x", "ক্ষ");
        ALIAS.put("X", "ৎ");
        ALIAS.put("TH", "ৎ");
        ALIAS.put("G", HASANTA);
        ALIAS.put("Nggh", "ঙ্ঘ");
        ALIAS.put("NGchh", "ঞ্ছ");
        ALIAS.put("..", "..");
        ALIAS.put("...", "...");

        String[][] pairs = {
            {"k", "k", "ক্ষ"}, {"k", "kh", "ক্ষ"}, {"k", "T", "ক্ট"}, {"k", "l", "ক্ল"},
            {"k", "s", "ক্স"}, {"k", "m", "ক্ম"}, {"k", "t", "ক্ত"},
            {"g", "g", "জ্ঞ"}, {"g", "l", "গ্ল"}, {"l", "p", "ল্প"}, {"l", "l", "ল্ল"},
            {"n", "t", "ন্ত"}, {"n", "d", "ন্দ"}, {"n", "dh", "ন্ধ"}, {"n", "m", "ন্ম"},
            {"n", "n", "ন্ন"}, {"n", "c", "ঞ্চ"}, {"n", "ch", "ঞ্ছ"}, {"n", "j", "ঞ্জ"},
            {"n", "k", "ঙ্ক"}, {"n", "g", "ংগ"}, {"n", "l", "ংল"}, {"n", "y", "ন্য"},
            {"m", "b", "ম্ব"}, {"m", "r", "ম্র"}, {"d", "m", "দ্ম"}, {"t", "r", "ত্র"},
            {"n", "T", "ন্ট"},
            {"b", "r", "ব্র"}, {"p", "r", "প্র"}, {"h", "m", "হ্ম"}, {"r", "h", "র্হ"}
        };
        for (String[] p : pairs) PAIR.put(p[0] + "|" + p[1], p[2]);
        NO_JOIN.addAll(Arrays.asList("s|s", "j|k", "s|b"));

        KEYS.addAll(VOWEL_LETTER.keySet());
        KEYS.addAll(CONSONANT.keySet());
        KEYS.addAll(SPECIAL.keySet());
        KEYS.addAll(ALIAS.keySet());
        KEYS.add("rr");
        for (String k : KEYS) maxKey = Math.max(maxKey, k.length());
    }

    private BanglaPhoneticEngine() {}

    public static String transliterate(String input) {
        if (input == null || input.isEmpty()) return "";
        StringBuilder out = new StringBuilder(input.length() * 2);
        String[] words = input.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            if (i > 0) out.append(' ');
            String special = WORDS.get(words[i]);
            if (special != null) { out.append(special); continue; }
            parseWord(words[i], out);
        }
        return out.toString();
    }

    private static void parseWord(String word, StringBuilder out) {
        List<String> t = tokenize(word);
        Before before = Before.START;
        String lastRoman = "";
        boolean noJoin = false;
        boolean forceIndependent = false;
        int lastConsonantStart = 0;
        for (int i = 0; i < t.size(); i++) {
            String tok = t.get(i);
            String next = i + 1 < t.size() ? t.get(i + 1) : "";
            String afterNext = i + 2 < t.size() ? t.get(i + 2) : "";

            if (tok.equals("`")) { noJoin = true; forceIndependent = true; continue; }

            if (VOWEL_LETTER.containsKey(tok)) {
                if (forceIndependent) {
                    out.append(VOWEL_LETTER.get(tok));
                    forceIndependent = false;
                } else if (tok.equals("a") && next.equals("`")) {
                    out.append("া"); i++;                     // a` forces the vowel sign
                } else if (tok.equals("a") && next.equals("Z")) {
                    out.append("অ");                          // অ + ্যা = অ্যা
                } else if (tok.equals("o") && (before == Before.CONSONANT || before == Before.PHOLA)) {
                    // inherent vowel writes nothing here
                } else if (tok.equals("o") && before == Before.VOWEL) {
                    out.append("ও");
                } else if (tok.equals("a") && before == Before.VOWEL) {
                    out.append("য়া");                        // khaoa → খাওয়া
                } else if (tok.equals("a") && before == Before.PHOLA) {
                    out.append("া");
                } else if (before == Before.CONSONANT || before == Before.PHOLA || before == Before.MARK) {
                    out.append(VOWEL_SIGN.get(tok));
                } else {
                    out.append(VOWEL_LETTER.get(tok));
                }
                before = Before.VOWEL;
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            if (tok.equals("n")) {
                lastConsonantStart = out.length();
                if (next.equals("g")) {
                    if (isVowel(afterNext)) {
                        out.append("\u0999");                  // ঙ before a vowel
                    } else if (afterNext.equals("g")) {
                        out.append("\u0999").append(HASANTA);  // ন্গ → ঙ্
                    } else {
                        out.append("\u0982");                  // anusvara
                    }
                    i++;                                       // ng is one sound
                    before = isVowel(afterNext) ? Before.CONSONANT : Before.OTHER;
                    lastRoman = "n";
                    noJoin = false;
                    continue;
                } else if (next.equals("k") || next.equals("kh")) {
                    out.append("\u0999");                      // ঙ before k
                } else if (!next.isEmpty() && !isVowel(next)) {
                    String pair = PAIR.get("n|" + next);
                    if (pair != null) {
                        out.append(pair);
                        lastConsonantStart = out.length() - pair.length();
                        i++;
                    } else out.append("ন");   // the next consonant adds the joining hasanta
                } else {
                    out.append("ন");
                }
                before = Before.CONSONANT;
                lastRoman = "n";
                noJoin = false;
                continue;
            }

            if (ALIAS.containsKey(tok)) {
                boolean consonantAlias = tok.equals("chh") || tok.equals("x")
                        || tok.equals("Nggh") || tok.equals("NGchh");
                if (consonantAlias) lastConsonantStart = out.length();
                out.append(ALIAS.get(tok));
                before = consonantAlias ? Before.CONSONANT
                        : (tok.equals("G") ? Before.MARK : Before.OTHER);
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            if (CONSONANT.containsKey(tok)) {
                forceIndependent = false;
                if (tok.equals("r") && (lastRoman.equals("o") || before == Before.VOWEL)) {
                    if (REPH_NEXT.indexOf(next) >= 0 && next.length() == 1) {
                        out.append("র্");                      // auto-reph: kormo → কর্ম
                        before = Before.OTHER;
                    } else {
                        out.append("র");                      // vowel/inherent + r writes র
                        before = Before.PHOLA;
                    }
                    lastRoman = "r";
                    noJoin = false;
                    continue;
                }
                String pair = PAIR.get(lastRoman + "|" + tok);
                if (pair != null && !noJoin && before == Before.CONSONANT) {
                    out.setLength(lastConsonantStart);           // replace whole consonant unit
                    out.append(pair);
                    lastConsonantStart = out.length() - pair.length();
                } else {
                    lastConsonantStart = out.length();
                    if (before == Before.CONSONANT && !noJoin
                            && !NO_JOIN.contains(lastRoman + "|" + tok)) {
                        out.append(HASANTA).append(CONSONANT.get(tok));
                    } else {
                        out.append(CONSONANT.get(tok));
                    }
                }
                before = Before.CONSONANT;
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            if (tok.equals("rr")) {
                forceIndependent = false;
                lastConsonantStart = out.length();
                boolean beforePhola = next.equals("y") || next.equals("z") || next.equals("Z")
                        || next.equals("w") || next.equals("r");
                boolean reph = before == Before.CONSONANT || before == Before.VOWEL || before == Before.MARK;
                out.append(reph && !beforePhola ? "র্" : "র");
                before = beforePhola || isVowel(next) ? Before.CONSONANT : Before.OTHER;
                lastRoman = "rr";
                noJoin = false;
                continue;
            }

            if (tok.equals("y") || tok.equals("Y") || tok.equals("z") || tok.equals("Z")) {
                forceIndependent = false;
                lastConsonantStart = out.length();
                if (tok.equals("Y")) {
                    out.append("\u09DF");                    // capital Y keeps letter য়
                    before = Before.CONSONANT;
                    lastRoman = "Y";
                    noJoin = false;
                    continue;
                }
                if (before == Before.CONSONANT || before == Before.PHOLA) {
                    if (lastRoman.equals("r")) out.append(ZWJ);
                    out.append(HASANTA).append("য");
                    before = Before.PHOLA;
                } else if (tok.equals("Z")) {
                    out.append(HASANTA).append("য").append("া");
                    before = Before.PHOLA;
                } else if (before == Before.START) {
                    out.append(tok.equals("y") ? "ই" : "য");
                    before = tok.equals("y") ? Before.VOWEL : Before.CONSONANT;
                } else {
                    out.append(tok.equals("y") ? "য়" : "য");
                    before = Before.PHOLA;
                }
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            if (tok.equals("w")) {
                forceIndependent = false;
                lastConsonantStart = out.length();
                if (before == Before.CONSONANT) out.append(HASANTA).append("ব");
                else out.append("ও");
                before = before == Before.CONSONANT ? Before.CONSONANT : Before.VOWEL;
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            if (SPECIAL.containsKey(tok)) {
                forceIndependent = false;
                out.append(SPECIAL.get(tok));
                before = tok.equals(";") || tok.equals(".") || tok.equals("?") || tok.equals("!")
                        || tok.equals(",") || tok.equals(".`") ? Before.OTHER : Before.MARK;
                lastRoman = tok;
                noJoin = false;
                continue;
            }

            out.append(tok);
            forceIndependent = false;
            before = Before.OTHER;
            lastRoman = tok;
            noJoin = false;
        }
    }

    private static void deleteLastConsonant(StringBuilder out) {
        if (out.length() > 0) out.deleteCharAt(out.length() - 1);
    }

    private static boolean isVowel(String tok) {
        return VOWEL_LETTER.containsKey(tok);
    }

    private static List<String> tokenize(String s) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            String found = null;
            for (int len = Math.min(maxKey, s.length() - i); len > 0; len--) {
                String cand = s.substring(i, i + len);
                if (KEYS.contains(cand)) { found = cand; break; }
            }
            if (found == null) { found = String.valueOf(s.charAt(i)); }
            out.add(found);
            i += found.length();
        }
        return out;
    }
}
