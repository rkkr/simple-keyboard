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
 *
 * Ported from `BornoPhonetic.kt` by Wasi Master (wmkeyboard),
 * Copyright (c) 2026 Wasi Master, licensed MIT. See the original notice:
 * https://github.com/wasi-master/wmkeyboard
 */
package rkr.simplekeyboard.inputmethod.latin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Borno Phonetic: the phonetic layout of Codepotro's Borno keyboard. Java port of
 * {@code BornoPhonetic.kt} by Wasi Master (wmkeyboard), licensed MIT — see
 * THIRD_PARTY_NOTICES.md. It implements Borno's published Android keymap and
 * observed behaviour; no code or data is taken from Borno's GPL parser (BornoTSF).
 * Which keys write which letters follows that keymap; which consonants join is
 * the list in {@link #JOINS}.
 *
 * <p>It differs from Avro in the keys people notice: {@code c} is চ and {@code ch}
 * is ছ, {@code z} is য, {@code y} and {@code w} after a consonant are য-ফলা and
 * ব-ফলা, {@code kkh} is ক্ষ, {@code gg} is জ্ঞ, {@code A} or {@code xx} is
 * চন্দ্রবিন্দু, and the reph takes {@code rr} ({@code korrm} is কর্ম, {@code korm}
 * is করম).
 *
 * <p>Letters not named in either case read as their lowercase key: {@code K} is ক,
 * but {@code T} and {@code t} differ.
 *
 * <p>Pure JVM on purpose: no Android imports, no regex in the hot path, the tables
 * are built once and are immutable, and one {@link StringBuilder} is used per call.
 * Spelling correction is not done here; that is a separate table.
 */
public final class BanglaPhoneticEngine {

    private static final String HASANT = "্";
    private static final String ZWNJ = "‌";
    private static final String ZWJ = "‍";
    private static final String NUKTA = "়";

    /** The longest key any table holds. */
    private static final int LONGEST_KEY = 4;

    /** Stands for the consonant before a phala: another may still sign it, and nothing joins it. */
    private static final char AFTER_PHALA = '্';

    private static final class Vowel {
        final String letter;
        final String sign;

        Vowel(String letter, String sign) {
            this.letter = letter;
            this.sign = sign;
        }
    }

    private static final Map<String, Vowel> VOWELS = buildVowels();

    private static final Map<String, String> CONSONANTS = buildConsonants();

    /** Written as they are, with nothing joining on after them. */
    private static final Map<String, String> MARKS = buildMarks();

    /** The hasant, and the joiners that shape the conjunct it makes. */
    private static final Map<String, String> BREAKS = buildBreaks();

    /** A vowel's sign whatever stands before it, and marks written as their own character. */
    private static final Map<String, String> FORCED = buildForced();

    /** Punctuation and digits, which end whatever cluster was being built. */
    private static final Map<String, String> PLAIN = buildPlain();

    /** The other keys a token can be; the rules in {@link #parse} say what each writes. */
    private static final Set<String> RULED = buildRuled();

    /** Which consonants take which after them with no hasant typed. */
    private static final Map<Character, String> JOINS = buildJoins();

    /** {@code n} in front of these is the nasal of their own row: {@code nk} is ঙ্ক, {@code nj} is ঞ্জ. */
    private static final Map<Character, Character> ROW_NASAL = buildRowNasal();

    private BanglaPhoneticEngine() {
    }

    /**
     * One romanized word (or free text) in Bangla. Anything no table knows stays
     * as it is. Backslashes keep Latin: {@code facebook\\} is {@code facebook}, one
     * of them is itself, and past that each pair is one backslash, an odd run also
     * keeping what stands before it in Latin.
     */
    public static String transliterate(String input) {
        int at = input.indexOf('\\');
        if (at < 0) {
            return parse(input);
        }
        int end = at;
        while (end < input.length() && input.charAt(end) == '\\') {
            end++;
        }
        int run = end - at;
        String left = input.substring(0, at);
        boolean latin = run <= 4 ? run == 2 || run == 3 : run % 2 == 1;
        int kept;
        switch (run) {
            case 1:
            case 3:
                kept = 1;
                break;
            case 2:
                kept = 0;
                break;
            case 4:
                kept = 2;
                break;
            default:
                kept = run / 2;
                break;
        }
        return (latin ? left : parse(left)) + "\\".repeat(kept) + transliterate(input.substring(end));
    }

    /** What the text so far ends on, which decides what the next key writes. */
    private enum Before { WORD_START, CONSONANT, VOWEL, MARK, OTHER }

    /** The parser's cursor: what was written so far and what it can still join. */
    private static final class ParseState {
        final StringBuilder out;
        Before before = Before.WORD_START;
        /** The letter a next consonant could join, or null when nothing can. */
        Character last;
        /** Whether the cluster has joined once already; it joins no further but for a third. */
        boolean joined;
        /** Whether {@link #last} is a ra-phala rather than a written র. */
        boolean raPhala;

        ParseState(StringBuilder out) {
            this.out = out;
        }

        void consonant(String text) {
            out.append(text);
            before = Before.CONSONANT;
            last = text.charAt(text.length() - 1);
            raPhala = false;
        }

        void ended(Before state) {
            before = state;
            last = null;
            joined = false;
            raPhala = false;
        }
    }

    private static String parse(String input) {
        ParseState s = new ParseState(new StringBuilder(input.length() * 2));

        List<String> tokens = tokens(input);
        int i = 0;
        while (i < tokens.size()) {
            String key = tokens.get(i++);
            String next = i < tokens.size() ? tokens.get(i) : null;
            Vowel vowel = VOWELS.get(key);
            if (vowel != null) {
                if (s.before == Before.CONSONANT) {
                    s.out.append(vowel.sign);
                } else if (s.before == Before.VOWEL) {
                    s.out.append("o".equals(key) ? "ও" : vowel.letter);
                } else {
                    s.out.append(vowel.letter);
                }
                s.ended(Before.VOWEL);
            } else if (" ".equals(key)) {
                s.out.append(' ');
                s.ended(Before.WORD_START);
            } else if ("r".equals(key)) {
                if (s.before == Before.CONSONANT && s.last != null) {
                    s.out.append(HASANT).append('র');
                    s.last = 'র';
                    s.raPhala = true;
                } else {
                    s.consonant("র");
                }
                s.before = Before.CONSONANT;
            } else if ("rr".equals(key)) {
                if (s.before == Before.CONSONANT && s.last != null) {
                    s.out.append(HASANT).append("রর");
                    s.before = Before.CONSONANT;
                    s.last = 'র';
                } else if (next != null && !VOWELS.containsKey(next)
                        && !"r".equals(next) && !"R".equals(next)) {
                    // A reph sits on what follows, so nothing is left to join.
                    s.out.append("র্");
                    s.ended(Before.OTHER);
                } else {
                    s.consonant("রর");
                }
            } else if ("y".equals(key) || "z".equals(key)) {
                if (s.before != Before.CONSONANT || s.last == null) {
                    s.consonant("y".equals(key) ? "য়" : "য");
                } else if (s.last == 'র' && !s.raPhala) {
                    // র‍্য: a written র keeps its shape in front of the য-ফলা.
                    s.out.append(ZWJ).append(HASANT).append('য');
                    s.last = AFTER_PHALA;
                } else {
                    s.out.append(HASANT).append('য');
                    s.last = AFTER_PHALA;
                }
            } else if ("w".equals(key)) {
                if (s.before == Before.CONSONANT && s.last != null) {
                    s.out.append(HASANT).append('ব');
                    s.last = AFTER_PHALA;
                } else if (s.before == Before.MARK) {
                    s.consonant("ব");
                } else if ("a".equals(next) || "e".equals(next)) {
                    s.out.append("a".equals(next) ? "ওয়া" : "ওয়ে");
                    i++;
                    s.ended(Before.VOWEL);
                } else {
                    s.out.append('ও');
                    s.ended(Before.VOWEL);
                }
            } else if ("x".equals(key)) {
                s.consonant(s.before == Before.WORD_START ? "এক্স" : "ক্স");
            } else if ("ng".equals(key)) {
                Vowel sign = next == null ? null : VOWELS.get(next);
                if (sign != null) {
                    i++;
                    s.out.append("ঙ্গ").append(sign.sign);
                    s.ended(Before.VOWEL);
                } else {
                    s.out.append('ং');
                    s.ended(Before.MARK);
                }
            } else if ("./".equals(key) || "/".equals(key)) {
                if (s.before == Before.CONSONANT) {
                    s.out.append(NUKTA);
                } else {
                    s.out.append(key);
                    s.ended(Before.OTHER);
                }
            } else if (BREAKS.containsKey(key)) {
                s.out.append(BREAKS.get(key));
                // A vowel after it is still a sign; a consonant joins by the hasant alone.
                s.before = Before.CONSONANT;
                s.last = null;
                s.joined = false;
            } else if (MARKS.containsKey(key)) {
                s.out.append(MARKS.get(key));
                s.ended(Before.MARK);
            } else if (FORCED.containsKey(key)) {
                s.out.append(FORCED.get(key));
                s.ended(Before.VOWEL);
            } else if (CONSONANTS.containsKey(key)) {
                String text = CONSONANTS.get(key);
                char head = text.charAt(0);
                Character previous = s.last;
                Character nasal = previous != null && previous == 'ন' ? ROW_NASAL.get(head) : null;
                if (previous == null || s.before != Before.CONSONANT) {
                    s.joined = false;
                } else if (s.joined && !joinsThird(s.out, head)) {
                    s.joined = false;
                } else if (nasal != null) {
                    s.out.setCharAt(s.out.length() - 1, nasal);
                    s.out.append(HASANT);
                    s.joined = true;
                } else if (joins(previous, head)) {
                    s.out.append(HASANT);
                    s.joined = true;
                } else {
                    s.joined = false;
                }
                s.consonant(text);
            } else if (PLAIN.containsKey(key)) {
                s.out.append(PLAIN.get(key));
                s.ended(Before.OTHER);
            } else {
                s.out.append(key);
                s.ended(Before.OTHER);
            }
        }
        return s.out.toString();
    }

    private static boolean joins(char previous, char head) {
        String takes = JOINS.get(previous);
        return takes != null && takes.indexOf(head) >= 0;
    }

    /** The two clusters a third consonant joins: স্ক্ল and স্প্ল. */
    private static boolean joinsThird(CharSequence out, char head) {
        return head == 'ল' && (endsWith(out, "স্ক") || endsWith(out, "স্প"));
    }

    private static boolean endsWith(CharSequence text, String suffix) {
        int offset = text.length() - suffix.length();
        if (offset < 0) {
            return false;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (text.charAt(offset + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static final class Token {
        final String key;
        final int length;

        Token(String key, int length) {
            this.key = key;
            this.length = length;
        }
    }

    private static List<String> tokens(String input) {
        List<String> out = new ArrayList<>();
        int at = 0;
        while (at < input.length()) {
            Token token = tokenAt(input, at);
            if (token == null) {
                out.add(String.valueOf(input.charAt(at)));
                at++;
            } else {
                out.add(token.key);
                at += token.length;
            }
        }
        return out;
    }

    /**
     * The longest key at {@code at}: as typed, and only then with its capitals folded,
     * so {@code Sh} stays ষ while {@code KH} still reads as {@code kh}.
     */
    private static Token tokenAt(String input, int at) {
        int max = Math.min(LONGEST_KEY, input.length() - at);
        for (int length = max; length >= 1; length--) {
            String typed = input.substring(at, at + length);
            if (known(typed)) {
                return new Token(typed, length);
            }
            String folded = typed.toLowerCase(Locale.ROOT);
            if (!folded.equals(typed) && known(folded)) {
                return new Token(folded, length);
            }
        }
        return null;
    }

    private static boolean known(String key) {
        return RULED.contains(key) || VOWELS.containsKey(key) || CONSONANTS.containsKey(key)
                || MARKS.containsKey(key) || BREAKS.containsKey(key) || FORCED.containsKey(key)
                || PLAIN.containsKey(key);
    }

    private static Map<String, Vowel> buildVowels() {
        Map<String, Vowel> map = new HashMap<>();
        map.put("o", new Vowel("অ", ""));
        map.put("a", new Vowel("আ", "া"));
        map.put("i", new Vowel("ই", "ি"));
        map.put("I", new Vowel("ঈ", "ী"));
        map.put("ee", new Vowel("ঈ", "ী"));
        map.put("u", new Vowel("উ", "ু"));
        map.put("oo", new Vowel("উ", "ু"));
        map.put("U", new Vowel("ঊ", "ূ"));
        map.put("rri", new Vowel("ঋ", "ৃ"));
        map.put("e", new Vowel("এ", "ে"));
        map.put("OI", new Vowel("ঐ", "ৈ"));
        map.put("O", new Vowel("ও", "ো"));
        map.put("OU", new Vowel("ঔ", "ৌ"));
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildConsonants() {
        Map<String, String> map = new HashMap<>();
        map.put("k", "ক");
        map.put("kh", "খ");
        map.put("g", "গ");
        map.put("gh", "ঘ");
        map.put("Ng", "ঙ");
        map.put("c", "চ");
        map.put("ch", "ছ");
        map.put("j", "জ");
        map.put("jh", "ঝ");
        map.put("NG", "ঞ");
        map.put("T", "ট");
        map.put("Th", "ঠ");
        map.put("D", "ড");
        map.put("Dh", "ঢ");
        map.put("N", "ণ");
        map.put("t", "ত");
        map.put("th", "থ");
        map.put("d", "দ");
        map.put("dh", "ধ");
        map.put("n", "ন");
        map.put("p", "প");
        map.put("ph", "ফ");
        map.put("f", "ফ");
        map.put("b", "ব");
        map.put("bh", "ভ");
        map.put("v", "ভ");
        map.put("m", "ম");
        map.put("l", "ল");
        map.put("sh", "শ");
        map.put("S", "শ");
        map.put("Sh", "ষ");
        map.put("SH", "ষ");
        map.put("s", "স");
        map.put("h", "হ");
        map.put("R", "ড়");
        map.put("Rh", "ঢ়");
        map.put("Y", "য়");
        map.put("q", "ক");
        map.put("kkh", "ক্ষ");
        map.put("kx", "ক্ষ");
        map.put("gg", "জ্ঞ");
        map.put("Ngx", "ঙ্ক্ষ");
        map.put("ngh", "ঙ্ঘ");
        map.put("ngH", "ংহ");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildMarks() {
        Map<String, String> map = new HashMap<>();
        map.put("X", "ৎ");
        map.put("TH", "ৎ");
        map.put("t``", "ৎ");
        map.put("::", "ঃ");
        map.put("ff", "ঃ");
        map.put("HH", "ঃ");
        map.put("^", "ঁ");
        map.put("xx", "ঁ");
        map.put("A", "ঁ");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildBreaks() {
        Map<String, String> map = new HashMap<>();
        map.put(",,", HASANT);
        map.put(",.", HASANT + ZWNJ);
        map.put(",..", HASANT + ZWJ);
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildForced() {
        Map<String, String> map = new HashMap<>();
        map.put("a`", "া");
        map.put("i`", "ি");
        map.put("I`", "ী");
        map.put("u`", "ু");
        map.put("U`", "ূ");
        map.put("e`", "ে");
        map.put("o`", "");
        map.put("O`", "ো");
        map.put("OI`", "ৈ");
        map.put("OU`", "ৌ");
        map.put("rri`", "ৃ");
        map.put("^`", "^");
        map.put(":`", ":");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> buildPlain() {
        Map<String, String> map = new HashMap<>();
        map.put("`", "");
        map.put("``", "`");
        map.put(".", "।");
        map.put("..", "।।");
        map.put("...", "...");
        map.put("$", "৳");
        map.put("$$", "$");
        map.put("$%", "₹");
        String digits = "০১২৩৪৫৬৭৮৯";
        for (int digit = 0; digit < digits.length(); digit++) {
            String latin = Integer.toString(digit);
            String bangla = String.valueOf(digits.charAt(digit));
            map.put(latin, bangla);
            map.put("." + latin, "." + bangla);
        }
        return Collections.unmodifiableMap(map);
    }

    private static Set<String> buildRuled() {
        Set<String> set = new HashSet<>();
        set.add("r");
        set.add("rr");
        set.add("y");
        set.add("z");
        set.add("w");
        set.add("x");
        set.add("ng");
        set.add("./");
        set.add("/");
        return Collections.unmodifiableSet(set);
    }

    private static Map<Character, String> buildJoins() {
        Map<Character, String> map = new HashMap<>();
        map.put('ক', "কটতমলসশষ");
        map.put('গ', "ধণনমল");
        map.put('ঘ', "ন");
        map.put('ঙ', "কখগঘম");
        map.put('চ', "চছঞ");
        map.put('জ', "জঝঞ");
        map.put('ঞ', "চছজঝ");
        map.put('ট', "টম");
        map.put('ড', "ড");
        map.put('ণ', "টঠডঢণনম");
        map.put('ত', "তথনম");
        map.put('দ', "গঘদধভম");
        map.put('ধ', "নম");
        map.put('ন', "টঠডতথদধনমস");
        map.put('প', "টতনপলস");
        map.put('ফ', "ল");
        map.put('ব', "জদধবল");
        map.put('ভ', "ল");
        map.put('ম', "থনপফবভমল");
        map.put('ল', "কখগঘটডপফবভধমল");
        map.put('শ', "চছতনমল");
        map.put('ষ', "কটঠণপফম");
        map.put('স', "কখটতথনপফমল");
        map.put('হ', "ণনমল");
        return Collections.unmodifiableMap(map);
    }

    private static Map<Character, Character> buildRowNasal() {
        Map<Character, Character> map = new HashMap<>();
        map.put('ক', 'ঙ');
        map.put('খ', 'ঙ');
        map.put('চ', 'ঞ');
        map.put('ছ', 'ঞ');
        map.put('জ', 'ঞ');
        map.put('ঝ', 'ঞ');
        return Collections.unmodifiableMap(map);
    }
}
