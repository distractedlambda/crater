package org.craterlang.language.util;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.lang.ref.WeakReference;

@SuppressWarnings("unchecked")
public final class InternedSet<E> {
    private Entry<E>[] table;

    public InternedSet(int initialCapacity) {
        table = new Entry[initialCapacity];
    }

    @TruffleBoundary
    public E intern(E candidate) {
        var hashCode = redistributeHashCode(candidate.hashCode());
        var table = this.table;
        var tableIndex = hashCode & (table.length - 1);
        var falseCollisionCount = 0;

        for (Entry<E> priorEntry = null, entry = table[tableIndex]; entry != null; entry = entry.next) {
            var value = entry.get();

            if (value == null) {
                if (priorEntry != null) {
                    priorEntry.next = entry.next;
                }
                else {
                    table[tableIndex] = entry.next;
                }

                continue;
            }

            priorEntry = entry;

            if (entry.hashCode != hashCode) {
                falseCollisionCount++;
                continue;
            }

            if (candidate == value || candidate.equals(value)) {
                return value;
            }
        }

        if (falseCollisionCount > MAX_FALSE_COLLISIONS && table.length < MAX_TABLE_SIZE) {
            table = this.table = growTable(table);
            tableIndex = hashCode & (table.length - 1);
        }

        var newEntry = new Entry<>(candidate, hashCode);
        newEntry.next = table[tableIndex];
        table[tableIndex] = newEntry;

        return candidate;
    }

    private static <E> Entry<E>[] growTable(Entry<E>[] table) {
        Entry<E>[] newTable = new Entry[table.length * 2];

        for (var entry : table) {
            for (Entry<E> next; entry != null; entry = next) {
                next = entry.next;

                if (entry.get() == null) {
                    continue;
                }

                var i = entry.hashCode & (newTable.length - 1);
                entry.next = newTable[i];
                newTable[i] = entry;
            }
        }

        return newTable;
    }

    // https://github.com/skeeto/hash-prospector/issues/19
    private static int redistributeHashCode(int x) {
        x ^= x >>> 16;
        x *= 0x21f0aaad;
        x ^= x >>> 15;
        x *= 0xd35a2d97;
        x ^= x >>> 15;
        return x;
    }

    private static final class Entry<E> extends WeakReference<E> {
        private final int hashCode;
        private Entry<E> next;

        public Entry(E value, int hashCode) {
            super(value);
            this.hashCode = hashCode;
        }
    }

    private static final int MAX_FALSE_COLLISIONS = 3;
    private static final int MAX_TABLE_SIZE = 1024 * 1024 * 64;
}
