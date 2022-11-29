package org.craterlang.language;

import org.craterlang.language.runtime.CraterStrings;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

public final class CraterInternedStringTable {
    private final ReferenceQueue<CraterStrings> queue = new ReferenceQueue<>();
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

    public CraterStrings findExisting(CraterStrings string) {
        var hashCode = string.hashCode();
        var tableIndex = hashCode & (table.length - 1);

        for (var entry = table[tableIndex]; entry != null; entry = entry.next) {
            if (entry.valueHashCode == hashCode) {
                var entryValue = entry.get();
                if (string.equals(entryValue)) {
                    return entryValue;
                }
            }
        }

        return null;
    }

    public void insertAssumingNotPresent(CraterStrings string) {
        prune();

        if (overestimatedLoad * LOAD_FACTOR_DENOMINATOR >= table.length * LOAD_FACTOR_NUMERATOR) {
            growTable();
        }

        var hashCode = string.hashCode();
        var tableIndex = hashCode & (table.length - 1);
        var newEntry = new Entry(string, hashCode, queue);
        newEntry.next = table[tableIndex];
        table[tableIndex] = newEntry;
        overestimatedLoad++;
    }

    private static final class Entry extends WeakReference<CraterStrings> {
        private final int valueHashCode;
        private Entry next;

        private Entry(CraterStrings element, int valueHashCode, ReferenceQueue<CraterStrings> queue) {
            super(element, queue);
            this.valueHashCode = valueHashCode;
        }
    }

    private static final long LOAD_FACTOR_NUMERATOR = 3;

    private static final long LOAD_FACTOR_DENOMINATOR = 4;
}
