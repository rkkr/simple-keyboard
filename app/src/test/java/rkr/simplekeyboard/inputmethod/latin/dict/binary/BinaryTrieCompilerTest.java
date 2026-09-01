package rkr.simplekeyboard.inputmethod.latin.dict.binary;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class BinaryTrieCompilerTest {

    @Rule
    public TemporaryFolder mTempFolder = new TemporaryFolder();

    @Test
    public void testCompileAndLoadV2WithoutBigrams() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> words = new ArrayList<>();
        words.add(new BinaryTrieCompiler.WordEntry("hola", 200));
        words.add(new BinaryTrieCompiler.WordEntry("holanda", 150));
        words.add(new BinaryTrieCompiler.WordEntry("bien", 220));
        words.add(new BinaryTrieCompiler.WordEntry("también", 210));
        words.add(new BinaryTrieCompiler.WordEntry("canción", 180));

        final File tempFile = mTempFolder.newFile("test_dict.bin");
        BinaryTrieCompiler.compile(words, tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 32);

        try (FileInputStream fis = new FileInputStream(tempFile);
             FileChannel channel = fis.getChannel()) {
            final ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length());
            final BinaryTrieDictionary dict = new BinaryTrieDictionary(buffer);

            assertEquals(2, dict.getVersion());
            assertEquals(5, dict.getWordCount());
            assertEquals(0, dict.getBigramCount());
            assertTrue(dict.validateStructure());

            assertTrue(dict.containsWord("hola"));
            assertTrue(dict.containsWord("holanda"));
            assertTrue(dict.containsWord("bien"));
            assertTrue(dict.containsWord("también"));
            assertTrue(dict.containsWord("canción"));
            assertFalse(dict.containsWord("bién"));
            assertFalse(dict.containsWord("nonexistent"));

            assertEquals(200, dict.getWordFrequency("hola"));
            assertEquals(220, dict.getWordFrequency("bien"));
            assertEquals(210, dict.getWordFrequency("también"));

            final List<CharSequence> suggestions = new ArrayList<>();
            dict.getPrefixSuggestions("hol", 5, suggestions);
            assertTrue(suggestions.size() >= 2);
            assertEquals("también", dict.getCanonicalWord("tambien"));
        }
    }

    @Test
    public void testCompileAndLoadV2WithBigrams() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> words = new ArrayList<>();
        words.add(new BinaryTrieCompiler.WordEntry("cómo", 220));
        words.add(new BinaryTrieCompiler.WordEntry("estás", 200));
        words.add(new BinaryTrieCompiler.WordEntry("va", 190));
        words.add(new BinaryTrieCompiler.WordEntry("bien", 180));

        final List<BinaryTrieCompiler.BigramEntry> bigrams = new ArrayList<>();
        bigrams.add(new BinaryTrieCompiler.BigramEntry("cómo", "estás", 150));
        bigrams.add(new BinaryTrieCompiler.BigramEntry("cómo", "va", 180));
        bigrams.add(new BinaryTrieCompiler.BigramEntry("estás", "bien", 120));
        // Duplicate with lower freq - should be ignored in favor of higher
        bigrams.add(new BinaryTrieCompiler.BigramEntry("cómo", "estás", 100));
        // Bigram with invalid/unknown word - should be ignored
        bigrams.add(new BinaryTrieCompiler.BigramEntry("cómo", "desconocido", 90));

        final File tempFile = mTempFolder.newFile("test_v2_bigrams.bin");
        BinaryTrieCompiler.compile(words, bigrams, tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 32);

        try (FileInputStream fis = new FileInputStream(tempFile);
             FileChannel channel = fis.getChannel()) {
            final ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, tempFile.length());
            final BinaryTrieDictionary dict = new BinaryTrieDictionary(buffer);

            assertEquals(2, dict.getVersion());
            assertEquals(4, dict.getWordCount());
            assertEquals(3, dict.getBigramCount());
            assertTrue(dict.validateStructure());

            // Bigram frequencies
            assertEquals(180, dict.getBigramFrequency("cómo", "va"));
            assertEquals(150, dict.getBigramFrequency("cómo", "estás"));
            assertEquals(120, dict.getBigramFrequency("estás", "bien"));
            assertEquals(0, dict.getBigramFrequency("cómo", "bien"));
            assertEquals(0, dict.getBigramFrequency("desconocido", "va"));

            // Next word predictions (sorted by frequency descending: "va" [180] before "estás" [150])
            final List<CharSequence> predictions = dict.getNextWordPredictions("cómo", 5);
            assertNotNull(predictions);
            assertEquals(2, predictions.size());
            assertEquals("va", predictions.get(0).toString());
            assertEquals("estás", predictions.get(1).toString());

            // Limit boundary test
            final List<CharSequence> limited = dict.getNextWordPredictions("cómo", 1);
            assertNotNull(limited);
            assertEquals(1, limited.size());
            assertEquals("va", limited.get(0).toString());

            // Non-existent predictions
            final List<CharSequence> none = dict.getNextWordPredictions("bien", 5);
            assertNotNull(none);
            assertEquals(0, none.size());
        }
    }
}
