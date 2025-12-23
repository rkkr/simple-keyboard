package rkr.simplekeyboard.inputmethod.latin;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PinyinDecoder {
    private static PinyinDecoder sInstance;
    private final Map<String, String> mPinyinMap = new HashMap<>();

    public static synchronized PinyinDecoder getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PinyinDecoder(context);
        }
        return sInstance;
    }

    private PinyinDecoder(Context context) {
        loadMapping(context);
    }

    private void loadMapping(Context context) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open("pinyin_mapping.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length >= 2) {
                    mPinyinMap.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> decode(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> results = new ArrayList<>();

        // 1. Exact match
        if (mPinyinMap.containsKey(pinyin)) {
            addCharsToList(mPinyinMap.get(pinyin), results);
        }

        // 2. Prefix match (if exact match provides few results)
        // Iterate over map keys to find those starting with pinyin.
        // This is O(N) where N is map size (~400), so it's very fast.
        for (Map.Entry<String, String> entry : mPinyinMap.entrySet()) {
            if (entry.getKey().startsWith(pinyin) && !entry.getKey().equals(pinyin)) {
                addCharsToList(entry.getValue(), results);
            }
            // Limit results to avoid massive lists
            if (results.size() > 50)
                break;
        }

        return results;
    }

    private void addCharsToList(String chars, List<String> list) {
        for (int i = 0; i < chars.length(); i++) {
            String c = String.valueOf(chars.charAt(i));
            if (!list.contains(c)) {
                list.add(c);
            }
        }
    }
}
