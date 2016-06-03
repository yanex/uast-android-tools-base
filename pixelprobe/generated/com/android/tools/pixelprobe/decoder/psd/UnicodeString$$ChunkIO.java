package com.android.tools.pixelprobe.decoder.psd;

import com.android.tools.chunkio.ChunkUtils;
import com.android.tools.chunkio.RangedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedList;

final class UnicodeString$$ChunkIO {
    static UnicodeString read(RangedInputStream in, LinkedList<Object> stack) throws IOException {
        UnicodeString unicodeString = new UnicodeString();
        stack.addFirst(unicodeString);

        int size = 0;
        long byteCount = 0;

        unicodeString.length = in.readInt() & 0xffffffffL;
        byteCount = unicodeString.length * 2;
        unicodeString.value = ChunkUtils.readString(in, byteCount, Charset.forName("UTF-16"));

        stack.removeFirst();
        return unicodeString;
    }
}