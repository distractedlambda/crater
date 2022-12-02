package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterSwitchProfileNode;
import sun.misc.Unsafe;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.oracle.truffle.api.ExactMath.multiplyHighUnsigned;

/**
 * Hashing function adapted from <a href="https://github.com/tkaitchuck/aHash">aHash</a>'s fallback algorithm (as of
 * v0.8.2).
 */
public final class CraterStrings {
    private CraterStrings() {}

    public static int getLength(byte[] string) {
        return string.length - HEADER_SIZE;
    }

    public static byte getByte(byte[] string, int index) {
        return string[HEADER_SIZE + index];
    }

    @TruffleBoundary
    public static boolean equals(byte[] lhs, byte[] rhs) {
        if (lhs == rhs) {
            return true;
        }

        if (lhs.length != rhs.length) {
            return false;
        }

        var lhsHeader = getHeader(lhs);
        var rhsHeader = getHeader(rhs);

        if (lhsHeader < 0 && rhsHeader < 0) {
            // Strings are interned and not identical
            return false;
        }

        var lhsHashCode = lhsHeader & HASH_CODE_MASK;
        var rhsHashCode = rhsHeader & HASH_CODE_MASK;

        if (lhsHashCode != 0 && rhsHashCode != 0 && lhsHashCode != rhsHashCode) {
            // Strings hash differently
            return false;
        }

        return Arrays.equals(lhs, HEADER_SIZE, lhs.length, rhs, HEADER_SIZE, rhs.length);
    }

    @TruffleBoundary
    public static int hashCode(byte[] string) {
        var header = getHeader(string);
        if (header == 0) {
            return populateHashCode(string);
        }
        else {
            return header & HASH_CODE_MASK;
        }
    }

    @TruffleBoundary
    public static byte[] internedFromUtf8(byte[] utf8, InternedSet set) {
        var hashCode = utf8HashCode(utf8);
        var existing = set.findExistingByUtf8(utf8, 0, utf8.length, hashCode);
        if (existing != null) {
            return existing;
        }
        else {
            return set.insertUtf8AssumingNotPresent(utf8, 0, utf8.length, hashCode);
        }
    }

    @TruffleBoundary
    public static byte[] internedFromUtf8(byte[] utf8, ThreadLocal<InternedSet> localSet, InternedSet globalSet) {
        var hashCode = utf8HashCode(utf8);
        var localSetValue = localSet.get();

        var interned = localSetValue.findExistingByUtf8(utf8, 0, utf8.length, hashCode);

        if (interned == null) {
            synchronized (globalSet) {
                interned = globalSet.findExistingByUtf8(utf8, 0, utf8.length, hashCode);
                if (interned == null) {
                    interned = globalSet.insertUtf8AssumingNotPresent(utf8, 0, utf8.length, hashCode);
                }
            }
            localSetValue.insertStringAssumingNotPresent(interned, hashCode);
        }

        return interned;
    }

    @TruffleBoundary
    private static int utf8HashCode(byte[] utf8) {
        return hashBytes(utf8, 0, utf8.length);
    }

    static int getHeader(byte[] string) {
        assert string.length >= HEADER_SIZE;
        return UNSAFE.getInt(string, Unsafe.ARRAY_BYTE_BASE_OFFSET);
    }

    private static void setHeader(byte[] string, int newHeader) {
        assert string.length >= HEADER_SIZE;
        UNSAFE.putInt(string, Unsafe.ARRAY_BYTE_BASE_OFFSET, newHeader);
    }

    private static int populateHashCode(byte[] string) {
        var hashCode = hashBytes(string, HEADER_SIZE, string.length - HEADER_SIZE);
        setHeader(string, hashCode);
        return hashCode;
    }

    private static int hashBytes(byte[] string, int start, int size) {
        if (size > 8) {
            return hashBytesLarge(string, start, size);
        }
        else {
            return hashBytesSmall(string, start, size);
        }
    }

    private static int hashBytesLarge(byte[] string, int start, int size) {
        var accum = (HASH_KEY_0 + Integer.toUnsignedLong(size)) * HASH_MULTIPLE;
        var offset = Unsafe.ARRAY_BYTE_BASE_OFFSET + start;

        if (size > 16) {
            var tailLow = UNSAFE.getLong(string, offset + size - 16);
            var tailHigh = UNSAFE.getLong(string, offset + size - 8);
            accum = mixHashCode(accum, tailLow, tailHigh);
            do {
                var low = UNSAFE.getLong(string, offset);
                var high = UNSAFE.getLong(string, offset + 8);
                accum = mixHashCode(accum, low, high);
                offset += 16;
                size -= 16;
            } while (size > 16);
        }
        else {
            var low = UNSAFE.getLong(string, offset);
            var high = UNSAFE.getLong(string, offset + size - 8);
            accum = mixHashCode(accum, low, high);
        }

        return finalizeHashCode(accum, HASH_KEY_1);
    }

    private static int hashBytesSmall(byte[] string, int start, int size) {
        var offset = Unsafe.ARRAY_BYTE_BASE_OFFSET + start;
        long low, high;

        if (size >= 2) {
            if (size >= 4) {
                low = Integer.toUnsignedLong(UNSAFE.getInt(string, offset));
                high = Integer.toUnsignedLong(UNSAFE.getInt(string, offset + size - 4));
            }
            else {
                low = Short.toUnsignedLong(UNSAFE.getShort(string, offset));
                high = Byte.toUnsignedLong(UNSAFE.getByte(string, offset + size - 1));
            }
        }
        else {
            if (size > 0) {
                low = high = Byte.toUnsignedLong(UNSAFE.getByte(string, offset));
            }
            else {
                low = high = 0;
            }
        }

        return finalizeHashCode(
            foldedMultiply(low ^ HASH_KEY_0, high ^ HASH_KEY_3),
            HASH_KEY_1 + Integer.toUnsignedLong(size)
        );
    }

    private static int finalizeHashCode(long accum, long finalMultiplier) {
        var longHash = Long.rotateLeft(foldedMultiply(accum, finalMultiplier), (int) accum);
        var intHash = ((int) longHash | (int) (longHash >> 32)) & HASH_CODE_MASK;
        return intHash != 0 ? intHash : 1;
    }

    private static long mixHashCode(long accum, long input0, long input1) {
        var combined = foldedMultiply(input0 ^ HASH_KEY_2, input1 ^ HASH_KEY_3);
        return Long.rotateLeft((accum + HASH_KEY_1) ^ combined, HASH_ROT);
    }

    private static long foldedMultiply(long a, long b) {
        return multiplyHighUnsigned(a, b) ^ (a * b);
    }

    private static final int HASH_ROT = 23;

    private static final long HASH_MULTIPLE = 6364136223846793005L;

    private static final long HASH_KEY_0 = 0x243f_6a88_85a3_08d3L;
    private static final long HASH_KEY_1 = 0x1319_8a2e_0370_7344L;
    private static final long HASH_KEY_2 = 0xa409_3822_299f_31d0L;
    private static final long HASH_KEY_3 = 0x082e_fa98_ec4e_6c89L;

    private static final int HEADER_SIZE = 4;
    private static final int HASH_CODE_MASK = 0x7FFF_FFFF;

    public static final class InternedSet {
        private final ReferenceQueue<byte[]> queue = new ReferenceQueue<>();
        private Entry[] table = new Entry[16];
        private int overestimatedLoad;

        private void pruneEntry(Entry entry) {
            var table = this.table;
            var tableIndex = entry.valueHashCode % table.length;
            var tableEntry = table[tableIndex];

            assert tableEntry != null;

            if (tableEntry == entry) {
                table[tableIndex] = tableEntry.next;
                return;
            }

            while (tableEntry.next != entry) {
                tableEntry = tableEntry.next;
                assert tableEntry != null;
            }

            tableEntry.next = tableEntry.next.next;
        }

        private void prune() {
            Entry entry;
            while ((entry = (Entry) queue.poll()) != null) {
                pruneEntry(entry);
                overestimatedLoad--;
            }
        }

        private void growTable() {
            var newTable = new Entry[table.length * 2];

            for (var entry : table) {
                while (entry != null) {
                    var nextEntry = entry.next;

                    var newTableIndex = entry.valueHashCode & (newTable.length - 1);
                    entry.next = newTable[newTableIndex];
                    newTable[newTableIndex] = entry;

                    entry = nextEntry;
                }
            }

            table = newTable;
        }

        private byte[] findExistingByUtf8(byte[] utf8, int utf8Start, int utf8Size, int utf8HashCode) {
            var tableIndex = utf8HashCode & (table.length - 1);

            for (var entry = table[tableIndex]; entry != null; entry = entry.next) {
                if (entry.valueHashCode == utf8HashCode) {
                    var entryValue = entry.get();
                    if (
                        entryValue != null && Arrays.equals(
                            entryValue, HEADER_SIZE, entryValue.length - HEADER_SIZE,
                            utf8, utf8Start, utf8Size
                        )
                    ) {
                        return entryValue;
                    }
                }
            }

            return null;
        }

        private byte[] insertUtf8AssumingNotPresent(byte[] utf8, int utf8Start, int utf8Size, int utf8HashCode) {
            var string = new byte[utf8Size + HEADER_SIZE];
            System.arraycopy(utf8, utf8Start, string, HEADER_SIZE, utf8Size);
            setHeader(string, utf8HashCode | (1 << 31));
            insertStringAssumingNotPresent(string, utf8HashCode);
            return string;
        }

        private void insertStringAssumingNotPresent(byte[] string, int hashCode) {
            prune();

            if (overestimatedLoad * LOAD_FACTOR_DENOMINATOR >= table.length * LOAD_FACTOR_NUMERATOR) {
                growTable();
            }

            var tableIndex = hashCode & (table.length - 1);
            var newEntry = new Entry(string, hashCode, queue);
            newEntry.next = table[tableIndex];
            table[tableIndex] = newEntry;
            overestimatedLoad++;
        }

        private static final class Entry extends WeakReference<byte[]> {
            private final int valueHashCode;
            private Entry next;

            private Entry(byte[] element, int valueHashCode, ReferenceQueue<byte[]> queue) {
                super(element, queue);
                this.valueHashCode = valueHashCode;
            }
        }

        private static final long LOAD_FACTOR_NUMERATOR = 3;

        private static final long LOAD_FACTOR_DENOMINATOR = 4;
    }

    @GenerateUncached
    @ImportStatic(CraterStrings.class)
    public static abstract class HashCodeNode extends CraterNode {
        public abstract int execute(byte[] string);

        @Specialization(guards = "string == cachedString")
        protected int doConstant(
            byte[] string,
            @Cached(value = "string", dimensions = 0) byte[] cachedString,
            @Cached("hashCode(cachedString)") int cachedHashCode
        ) {
            return cachedHashCode;
        }

        @Specialization(guards = "header != 0", replaces = "doConstant")
        protected int doPopulatedHashCode(byte[] string, @Bind("getHeader(string)") int header) {
            return header & HASH_CODE_MASK;
        }

        @Specialization(replaces = "doPopulatedHashCode")
        protected int doGeneric(byte[] string) {
            return CraterStrings.hashCode(string);
        }
    }

    @GenerateUncached
    public static abstract class EqualsNode extends CraterNode {
        public abstract boolean execute(byte[] lhs, byte[] rhs);

        @Specialization
        protected boolean doExecute(
            byte[] lhs,
            byte[] rhs,
            @Cached CraterSwitchProfileNode profileNode
        ) {
            if (lhs == rhs) {
                profileNode.execute(0x01);
                return true;
            }

            if (lhs.length != rhs.length) {
                profileNode.execute(0x02);
                return false;
            }

            profileNode.execute(0x04);
            var lhsHeader = getHeader(lhs);
            var rhsHeader = getHeader(rhs);
            if (lhsHeader < 0 && rhsHeader < 0) {
                // Strings are interned and not identical
                profileNode.execute(0x08);
                return false;
            }

            profileNode.execute(0x10);
            var lhsHashCode = lhsHeader & HASH_CODE_MASK;
            var rhsHashCode = rhsHeader & HASH_CODE_MASK;
            if (lhsHashCode != 0 && rhsHashCode != 0 && lhsHashCode != rhsHashCode) {
                // Strings hash differently
                profileNode.execute(0x20);
                return false;
            }

            profileNode.execute(0x40);
            return boundary(lhs, rhs);
        }

        @TruffleBoundary(allowInlining = true)
        static boolean boundary(byte[] lhs, byte[] rhs) {
            return Arrays.equals(lhs, HEADER_SIZE, lhs.length, rhs, HEADER_SIZE, rhs.length);
        }
    }

    private static final Unsafe UNSAFE;

    static {
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
