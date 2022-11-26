package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.TruffleObject;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class CraterString implements TruffleObject {
    @CompilationFinal(dimensions = 1) private final byte[] bytes;
    private int hashCode;

    private CraterString(byte[] bytes) {
        assert bytes != null;
        this.bytes = bytes;
    }

    public static CraterString wrapping(byte[] bytes) {
        return new CraterString(bytes);
    }

    @TruffleBoundary
    public static CraterString fromJavaString(String string) {
        return new CraterString(string.getBytes(StandardCharsets.UTF_8));
    }

    public int getLength() {
        return bytes.length;
    }

    public byte getByte(int index) {
        return bytes[index];
    }

    public byte[] getInternalByteArray() {
        return bytes;
    }

    @TruffleBoundary(allowInlining = true)
    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof CraterString other)) {
            return false;
        }

        if (hashCode != 0 && other.hashCode != 0 && hashCode != other.hashCode) {
            return false;
        }

        return Arrays.equals(bytes, other.bytes);
    }

    @TruffleBoundary(allowInlining = true)
    @Override public int hashCode() {
        if (hashCode != 0) {
            return hashCode;
        }
        else {
            return (hashCode = computeHashCode(bytes));
        }
    }

    @TruffleBoundary
    private static int computeHashCode(byte[] bytes) {
        var code = CraterHashing.hash(bytes);
        return code != 0 ? code : -1;
    }
}
