package rkr.simplekeyboard.inputmethod.latin.dict.decoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rkr.simplekeyboard.inputmethod.latin.common.StringUtils;
import rkr.simplekeyboard.inputmethod.latin.dict.binary.BinaryTrieDictionary;
import rkr.simplekeyboard.inputmethod.latin.dict.spatial.SpatialCandidate;
import rkr.simplekeyboard.inputmethod.latin.dict.spatial.SpatialTouchModel;

public class BeamSearchDecoder {
    private static final int MAX_DEPTH = 64;
    private static final int BEAM_WIDTH = 16;
    private static final int MAX_CHILDREN = 256;
    private static final int MAX_CANDIDATES = 16;
    private static final int MAX_EXPAND = BEAM_WIDTH * MAX_CANDIDATES;
    private static final float LAMBDA = 0.5f;

    public static class BeamHypothesis implements Comparable<BeamHypothesis> {
        public final char[] chars = new char[MAX_DEPTH];
        public int length = 0;
        public int nodeOffset = -1;
        public float spatialLogScore = 0.0f;
        public float totalScore = 0.0f;
        public String word = null;

        public BeamHypothesis() {
        }

        public BeamHypothesis(String word, int nodeOffset, float spatialLogScore, float totalScore) {
            set(word, word != null ? word.length() : 0, nodeOffset, spatialLogScore, totalScore);
        }

        public void set(String word, int len, int nodeOffset, float spatialLogScore, float totalScore) {
            this.word = word;
            this.length = Math.min(len, MAX_DEPTH);
            if (word != null) {
                for (int i = 0; i < this.length; i++) {
                    this.chars[i] = word.charAt(i);
                }
            }
            this.nodeOffset = nodeOffset;
            this.spatialLogScore = spatialLogScore;
            this.totalScore = totalScore;
        }

        public void set(BeamHypothesis parent, char childChar, int nodeOffset, float spatialLogScore, float totalScore) {
            this.length = Math.min(parent.length + 1, MAX_DEPTH);
            if (parent.length > 0) {
                System.arraycopy(parent.chars, 0, this.chars, 0, Math.min(parent.length, MAX_DEPTH - 1));
            }
            if (this.length > 0) {
                this.chars[this.length - 1] = childChar;
            }
            this.word = null;
            this.nodeOffset = nodeOffset;
            this.spatialLogScore = spatialLogScore;
            this.totalScore = totalScore;
        }

        public void copyFrom(BeamHypothesis other) {
            this.length = other.length;
            if (this.length > 0) {
                System.arraycopy(other.chars, 0, this.chars, 0, this.length);
            }
            this.word = other.word;
            this.nodeOffset = other.nodeOffset;
            this.spatialLogScore = other.spatialLogScore;
            this.totalScore = other.totalScore;
        }

        public String getWordString() {
            if (word == null) {
                word = (length > 0) ? new String(chars, 0, length) : "";
            }
            return word;
        }

        @Override
        public int compareTo(BeamHypothesis o) {
            return Float.compare(o.totalScore, this.totalScore); // descending
        }
    }

    private final BinaryTrieDictionary dictionary;
    private final SpatialTouchModel spatialModel;

    // Preallocated beam search history state matrix (MAX_DEPTH x BEAM_WIDTH)
    private final BeamHypothesis[][] mStateHistory = new BeamHypothesis[MAX_DEPTH][BEAM_WIDTH];
    private final int[] mStateHypothesisCounts = new int[MAX_DEPTH];
    private int mCurrentDepth = 0;

    // Preallocated expansion pool
    private final BeamHypothesis[] mNextHypotheses = new BeamHypothesis[MAX_EXPAND];
    private int mNextCount = 0;

    // Reusable scratch buffers for 0 heap allocations
    private final char[] mChildrenChars = new char[MAX_CHILDREN];
    private final int[] mChildrenOffsets = new int[MAX_CHILDREN];
    private final char[] mCandChars = new char[MAX_CANDIDATES];
    private final float[] mCandProbs = new float[MAX_CANDIDATES];
    private final float[] mCandLogProbs = new float[MAX_CANDIDATES];

    private final List<CharSequence> mScratchSuggestions = new ArrayList<>(16);
    private final List<CharSequence> mScratchPrefixes = new ArrayList<>(16);
    private final Set<String> mScratchDeduplication = new HashSet<>(16);

    public BeamSearchDecoder(BinaryTrieDictionary dictionary, SpatialTouchModel spatialModel) {
        this.dictionary = dictionary;
        this.spatialModel = spatialModel;

        for (int d = 0; d < MAX_DEPTH; d++) {
            for (int k = 0; k < BEAM_WIDTH; k++) {
                mStateHistory[d][k] = new BeamHypothesis();
            }
        }
        for (int i = 0; i < MAX_EXPAND; i++) {
            mNextHypotheses[i] = new BeamHypothesis();
        }

        reset();
    }

    public synchronized void reset() {
        mCurrentDepth = 0;
        int rootNode = (dictionary != null) ? dictionary.getRootNode() : -1;
        mStateHistory[0][0].set("", 0, rootNode, 0.0f, 0.0f);
        mStateHypothesisCounts[0] = 1;
    }

    public synchronized void onTouch(float x, float y, char rawChar) {
        if (mCurrentDepth >= MAX_DEPTH - 1) {
            return;
        }
        final int currentCount = mStateHypothesisCounts[mCurrentDepth];
        if (currentCount == 0) {
            return;
        }

        int candCount = (spatialModel != null)
                ? spatialModel.getCandidatesForTouch(x, y, rawChar, mCandChars, mCandProbs, mCandLogProbs, MAX_CANDIDATES)
                : 0;

        if (candCount == 0) {
            mCandChars[0] = rawChar;
            mCandProbs[0] = 1.0f;
            mCandLogProbs[0] = 0.0f;
            candCount = 1;
        }

        mNextCount = 0;
        if (dictionary != null) {
            for (int i = 0; i < currentCount; i++) {
                final BeamHypothesis hyp = mStateHistory[mCurrentDepth][i];
                if (hyp.nodeOffset > 0) {
                    final int childCount = dictionary.getChildren(hyp.nodeOffset, mChildrenChars, mChildrenOffsets);
                    for (int c = 0; c < childCount; c++) {
                        final char childChar = mChildrenChars[c];
                        final int childOffset = mChildrenOffsets[c];
                        final char foldedChild = StringUtils.foldChar(childChar);

                        for (int k = 0; k < candCount; k++) {
                            if (foldedChild == StringUtils.foldChar(mCandChars[k])) {
                                if (mNextCount < MAX_EXPAND) {
                                    int freq = dictionary.getNodeFrequency(childOffset);
                                    float freqLog = (freq > 0) ? (float) Math.log(freq) : 0.0f;
                                    float newSpatialLogScore = hyp.spatialLogScore + mCandLogProbs[k];
                                    float newTotalScore = newSpatialLogScore + (LAMBDA * freqLog);
                                    mNextHypotheses[mNextCount].set(hyp, childChar, childOffset, newSpatialLogScore, newTotalScore);
                                    mNextCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (mNextCount == 0) {
            // Fallback: append rawChar to parent hypotheses
            for (int i = 0; i < currentCount && mNextCount < MAX_EXPAND; i++) {
                final BeamHypothesis hyp = mStateHistory[mCurrentDepth][i];
                mNextHypotheses[mNextCount].set(hyp, rawChar, -1, hyp.spatialLogScore, hyp.totalScore - 5.0f);
                mNextCount++;
            }
        }

        // Sort next hypotheses descending by totalScore in-place (zero allocations)
        sortHypotheses(mNextHypotheses, mNextCount);

        // Push top K to next state
        final int nextDepth = mCurrentDepth + 1;
        final int keep = Math.min(mNextCount, BEAM_WIDTH);
        for (int i = 0; i < keep; i++) {
            mStateHistory[nextDepth][i].copyFrom(mNextHypotheses[i]);
        }
        mStateHypothesisCounts[nextDepth] = keep;
        mCurrentDepth = nextDepth;
    }

    private static void sortHypotheses(BeamHypothesis[] array, int count) {
        // Find top K elements without fully sorting if count is large
        // But since we want 0 allocations, an optimized insertion sort is okay
        // if we stop it early. Let's stick to standard insertion sort for now,
        // but limit the outer loop if we only need top K.
        for (int i = 1; i < count; i++) {
            BeamHypothesis key = array[i];
            float keyScore = key.totalScore;
            int j = i - 1;
            // Only swap if it's within the top BEAM_WIDTH or we're sorting the whole thing
            // Actually standard insertion sort is fine, but we can break early if we only want K
            while (j >= 0 && array[j].totalScore < keyScore) {
                array[j + 1] = array[j];
                j--;
            }
            array[j + 1] = key;
            // Truncate array logic in onTouch is safer, let's just keep this standard
        }
    }

    public synchronized void onBackspace() {
        if (mCurrentDepth > 0) {
            mCurrentDepth--;
        }
    }

    public synchronized List<CharSequence> getSuggestions(String typedWord, int limit, String prevWord) {
        mScratchSuggestions.clear();
        mScratchDeduplication.clear();
        if (typedWord == null || typedWord.isEmpty() || mCurrentDepth <= 0 || dictionary == null || limit <= 0) {
            return mScratchSuggestions;
        }
        if (mCurrentDepth != typedWord.length()) {
            return mScratchSuggestions;
        }
        final int count = mStateHypothesisCounts[mCurrentDepth];
        if (count == 0) {
            return mScratchSuggestions;
        }

        // 1. Collect terminal suggestions from hypotheses
        for (int i = 0; i < count; i++) {
            final BeamHypothesis hyp = mStateHistory[mCurrentDepth][i];
            if (hyp.nodeOffset > 0 && dictionary.isTerminal(hyp.nodeOffset)) {
                final String word = dictionary.getNodeWord(hyp.nodeOffset);
                if (word != null && mScratchDeduplication.add(word.toLowerCase())) {
                    mScratchSuggestions.add(StringUtils.applyCasing(typedWord, word));
                    if (mScratchSuggestions.size() >= limit) {
                        return mScratchSuggestions;
                    }
                }
            }
        }

        // 2. Query prefix suggestions from the top hypothesis
        if (mScratchSuggestions.size() < limit && count > 0) {
            final BeamHypothesis topHyp = mStateHistory[mCurrentDepth][0];
            if (topHyp.nodeOffset > 0 && topHyp.length > 0) {
                final String topWord = topHyp.getWordString();
                mScratchPrefixes.clear();
                dictionary.getPrefixSuggestions(topWord, limit, mScratchPrefixes);
                for (int i = 0; i < mScratchPrefixes.size(); i++) {
                    if (mScratchSuggestions.size() >= limit) {
                        break;
                    }
                    final String word = mScratchPrefixes.get(i).toString();
                    if (mScratchDeduplication.add(word.toLowerCase())) {
                        mScratchSuggestions.add(StringUtils.applyCasing(typedWord, word));
                    }
                }
            }
        }

        return mScratchSuggestions;
    }

    private boolean isValidForCorrection(final String typedWord) {
        return typedWord != null && !typedWord.isEmpty() && dictionary != null
                && mCurrentDepth > 0 && mCurrentDepth == typedWord.length();
    }

    public synchronized String getBestCorrection(String typedWord, float threshold, String prevWord) {
        if (!isValidForCorrection(typedWord)) {
            return null;
        }
        if (dictionary.containsWord(typedWord)) {
            return null;
        }
        final int count = mStateHypothesisCounts[mCurrentDepth];
        if (count == 0) {
            return null;
        }

        // Check top hypothesis
        final BeamHypothesis top = mStateHistory[mCurrentDepth][0];
        if (top != null && top.length > 0 && top.totalScore >= threshold) {
            if (top.nodeOffset > 0 && dictionary.isTerminal(top.nodeOffset)) {
                final String word = dictionary.getNodeWord(top.nodeOffset);
                if (word != null && !word.equalsIgnoreCase(typedWord)) {
                    return StringUtils.applyCasing(typedWord, word);
                }
            }
        }

        // Check other terminal hypotheses in the beam
        for (int i = 1; i < count; i++) {
            final BeamHypothesis hyp = mStateHistory[mCurrentDepth][i];
            if (hyp.nodeOffset > 0 && hyp.totalScore >= threshold && dictionary.isTerminal(hyp.nodeOffset)) {
                final String word = dictionary.getNodeWord(hyp.nodeOffset);
                if (word != null && !word.equalsIgnoreCase(typedWord)) {
                    return StringUtils.applyCasing(typedWord, word);
                }
            }
        }

        return null;
    }
}

