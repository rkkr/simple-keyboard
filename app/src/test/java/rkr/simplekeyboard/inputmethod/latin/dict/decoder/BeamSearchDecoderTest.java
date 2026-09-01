package rkr.simplekeyboard.inputmethod.latin.dict.decoder;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import rkr.simplekeyboard.inputmethod.latin.dict.binary.BinaryTrieCompiler;
import rkr.simplekeyboard.inputmethod.latin.dict.binary.BinaryTrieDictionary;
import rkr.simplekeyboard.inputmethod.latin.dict.spatial.SpatialCandidate;
import rkr.simplekeyboard.inputmethod.latin.dict.spatial.SpatialTouchModel;

public class BeamSearchDecoderTest {
    
    private BeamSearchDecoder decoder;
    
    // Custom Mock BinaryTrieDictionary
    private class MockDictionary extends BinaryTrieDictionary {
        public MockDictionary() {
            super(createDummyBuffer());
        }
        
        @Override
        public int getRootNode() {
            return 1;
        }
        
        @Override
        public int getChildren(int nodeOffset, char[] outChars, int[] outOffsets) {
            if (nodeOffset == 1) {
                outChars[0] = 's';
                outChars[1] = 'a';
                outOffsets[0] = 2;
                outOffsets[1] = 3;
                return 2;
            }
            return 0;
        }
        
        @Override
        public int getNodeFrequency(int nodeOffset) {
            if (nodeOffset == 2) return 100;
            if (nodeOffset == 3) return 200;
            return 0;
        }
        
        @Override
        public boolean isTerminal(int nodeOffset) {
            return nodeOffset == 2 || nodeOffset == 3;
        }
        
        @Override
        public String getNodeWord(int nodeOffset) {
            if (nodeOffset == 2) return "s";
            if (nodeOffset == 3) return "a";
            return null;
        }
        
        @Override
        public void getPrefixSuggestions(String prefix, int limit, List<CharSequence> result) {
        }
    }
    
    // Custom Mock SpatialTouchModel
    private class MockTouchModel extends SpatialTouchModel {
        @Override
        public int getCandidatesForTouch(float touchX, float touchY, int fallbackCode,
                char[] outChars, float[] outProbs, float[] outLogProbs, int maxCandidates) {
            if (touchX == 10.0f && touchY == 10.0f && fallbackCode == 's') {
                outChars[0] = 's';
                outProbs[0] = 0.6f;
                outLogProbs[0] = -0.51f;
                outChars[1] = 'a';
                outProbs[1] = 0.4f;
                outLogProbs[1] = -0.91f;
                return 2;
            }
            return 0;
        }

        @Override
        public List<SpatialCandidate> getCandidatesForTouch(float touchX, float touchY, int fallbackCode) {
            if (touchX == 10.0f && touchY == 10.0f && fallbackCode == 's') {
                return java.util.Arrays.asList(
                    new SpatialCandidate('s', 0.6f, -0.51f),
                    new SpatialCandidate('a', 0.4f, -0.91f)
                );
            }
            return new java.util.ArrayList<>();
        }
    }
    
    private static ByteBuffer createDummyBuffer() {
        ByteBuffer buf = ByteBuffer.allocate(32);
        buf.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        buf.putInt(0, 0x42444b53); // magic
        buf.putInt(4, 1);          // version
        buf.putInt(8, 0);          // wordCount
        buf.putInt(12, 16);        // rootOffset
        return buf;
    }

    @Before
    public void setUp() {
        decoder = new BeamSearchDecoder(new MockDictionary(), new MockTouchModel());
    }

    @Test
    public void testTypingWithErrors() {
        decoder.onTouch(10.0f, 10.0f, 's');
        
        List<CharSequence> suggestions = decoder.getSuggestions("s", 3, "");
        assertFalse(suggestions.isEmpty());
    }

    @Test
    public void testBackspacePopping() {
        decoder.onTouch(0, 0, 'h');
        decoder.onTouch(0, 0, 'e');
        decoder.onBackspace();
        assertNotNull(decoder.getSuggestions("h", 3, ""));
    }

    @Test
    public void testBestCorrectionThreshold() {
        decoder.onTouch(10.0f, 10.0f, 's');
        // If threshold is very high, it should return null
        assertNull(decoder.getBestCorrection("s", 100.0f, ""));
        
        // If threshold is reasonable and best correction differs from typedWord, it returns the correction
        String correction = decoder.getBestCorrection("x", 0.5f, "");
        assertNotNull(correction);
    }

    @Test
    public void testVuenosToBuenosCorrectionRanking() throws Exception {
        final List<BinaryTrieCompiler.WordEntry> words = new ArrayList<>();
        words.add(new BinaryTrieCompiler.WordEntry("buenos", 220));
        words.add(new BinaryTrieCompiler.WordEntry("dias", 180));

        final File tmp = File.createTempFile("test_vuenos_dict_", ".bin");
        tmp.deleteOnExit();
        BinaryTrieCompiler.compile(words, tmp);

        byte[] b = new byte[(int) tmp.length()];
        try (FileInputStream fis = new FileInputStream(tmp)) {
            int read = 0;
            while (read < b.length) {
                int r = fis.read(b, read, b.length - read);
                if (r < 0) break;
                read += r;
            }
        }
        BinaryTrieDictionary dict = new BinaryTrieDictionary(ByteBuffer.wrap(b));

        SpatialTouchModel spatialModel = new SpatialTouchModel();
        // Row 1: e(200,50), u(400,50), o(600,50)
        spatialModel.addKey('e', 200f, 50f, 100f, 100f);
        spatialModel.addKey('u', 400f, 50f, 100f, 100f);
        spatialModel.addKey('o', 600f, 50f, 100f, 100f);
        // Row 2: s(150,100)
        spatialModel.addKey('s', 150f, 100f, 100f, 100f);
        // Row 3: v(350,150), b(450,150), n(550,150)
        spatialModel.addKey('v', 350f, 150f, 100f, 100f);
        spatialModel.addKey('b', 450f, 150f, 100f, 100f);
        spatialModel.addKey('n', 550f, 150f, 100f, 100f);

        BeamSearchDecoder realDecoder = new BeamSearchDecoder(dict, spatialModel);

        // User typed "vuenos" by touching between 'v' and 'b' (x=390, y=150, fallback='v'), then 'u', 'e', 'n', 'o', 's'
        realDecoder.onTouch(390f, 150f, 'v');
        realDecoder.onTouch(400f, 50f, 'u');
        realDecoder.onTouch(200f, 50f, 'e');
        realDecoder.onTouch(550f, 150f, 'n');
        realDecoder.onTouch(600f, 50f, 'o');
        realDecoder.onTouch(150f, 100f, 's');

        List<CharSequence> suggestions = realDecoder.getSuggestions("vuenos", 3, "");
        assertFalse(suggestions.isEmpty());
        assertEquals("buenos", suggestions.get(0).toString());

        String bestCorrection = realDecoder.getBestCorrection("vuenos", -30.0f, "");
        assertNotNull(bestCorrection);
        assertEquals("buenos", bestCorrection);
    }
}

