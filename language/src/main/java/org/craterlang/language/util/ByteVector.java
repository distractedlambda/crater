package org.craterlang.language.util;

import java.util.Arrays;

public final class ByteVector {
    private static final byte[] EMPTY_BYTES = new byte[0];

    private byte[] bytes;
    private int size;

    public ByteVector() {
        bytes = EMPTY_BYTES;
        size = 0;
    }

    public ByteVector(int initialCapacity) {
        bytes = new byte[initialCapacity];
        size = 0;
    }

    public int getSize() {
        return size;
    }

    private void growByteArray(int minCapacity) {
        var newCapacity = Math.max(minCapacity, bytes.length + bytes.length / 2);
        bytes = Arrays.copyOf(bytes, newCapacity);
    }

    public void reserveCapacity(int minCapacity) {
        if (minCapacity > bytes.length) {
            growByteArray(minCapacity);
        }
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(bytes, size);
    }

    public void add(byte b) {
        reserveCapacity(size + 1);
        bytes[size++] = b;
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof ByteVector other)) {
            return false;
        }

        return size == other.size && Arrays.equals(bytes, 0, size, other.bytes, 0, size);
    }

    @Override public int hashCode() {
        var code = size;

        for (var i = 0; i < size; i++) {
            code = 31 * code + bytes[i];
        }

        return code;
    }
}
