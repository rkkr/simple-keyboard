package rkr.simplekeyboard.inputmethod.latin.dict.binary;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class BinaryTrieDictionaryTest {
    
    private BinaryTrieDictionary esDict;
    private BinaryTrieDictionary enDict;

    @Before
    public void setUp() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> esWords = new java.util.ArrayList<>();
        esWords.add(new BinaryTrieCompiler.WordEntry("que", 250));
        esWords.add(new BinaryTrieCompiler.WordEntry("qué", 220));
        esWords.add(new BinaryTrieCompiler.WordEntry("hay", 200));
        esWords.add(new BinaryTrieCompiler.WordEntry("el", 240));
        esWords.add(new BinaryTrieCompiler.WordEntry("rara", 100));
        esWords.add(new BinaryTrieCompiler.WordEntry("bien", 220));
        esWords.add(new BinaryTrieCompiler.WordEntry("hola", 210));
        esWords.add(new BinaryTrieCompiler.WordEntry("también", 210));
        esWords.add(new BinaryTrieCompiler.WordEntry("esta", 200));
        esWords.add(new BinaryTrieCompiler.WordEntry("está", 220));
        esWords.add(new BinaryTrieCompiler.WordEntry("hoy", 190));
        esWords.add(new BinaryTrieCompiler.WordEntry("hora", 180));
        esWords.add(new BinaryTrieCompiler.WordEntry("hecho", 170));
        esWords.add(new BinaryTrieCompiler.WordEntry("hemos", 160));
        esWords.add(new BinaryTrieCompiler.WordEntry("manzana", 150));
        esWords.add(new BinaryTrieCompiler.WordEntry("buenos", 220));
        esWords.add(new BinaryTrieCompiler.WordEntry("casa", 220));
        esWords.add(new BinaryTrieCompiler.WordEntry("casas", 180));
        for (int i = 0; i < 50; i++) {
            esWords.add(new BinaryTrieCompiler.WordEntry("a" + (char)('a' + (i % 26)) + i, 100 + (i % 50)));
        }

        final List<BinaryTrieCompiler.WordEntry> enWords = new java.util.ArrayList<>();
        enWords.add(new BinaryTrieCompiler.WordEntry("the", 250));
        enWords.add(new BinaryTrieCompiler.WordEntry("and", 240));

        final File esFile = File.createTempFile("test_es_", ".bin");
        esFile.deleteOnExit();
        BinaryTrieCompiler.compile(esWords, esFile);

        final File enFile = File.createTempFile("test_en_", ".bin");
        enFile.deleteOnExit();
        BinaryTrieCompiler.compile(enWords, enFile);

        try (FileInputStream fis = new FileInputStream(esFile);
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, esFile.length());
            esDict = new BinaryTrieDictionary(buffer);
        }

        try (FileInputStream fis = new FileInputStream(enFile);
             FileChannel channel = fis.getChannel()) {
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, enFile.length());
            enDict = new BinaryTrieDictionary(buffer);
        }
    }

    @Test
    public void testContainsWord() {
        assertTrue("Should contain 'que'", esDict.containsWord("que"));
        assertTrue("Should contain 'qué'", esDict.containsWord("qué"));
        assertTrue("Should contain 'hay'", esDict.containsWord("hay"));
        assertTrue("Should contain 'el'", esDict.containsWord("el"));
        assertTrue("Should contain 'rara'", esDict.containsWord("rara"));
        assertTrue("Should contain 'the'", enDict.containsWord("the"));
        
        assertTrue("Should contain 'bien'", esDict.containsWord("bien"));
        assertFalse("Should not contain 'bién'", esDict.containsWord("bién"));
        assertTrue("Should contain 'hola'", esDict.containsWord("hola"));
        assertFalse("Should not contain 'holá'", esDict.containsWord("holá"));
        assertTrue("Should contain 'también'", esDict.containsWord("también"));
        assertFalse("Should not contain 'tí'", esDict.containsWord("tí"));
        assertFalse("Should not contain 'nonexistentword123'", esDict.containsWord("nonexistentword123"));
    }

    @Test
    public void testGetWordFrequency() {
        int freqQue = esDict.getWordFrequency("que");
        assertTrue("Frequency should be valid", freqQue > 0);
        
        int freqQueAccent = esDict.getWordFrequency("qué");
        assertTrue("Frequency should be valid", freqQueAccent > 0);
    }

    @Test
    public void testGetCanonicalWord() {
        // "que" is both unaccented and in the dictionary
        // "qué" should be found when asking for canonical of "que" (wait, which is more frequent?)
        String canonicalQue = esDict.getCanonicalWord("que");
        assertNotNull(canonicalQue);
        // It should match unaccented 'que' chars
        assertTrue(canonicalQue.equals("que") || canonicalQue.equals("qué"));
        
        // "esta" -> "está" or "esta"
        String canonicalEsta = esDict.getCanonicalWord("esta");
        assertNotNull(canonicalEsta);
    }

    @Test
    public void testGetPrefixSuggestions() {
        List<CharSequence> suggestions = new ArrayList<>();
        esDict.getPrefixSuggestions("qu", 5, suggestions);
        assertTrue("Should have some suggestions", suggestions.size() > 0);
    }

    @Test
    public void testNavigationMethods() {
        int root = esDict.getRootNode();
        assertTrue(root > 0);
        
        int childQ = esDict.getChildNode(root, 'q');
        assertTrue(childQ > 0);
        
        int childU = esDict.getChildNode(childQ, 'u');
        assertTrue(childU > 0);
        
        char[] outChars = new char[10];
        int[] outOffsets = new int[10];
        int count = esDict.getChildren(childU, outChars, outOffsets);
        assertTrue(count > 0);
    }

    @Test
    public void testPrefixSuggestionsFindsWordsAcrossMultipleBranches() {
        List<CharSequence> suggestions = new ArrayList<>();
        esDict.getPrefixSuggestions("h", 30, suggestions);
        assertTrue("Should collect suggestions across multiple branches", suggestions.size() > 0);
        boolean foundHoBranch = false;
        boolean foundHeBranch = false;
        for (CharSequence s : suggestions) {
            String w = s.toString().toLowerCase();
            if (w.startsWith("ho")) {
                foundHoBranch = true;
            }
            if (w.startsWith("he")) {
                foundHeBranch = true;
            }
        }
        assertTrue("Prefix 'h' must discover words in 'ho...' branch (e.g. hoy, hola, hora)", foundHoBranch);
        assertTrue("Prefix 'h' must discover words in 'he...' branch (e.g. hecho, hemos)", foundHeBranch);
    }

    @Test
    public void testFuzzySearchFindsMatchWithCostZeroPriority() {
        assertTrue("Dictionary must contain 'manzana'", esDict.containsWord("manzana"));
        List<rkr.simplekeyboard.inputmethod.latin.dict.PrefixDictionary.ScoredWord> candidates = new java.util.ArrayList<>();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "mwnzana", 0, 1, candidates);
        boolean foundManzana = false;
        for (rkr.simplekeyboard.inputmethod.latin.dict.PrefixDictionary.ScoredWord sw : candidates) {
            if ("manzana".equalsIgnoreCase(sw.word)) {
                foundManzana = true;
                break;
            }
        }
        assertTrue("Fuzzy search for 'mwnzana' must find 'manzana' without getting trapped in earlier branches", foundManzana);

        candidates.clear();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "hxla", 0, 1, candidates);
        boolean foundHola = false;
        for (rkr.simplekeyboard.inputmethod.latin.dict.PrefixDictionary.ScoredWord sw : candidates) {
            if ("hola".equalsIgnoreCase(sw.word)) {
                foundHola = true;
                break;
            }
        }
        assertTrue("Fuzzy search for 'hxla' must find 'hola'", foundHola);
    }

    @Test
    public void testPrefixSuggestionsBoundariesAndLimits() {
        // Limit 1, 5, 40 boundary tests
        List<CharSequence> res1 = new ArrayList<>();
        esDict.getPrefixSuggestions("a", 1, res1);
        assertEquals(1, res1.size());

        List<CharSequence> res5 = new ArrayList<>();
        esDict.getPrefixSuggestions("a", 5, res5);
        assertEquals(5, res5.size());

        List<CharSequence> res40 = new ArrayList<>();
        esDict.getPrefixSuggestions("a", 40, res40);
        assertEquals(40, res40.size());

        // Empty / non-matching prefix
        List<CharSequence> resNone = new ArrayList<>();
        esDict.getPrefixSuggestions("zzxxqq123", 10, resNone);
        assertEquals(0, resNone.size());
    }

    @Test
    public void testOutParameterBufferReuseAndIsolation() {
        final List<CharSequence> buffer = new ArrayList<>();

        // 1. Initial query fills buffer
        esDict.getPrefixSuggestions("ho", 5, buffer);
        assertEquals(3, buffer.size());
        final List<CharSequence> firstSnapshot = new ArrayList<>(buffer);

        // 2. Caller properly clears buffer and queries again: exact same result
        buffer.clear();
        esDict.getPrefixSuggestions("ho", 5, buffer);
        assertEquals(firstSnapshot, buffer);

        // 3. Invalid/empty inputs should not leave residual elements or fail
        buffer.clear();
        esDict.getPrefixSuggestions(null, 5, buffer);
        assertTrue(buffer.isEmpty());

        buffer.clear();
        esDict.getPrefixSuggestions("", 5, buffer);
        assertTrue(buffer.isEmpty());

        buffer.clear();
        esDict.getPrefixSuggestions("ho", 0, buffer);
        assertTrue(buffer.isEmpty());

        buffer.clear();
        esDict.getPrefixSuggestions("ho", -1, buffer);
        assertTrue(buffer.isEmpty());

        // 4. Null buffer does not crash
        esDict.getPrefixSuggestions("ho", 5, null);

        // 5. Query after invalid inputs works identically
        buffer.clear();
        esDict.getPrefixSuggestions("ho", 5, buffer);
        assertEquals(firstSnapshot, buffer);
    }

    @Test
    public void testFuzzySearchAdversarialCases() {
        List<rkr.simplekeyboard.inputmethod.latin.dict.PrefixDictionary.ScoredWord> candidates = new java.util.ArrayList<>();

        // 1. Exact match (Cost 0)
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "casa", 0, 0, candidates);
        assertTrue("Exact match search for 'casa' with distance 0 must find 'casa'",
                candidates.stream().anyMatch(sw -> "casa".equalsIgnoreCase(sw.word)));

        // 2. Extra character typed by user (deletion from target)
        candidates.clear();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "casasx", 0, 1, candidates);
        assertTrue("Fuzzy search for 'casasx' must find 'casas'",
                candidates.stream().anyMatch(sw -> "casas".equalsIgnoreCase(sw.word)));

        // 3. Missing character typed by user (insertion into target)
        candidates.clear();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "csa", 0, 1, candidates);
        assertTrue("Fuzzy search for 'csa' must find 'casa'",
                candidates.stream().anyMatch(sw -> "casa".equalsIgnoreCase(sw.word)));

        // 4. Non-matching input (negative case)
        candidates.clear();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "zzqqxx123", 0, 1, candidates);
        assertEquals("Fuzzy search for impossible word must return 0 candidates", 0, candidates.size());
    }

    @Test
    public void testFuzzySearchTranspositionCorrections() {
        // Transposition test 1: "teh" -> "the"
        List<rkr.simplekeyboard.inputmethod.latin.dict.PrefixDictionary.ScoredWord> candidates = new java.util.ArrayList<>();
        enDict.searchFuzzy(enDict.getRootNode(), new StringBuilder(), "teh", 0, 1, candidates);
        assertTrue("Fuzzy search for transposed 'teh' must find 'the'",
                candidates.stream().anyMatch(sw -> "the".equalsIgnoreCase(sw.word)));

        // Transposition test 2: "buneos" -> "buenos"
        candidates.clear();
        esDict.searchFuzzy(esDict.getRootNode(), new StringBuilder(), "buneos", 0, 1, candidates);
        assertTrue("Fuzzy search for transposed 'buneos' must find 'buenos'",
                candidates.stream().anyMatch(sw -> "buenos".equalsIgnoreCase(sw.word)));
    }

    @Test
    public void testForEachWord() {
        int[] count = new int[1];
        boolean[] foundQue = new boolean[1];
        esDict.forEachWord((word, freq) -> {
            count[0]++;
            if ("que".equals(word) && freq > 0) {
                foundQue[0] = true;
            }
        });
        assertTrue("Should traverse words", count[0] >= 50);
        assertTrue("Should find 'que'", foundQue[0]);
    }

    @Test
    public void testVersion1BackwardCompatibility() {
        // Build a manual Version 1 buffer:
        // Header (16 bytes), Root Node (16 bytes), Child Node 'h' (16 bytes), String "h\0" (2 bytes)
        final byte[] stringBytes = "h\0".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final int headerSize = 16;
        final int nodeSize = 16;
        final int rootOffset = headerSize; // 16
        final int childOffset = rootOffset + nodeSize; // 32
        final int stringOffset = childOffset + nodeSize; // 48
        final ByteBuffer buf = ByteBuffer.allocate(stringOffset + stringBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        // Header v1 (16 bytes)
        buf.putInt(0x42444B53); // Magic
        buf.putInt(1);          // Version 1
        buf.putInt(1);          // wordCount
        buf.putInt(rootOffset); // rootOffset

        // Root Node at offset 16 (char '\0', flags 2 = has children, childCount 1, childrenOffset 32)
        buf.putShort((short) '\0');
        buf.put((byte) 2);      // has children
        buf.put((byte) 0);      // freq
        buf.put((byte) 1);      // childCount
        buf.put((byte) 0);      // pad
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.putInt(childOffset); // childrenOffset = 32
        buf.putInt(0);          // wordOffset

        // Child Node 'h' at offset 32 (terminal with freq 200, wordOffset pointing to 48)
        buf.putShort((short) 'h');
        buf.put((byte) 1);      // terminal flag
        buf.put((byte) 200);    // freq
        buf.put((byte) 0);      // childCount
        buf.put((byte) 0);      // pad
        buf.put((byte) 0);
        buf.put((byte) 0);
        buf.putInt(0);          // childrenOffset
        buf.putInt(stringOffset); // wordOffset = 48

        // String pool
        buf.put(stringBytes);

        final BinaryTrieDictionary v1Dict = new BinaryTrieDictionary(buf);
        assertEquals(1, v1Dict.getVersion());
        assertEquals(1, v1Dict.getWordCount());
        assertEquals(0, v1Dict.getBigramCount());
        assertTrue(v1Dict.validateStructure());
        assertEquals(200, v1Dict.getWordFrequency("h"));
        assertTrue(v1Dict.containsWord("h"));
        assertEquals(0, v1Dict.getBigramFrequency("h", "anything"));
        assertTrue(v1Dict.getNextWordPredictions("h", 5).isEmpty());
    }

    @Test
    public void testV2BigramPredictionsAndFrequencies() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> words = new java.util.ArrayList<>();
        words.add(new BinaryTrieCompiler.WordEntry("buenos", 220));
        words.add(new BinaryTrieCompiler.WordEntry("días", 210));
        words.add(new BinaryTrieCompiler.WordEntry("tardes", 190));
        words.add(new BinaryTrieCompiler.WordEntry("noches", 180));

        final List<BinaryTrieCompiler.BigramEntry> bigrams = new java.util.ArrayList<>();
        bigrams.add(new BinaryTrieCompiler.BigramEntry("buenos", "días", 250));
        bigrams.add(new BinaryTrieCompiler.BigramEntry("buenos", "tardes", 200));
        bigrams.add(new BinaryTrieCompiler.BigramEntry("buenos", "noches", 150));

        final File tempFile = File.createTempFile("test_v2_preds_", ".bin");
        tempFile.deleteOnExit();
        BinaryTrieCompiler.compile(words, bigrams, tempFile);

        try (FileInputStream fis = new FileInputStream(tempFile);
             FileChannel channel = fis.getChannel()) {
            final ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length());
            final BinaryTrieDictionary dict = new BinaryTrieDictionary(buf);

            assertEquals(2, dict.getVersion());
            assertEquals(4, dict.getWordCount());
            assertEquals(3, dict.getBigramCount());
            assertTrue(dict.validateStructure());

            assertEquals(250, dict.getBigramFrequency("buenos", "días"));
            assertEquals(200, dict.getBigramFrequency("buenos", "tardes"));
            assertEquals(150, dict.getBigramFrequency("buenos", "noches"));
            assertEquals(0, dict.getBigramFrequency("días", "noches"));

            final java.util.List<CharSequence> out = new java.util.ArrayList<>();
            int count = dict.getNextWordPredictions("buenos", 2, out);
            assertEquals(2, count);
            assertEquals(2, out.size());
            assertEquals("días", out.get(0).toString());
            assertEquals("tardes", out.get(1).toString());

            final java.util.List<CharSequence> all = dict.getNextWordPredictions("buenos", 10);
            assertEquals(3, all.size());
            assertEquals("días", all.get(0).toString());
            assertEquals("tardes", all.get(1).toString());
            assertEquals("noches", all.get(2).toString());
        }
    }

    @Test
    public void testValidateStructureV2CorruptBigramOffsets() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> words = new java.util.ArrayList<>();
        words.add(new BinaryTrieCompiler.WordEntry("hola", 200));
        words.add(new BinaryTrieCompiler.WordEntry("mundo", 180));

        final List<BinaryTrieCompiler.BigramEntry> bigrams = new java.util.ArrayList<>();
        bigrams.add(new BinaryTrieCompiler.BigramEntry("hola", "mundo", 150));

        final File tempFile = File.createTempFile("test_corrupt_", ".bin");
        tempFile.deleteOnExit();
        BinaryTrieCompiler.compile(words, bigrams, tempFile);

        final byte[] bytes = java.nio.file.Files.readAllBytes(tempFile.toPath());
        final ByteBuffer validBuf = ByteBuffer.wrap(bytes);
        validBuf.order(ByteOrder.LITTLE_ENDIAN);
        final BinaryTrieDictionary validDict = new BinaryTrieDictionary(validBuf);
        assertTrue(validDict.validateStructure());

        // Corrupt word2Offset inside the bigram entry (offset = bigramTableOffset + 4)
        final ByteBuffer corruptBuf = ByteBuffer.wrap(bytes.clone());
        corruptBuf.order(ByteOrder.LITTLE_ENDIAN);
        int bigramTableOffset = corruptBuf.getInt(20);
        corruptBuf.putInt(bigramTableOffset + 4, 999999); // Out of bounds word2 offset
        final BinaryTrieDictionary corruptDict = new BinaryTrieDictionary(corruptBuf);
        assertFalse(corruptDict.validateStructure());
    }
}
