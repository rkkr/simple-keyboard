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

public final class BanglaPhoneticComposer {
    public interface Output {
        void show(String bangla);   // called with setComposingText text
        void finish();              // called when the buffer ends (finishComposingText)
    }

    private final Output mOutput;
    private final StringBuilder mBuffer = new StringBuilder();
    private int mRenderedLength = 0;

    public BanglaPhoneticComposer(Output output) {
        mOutput = output;
    }

    public boolean handleCharacter(int codePoint) {
        if (!isConsumable(codePoint)) {
            return false;
        }
        mBuffer.appendCodePoint(codePoint);
        final String bangla = BanglaPhoneticEngine.transliterate(mBuffer.toString());
        mRenderedLength = bangla == null ? 0 : bangla.length();
        mOutput.show(bangla);
        return true;
    }

    public boolean handleBackspace() {
        if (mBuffer.length() == 0) {
            return false;
        }
        mBuffer.deleteCharAt(mBuffer.length() - 1);
        if (mBuffer.length() == 0) {
            mRenderedLength = 0;
            mOutput.finish();
        } else {
            final String bangla = BanglaPhoneticEngine.transliterate(mBuffer.toString());
            mRenderedLength = bangla == null ? 0 : bangla.length();
            mOutput.show(bangla);
        }
        return true;
    }

    public void finish() {
        if (mBuffer.length() > 0) {
            // Clear state first: finishComposingText can synchronously re-enter selection callbacks.
            mBuffer.setLength(0);
            mRenderedLength = 0;
            mOutput.finish();
        }
    }

    public void reset() {
        finish();
    }

    public boolean isComposing() {
        return mBuffer.length() > 0;
    }

    /** Length of the composing text last shown, for validating selection callbacks. */
    public int composedLength() {
        return mRenderedLength;
    }

    private static boolean isConsumable(int codePoint) {
        if (codePoint >= 'a' && codePoint <= 'z') return true;
        if (codePoint >= 'A' && codePoint <= 'Z') return true;
        if (codePoint >= '0' && codePoint <= '9') return true;
        return codePoint == '.' || codePoint == ',' || codePoint == '$' || codePoint == '^'
                || codePoint == ':' || codePoint == '%' || codePoint == '/' || codePoint == '`'
                || codePoint == ';' || codePoint == '!' || codePoint == '?';
    }
}
