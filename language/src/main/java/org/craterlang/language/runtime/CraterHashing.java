package org.craterlang.language.runtime;

import com.oracle.truffle.api.HostCompilerDirectives.InliningCutoff;
import sun.misc.Unsafe;

import static com.oracle.truffle.api.ExactMath.multiplyHighUnsigned;

/**
 * Implementation adapted from <a href="https://github.com/tkaitchuck/aHash">aHash</a>'s fallback algorithm (as of
 * v0.8.2).
 */
public class CraterHashing {
    private CraterHashing() {}

    public static int hash(byte[] data) {
        return hash(data, 0, data.length);
    }

    public static int hash(byte[] data, int start, int size) {
        assert (size >= 0) && (start >= 0) && (start <= data.length - size);
        if (size > 8) {
            return hashLarge(data, start, size);
        }
        else {
            return hashSmall(data, start, size);
        }
    }

    @InliningCutoff
    private static int hashLarge(byte[] data, int start, int size) {
        assert size > 8;

        var accum = (KEY_0 + Integer.toUnsignedLong(size)) * MULTIPLE;
        var offset = Unsafe.ARRAY_BYTE_BASE_OFFSET + start;

        if (size > 16) {
            var tailLow = UNSAFE.getLong(data, offset + size - 16);
            var tailHigh = UNSAFE.getLong(data, offset + size - 8);
            accum = update(accum, tailLow, tailHigh);
            do {
                var low = UNSAFE.getLong(data, offset);
                var high = UNSAFE.getLong(data, offset + 8);
                accum = update(accum, low, high);
                offset += 16;
                size -= 16;
            } while (size > 16);
        }
        else {
            var low = UNSAFE.getLong(data, offset);
            var high = UNSAFE.getLong(data, offset + size - 8);
            accum = update(accum, low, high);
        }

        return finalize(accum, KEY_1);
    }

    @InliningCutoff
    private static int hashSmall(byte[] data, int start, int size) {
        assert size <= 8;

        var offset = Unsafe.ARRAY_BYTE_BASE_OFFSET + start;
        long low, high;

        if (size >= 2) {
            if (size >= 4) {
                low = Integer.toUnsignedLong(UNSAFE.getInt(data, offset));
                high = Integer.toUnsignedLong(UNSAFE.getInt(data, offset + size - 4));
            }
            else {
                low = Short.toUnsignedLong(UNSAFE.getShort(data, offset));
                high = Byte.toUnsignedLong(UNSAFE.getByte(data, offset + size - 1));
            }
        }
        else {
            if (size > 0) {
                low = high = Byte.toUnsignedLong(UNSAFE.getByte(data, offset));
            }
            else {
                low = high = 0;
            }
        }

        return finalize(
            foldedMultiply(low ^ KEY_0, high ^ KEY_3),
            KEY_1 + Integer.toUnsignedLong(size)
        );
    }

    private static int finalize(long accum, long finalMultiplier) {
        return (int) Long.rotateLeft(foldedMultiply(accum, finalMultiplier), (int) accum);
    }

    private static long update(long accum, long input0, long input1) {
        var combined = foldedMultiply(input0 ^ KEY_2, input1 ^ KEY_3);
        return Long.rotateLeft((accum + KEY_1) ^ combined, ROT);
    }

    private static long foldedMultiply(long a, long b) {
        return multiplyHighUnsigned(a, b) ^ (a * b);
    }

    private static final int ROT = 23;

    private static final long MULTIPLE = 6364136223846793005L;

    private static final long KEY_0 = 0x243f_6a88_85a3_08d3L;
    private static final long KEY_1 = 0x1319_8a2e_0370_7344L;
    private static final long KEY_2 = 0xa409_3822_299f_31d0L;
    private static final long KEY_3 = 0x082e_fa98_ec4e_6c89L;

    private static final Unsafe UNSAFE;

    static {
        if (Unsafe.ARRAY_BYTE_INDEX_SCALE != 1) {
            throw new UnsupportedOperationException("This class requires that byte arrays are contiguous");
        }

        Unsafe unsafe;

        try {
            unsafe = Unsafe.getUnsafe();
        }
        catch (SecurityException securityException) {
            try {
                var unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = (Unsafe) unsafeField.get(null);
            }
            catch (NoSuchFieldException | SecurityException | IllegalAccessException exception) {
                exception.addSuppressed(securityException);
                throw new UnsupportedOperationException("This class requires access to sun.misc.Unsafe", exception);
            }
        }

        UNSAFE = unsafe;
    }
}
