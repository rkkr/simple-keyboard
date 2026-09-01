/*
 * Copyright (C) 2026 Simple Keyboard Authors
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

package rkr.simplekeyboard.inputmethod.latin.dict;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class PrefixDictionaryNGramEquivalenceTest {

    private PrefixDictionary mDict;

    @Before
    public void setUp() {
        mDict = new PrefixDictionary();
    }

    /**
     * Exact reference implementation of legacy collectTopNGramWords using full O(N log N) sort.
     */
    private boolean collectTopNGramWordsLegacy(final PrefixDictionary dict, final Map<String, Short> nextMap,
                                              final List<CharSequence> results, final Set<String> added, final int limit) {
        if (nextMap == null || nextMap.isEmpty()) {
            return false;
        }
        if (results.size() >= limit) {
            return true;
        }
        final List<Map.Entry<String, Short>> sortedEntries = new ArrayList<>(nextMap.entrySet());
        Collections.sort(sortedEntries, (a, b) -> Integer.compare(b.getValue() & 0xFFFF, a.getValue() & 0xFFFF));
        for (Map.Entry<String, Short> entry : sortedEntries) {
            if (dict.isBlocked(entry.getKey())) continue;
            final String candidate = dict.getCanonicalWord(entry.getKey());
            if (candidate != null && !dict.isBlocked(candidate) && added.add(candidate.toLowerCase())) {
                results.add(candidate);
                if (results.size() >= limit) {
                    return true;
                }
            }
        }
        return false;
    }

    private void assertEquivalence(final PrefixDictionary dict, final Map<String, Short> nextMap,
                                   final List<CharSequence> initialResults, final Set<String> initialAdded,
                                   final int limit) {
        final List<CharSequence> legacyResults = new ArrayList<>(initialResults);
        final Set<String> legacyAdded = new HashSet<>(initialAdded);
        final boolean legacyFull = collectTopNGramWordsLegacy(dict, nextMap, legacyResults, legacyAdded, limit);

        final List<CharSequence> actualResults = new ArrayList<>(initialResults);
        final Set<String> actualAdded = new HashSet<>(initialAdded);
        final boolean actualFull = dict.collectTopNGramWords(nextMap, actualResults, actualAdded, limit);

        assertEquals("Full return value mismatch", legacyFull, actualFull);
        assertEquals("Results size mismatch", legacyResults.size(), actualResults.size());
        assertEquals("Results content/order mismatch", legacyResults, actualResults);
        assertEquals("Added set mismatch", legacyAdded, actualAdded);
    }

    @Test
    public void testEmptyMap() {
        assertEquivalence(mDict, null, new ArrayList<>(), new HashSet<>(), 3);
        assertEquivalence(mDict, new LinkedHashMap<>(), new ArrayList<>(), new HashSet<>(), 3);
    }

    @Test
    public void testSmallN() {
        mDict.insert("apple", 100);
        mDict.insert("banana", 80);

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("apple", (short) 100);
        map.put("banana", (short) 80);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 1);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 3);
    }

    @Test
    public void testLargeN() {
        final Map<String, Short> map = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            final String word = "word" + i;
            mDict.insert(word, i + 1);
            map.put(word, (short) (i + 1));
        }

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 1);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 3);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 10);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 32);
    }

    @Test
    public void testLimitBoundaries() {
        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("w1", (short) 10);
        map.put("w2", (short) 50);
        map.put("w3", (short) 40);
        map.put("w4", (short) 90);
        map.put("w5", (short) 30);

        for (String k : map.keySet()) {
            mDict.insert(k, map.get(k));
        }

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 0);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 1);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 3);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 4);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 5);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 6);
    }

    @Test
    public void testEqualFrequenciesPreservesOrder() {
        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("alpha", (short) 100);
        map.put("beta", (short) 100);
        map.put("gamma", (short) 100);
        map.put("delta", (short) 100);

        mDict.insert("alpha", 100);
        mDict.insert("beta", 100);
        mDict.insert("gamma", 100);
        mDict.insert("delta", 100);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 3);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 4);
    }

    @Test
    public void testDistinctCaseCanonicals() {
        mDict.insert("hello", 100);
        mDict.insert("Hello", 90);
        mDict.insert("HELLO", 80);
        mDict.insert("world", 70);

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("hello", (short) 100);
        map.put("Hello", (short) 90);
        map.put("HELLO", (short) 80);
        map.put("world", (short) 70);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
    }

    @Test
    public void testCandidateAlreadyInAddedWithDifferentCasing() {
        mDict.insert("HELLO", 100);
        mDict.insert("world", 80);

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("HELLO", (short) 100);
        map.put("world", (short) 80);

        final List<CharSequence> initialResults = new ArrayList<>();
        initialResults.add("hello");
        final Set<String> initialAdded = new HashSet<>();
        initialAdded.add("hello");

        assertEquivalence(mDict, map, initialResults, initialAdded, 2);
    }

    @Test
    public void testHigherFreqAppearsAfterLowerFreqForSameCanonical() {
        mDict.insert("hello", 50);
        mDict.insert("Hello", 100);
        mDict.insert("world", 80);

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("hello", (short) 50);
        map.put("world", (short) 80);
        map.put("Hello", (short) 100);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
    }

    @Test
    public void testCanonicalDuplicateAfterTopKIsFull() {
        mDict.insert("apple", 90);
        mDict.insert("banana", 80);
        mDict.insert("cherry", 70);
        mDict.insert("BANANA", 100);

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("apple", (short) 90);
        map.put("banana", (short) 80);
        map.put("cherry", (short) 70);
        map.put("BANANA", (short) 100);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
    }

    @Test
    public void testBlockedWordsAboveLimit() {
        mDict.insert("valid1", 100);
        mDict.insert("blocked1", 200);
        mDict.insert("valid2", 150);
        mDict.insert("blocked2", 140);
        mDict.insert("valid3", 130);
        mDict.insert("blocked3", 120);
        mDict.insert("valid4", 110);

        mDict.blockWord("blocked1");
        mDict.blockWord("blocked2");
        mDict.blockWord("blocked3");

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("blocked1", (short) 200);
        map.put("valid2", (short) 150);
        map.put("blocked2", (short) 140);
        map.put("valid3", (short) 130);
        map.put("blocked3", (short) 120);
        map.put("valid4", (short) 110);
        map.put("valid1", (short) 100);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 3);
        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 4);
    }

    @Test
    public void testAllTopKBlocked() {
        mDict.insert("b1", 200);
        mDict.insert("b2", 190);
        mDict.insert("b3", 180);
        mDict.insert("valid1", 170);
        mDict.insert("valid2", 160);

        mDict.blockWord("b1");
        mDict.blockWord("b2");
        mDict.blockWord("b3");

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("b1", (short) 200);
        map.put("b2", (short) 190);
        map.put("b3", (short) 180);
        map.put("valid1", (short) 170);
        map.put("valid2", (short) 160);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 2);
    }

    @Test
    public void testBlockedNLessThanK() {
        mDict.insert("b1", 200);
        mDict.insert("valid1", 100);
        mDict.blockWord("b1");

        final Map<String, Short> map = new LinkedHashMap<>();
        map.put("b1", (short) 200);
        map.put("valid1", (short) 100);

        assertEquivalence(mDict, map, new ArrayList<>(), new HashSet<>(), 5);
    }

    @Test
    public void testRandomizedDifferentialFuzzing() {
        final Random random = new Random(1337);
        final String[] wordBases = new String[] {
            "apple", "banana", "cherry", "date", "elderberry",
            "fig", "grape", "honeydew", "kiwi", "lemon"
        };

        // 1000 randomized differential fuzz runs comparing new Top-K against legacy reference
        for (int run = 0; run < 1000; run++) {
            final PrefixDictionary dict = new PrefixDictionary();
            final Map<String, Short> map = new LinkedHashMap<>();
            final int mapSize = random.nextInt(40);

            for (int i = 0; i < mapSize; i++) {
                final String base = wordBases[random.nextInt(wordBases.length)];
                final String word;
                int casing = random.nextInt(4);
                if (casing == 0) {
                    word = base.toLowerCase();
                } else if (casing == 1) {
                    word = Character.toUpperCase(base.charAt(0)) + base.substring(1).toLowerCase();
                } else if (casing == 2) {
                    word = base.toUpperCase();
                } else {
                    word = base + "_" + random.nextInt(5);
                }

                final short freq;
                int freqType = random.nextInt(5);
                if (freqType == 0) {
                    freq = 0; // min
                } else if (freqType == 1) {
                    freq = (short) 65535; // max unsigned short
                } else if (freqType == 2) {
                    freq = (short) 100; // tie bucket
                } else if (freqType == 3) {
                    freq = (short) 200; // tie bucket 2
                } else {
                    freq = (short) random.nextInt(256);
                }

                dict.insert(word, freq & 0xFFFF);
                map.put(word, freq);

                if (random.nextInt(8) == 0) {
                    dict.blockWord(word);
                }
            }

            final List<CharSequence> initialResults = new ArrayList<>();
            final Set<String> initialAdded = new HashSet<>();
            if (random.nextBoolean()) {
                final String preset = wordBases[random.nextInt(wordBases.length)].toLowerCase();
                initialResults.add(preset);
                initialAdded.add(preset);
            }

            final int limit = random.nextInt(8); // 0..7
            assertEquivalence(dict, map, initialResults, initialAdded, limit);
        }
    }
}
