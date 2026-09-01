package rkr.simplekeyboard.inputmethod.latin.dict.binary;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

public final class BinaryTrieCompiler {

    public static final class WordEntry {
        public final String word;
        public final int frequency;

        public WordEntry(final String word, final int frequency) {
            this.word = word;
            this.frequency = Math.min(Math.max(frequency, 1), 255);
        }
    }

    public static final class BigramEntry {
        public final String word1;
        public final String word2;
        public final int frequency;

        public BigramEntry(final String word1, final String word2, final int frequency) {
            this.word1 = word1;
            this.word2 = word2;
            this.frequency = Math.min(Math.max(frequency, 1), 255);
        }
    }

    private static final class BuildNode {
        final char character;
        int frequency = 0;
        boolean isTerminal = false;
        String word = null;
        int offset = 0;
        final Map<Character, BuildNode> children = new TreeMap<>();
        List<BuildNode> childrenList = Collections.emptyList();

        BuildNode(final char character) {
            this.character = character;
        }
    }

    private static final class CompiledBigram {
        final int word1Offset;
        final int word2Offset;
        final int frequency;

        CompiledBigram(final int word1Offset, final int word2Offset, final int frequency) {
            this.word1Offset = word1Offset;
            this.word2Offset = word2Offset;
            this.frequency = frequency;
        }
    }

    private BinaryTrieCompiler() {
    }

    public static void compile(final List<WordEntry> words, final File outputFile) throws IOException {
        compile(words, Collections.emptyList(), outputFile);
    }

    public static void compile(final List<WordEntry> words, final List<BigramEntry> bigrams, final File outputFile) throws IOException {
        final BuildNode root = new BuildNode('\0');

        for (final WordEntry entry : words) {
            if (entry.word == null || entry.word.isEmpty()) {
                continue;
            }
            BuildNode current = root;
            final int len = entry.word.length();
            for (int i = 0; i < len; i++) {
                final char c = Character.toLowerCase(entry.word.charAt(i));
                BuildNode next = current.children.get(c);
                if (next == null) {
                    next = new BuildNode(c);
                    current.children.put(c, next);
                }
                current = next;
            }
            current.isTerminal = true;
            current.frequency = entry.frequency;
            current.word = entry.word;
        }

        final ByteArrayOutputStream stringPoolStream = new ByteArrayOutputStream();
        final Map<String, Integer> wordToOffset = new HashMap<>();

        final List<BuildNode> allNodes = new ArrayList<>();
        final Queue<List<BuildNode>> queue = new LinkedList<>();

        final int headerSize = 32;
        final int nodeSize = 16;
        int currentNodeOffset = headerSize;

        final List<BuildNode> rootList = Collections.singletonList(root);
        queue.add(rootList);

        while (!queue.isEmpty()) {
            final List<BuildNode> levelNodes = queue.poll();
            if (levelNodes == null || levelNodes.isEmpty()) {
                continue;
            }
            int offset = currentNodeOffset;
            currentNodeOffset += levelNodes.size() * nodeSize;
            for (final BuildNode node : levelNodes) {
                node.offset = offset;
                offset += nodeSize;
                allNodes.add(node);

                if (!node.children.isEmpty()) {
                    final List<BuildNode> children = new ArrayList<>(node.children.values());
                    node.childrenList = children;
                    queue.add(children);
                }
            }
        }

        for (final BuildNode node : allNodes) {
            if (node.isTerminal && node.word != null) {
                final String lower = node.word.toLowerCase();
                if (!wordToOffset.containsKey(lower)) {
                    final byte[] utf8 = node.word.getBytes(StandardCharsets.UTF_8);
                    final int strOffset = stringPoolStream.size();
                    wordToOffset.put(lower, strOffset);
                    wordToOffset.put(node.word, strOffset);
                    stringPoolStream.write(utf8);
                    stringPoolStream.write(0);
                }
            }
        }

        final int trieNodesEnd = currentNodeOffset;
        final int bigramTableOffset = trieNodesEnd;

        // Process and deduplicate bigrams
        final List<CompiledBigram> compiledBigrams = new ArrayList<>();
        if (bigrams != null && !bigrams.isEmpty()) {
            final Map<Long, Integer> dedupBigrams = new HashMap<>();
            for (final BigramEntry bg : bigrams) {
                if (bg == null || bg.word1 == null || bg.word2 == null || bg.word1.isEmpty() || bg.word2.isEmpty()) {
                    continue;
                }
                final Integer w1Rel = wordToOffset.get(bg.word1.toLowerCase());
                final Integer w2Rel = wordToOffset.get(bg.word2.toLowerCase());
                if (w1Rel == null || w2Rel == null) {
                    continue;
                }
                final long pairKey = (((long) w1Rel) << 32) | (w2Rel & 0xFFFFFFFFL);
                final Integer existingFreq = dedupBigrams.get(pairKey);
                if (existingFreq == null || bg.frequency > existingFreq) {
                    dedupBigrams.put(pairKey, bg.frequency);
                }
            }

            final int bigramTableSize = dedupBigrams.size() * 12;
            final int stringPoolStart = bigramTableOffset + bigramTableSize;

            for (final Map.Entry<Long, Integer> entry : dedupBigrams.entrySet()) {
                final long key = entry.getKey();
                final int w1Rel = (int) (key >>> 32);
                final int w2Rel = (int) (key & 0xFFFFFFFFL);
                final int freq = entry.getValue();
                compiledBigrams.add(new CompiledBigram(stringPoolStart + w1Rel, stringPoolStart + w2Rel, freq));
            }

            // Sort: word1Offset ascending, frequency descending, word2Offset ascending
            Collections.sort(compiledBigrams, (a, b) -> {
                if (a.word1Offset != b.word1Offset) {
                    return Integer.compare(a.word1Offset, b.word1Offset);
                }
                if (a.frequency != b.frequency) {
                    return Integer.compare(b.frequency, a.frequency);
                }
                return Integer.compare(a.word2Offset, b.word2Offset);
            });
        }

        final int bigramTableSize = compiledBigrams.size() * 12;
        final int stringPoolStart = bigramTableOffset + bigramTableSize;
        final byte[] stringPoolBytes = stringPoolStream.toByteArray();

        final ByteBuffer buffer = ByteBuffer.allocate(stringPoolStart + stringPoolBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Header (32 bytes):
        // Magic "SKDB" (0x42444B53), Version 2, wordCount, rootOffset, bigramCount, bigramTableOffset, stringPoolOffset, reserved
        buffer.putInt(0x42444B53);
        buffer.putInt(2);
        buffer.putInt(words.size());
        buffer.putInt(root.offset);
        buffer.putInt(compiledBigrams.size());
        buffer.putInt(bigramTableOffset);
        buffer.putInt(stringPoolStart);
        buffer.putInt(0);

        // Sort nodes by offset to write sequentially
        Collections.sort(allNodes, (a, b) -> Integer.compare(a.offset, b.offset));

        for (final BuildNode node : allNodes) {
            final short charVal = (short) node.character;
            byte flags = 0;
            if (node.isTerminal) flags |= 1;
            if (!node.childrenList.isEmpty()) flags |= 2;

            final byte freq = (byte) (node.frequency & 0xFF);
            final byte childCount = (byte) Math.min(node.childrenList.size(), 255);
            final int childrenOffset = !node.childrenList.isEmpty() ? node.childrenList.get(0).offset : 0;
            final Integer relWordOffset = (node.isTerminal && node.word != null)
                    ? wordToOffset.get(node.word.toLowerCase()) : null;
            final int wordOffset = relWordOffset != null ? stringPoolStart + relWordOffset : 0;

            buffer.putShort(charVal);
            buffer.put(flags);
            buffer.put(freq);
            buffer.put(childCount);
            buffer.put((byte) 0); // 3 pad bytes
            buffer.put((byte) 0);
            buffer.put((byte) 0);
            buffer.putInt(childrenOffset);
            buffer.putInt(wordOffset);
        }

        // Bigram table entries (12 bytes each)
        for (final CompiledBigram bg : compiledBigrams) {
            buffer.putInt(bg.word1Offset);
            buffer.putInt(bg.word2Offset);
            buffer.putShort((short) (bg.frequency & 0xFFFF));
            buffer.putShort((short) 0); // pad/reserved
        }

        // String pool
        buffer.put(stringPoolBytes);

        final File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(buffer.array(), 0, buffer.position());
            fos.flush();
            fos.getFD().sync();
        }
    }
}
