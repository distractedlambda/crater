package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerDirectives.castExact;

public final class CraterTable implements TruffleObject {
    private int header = 0;
    private Object slot0 = CraterNil.getInstance();
    private Object slot1 = null;

    boolean hasMap() {
        return (header & 0x1) != 0;
    }

    boolean hasSequence() {
        return (header & 0x2) != 0;
    }

    boolean keysAreInterned() {
        return (header & 0x4) != 0;
    }

    boolean sequenceIsDisabled() {
        return (header & 0x8) != 0;
    }

    int getLength() {
        return header >>> 4;
    }

    static final int INVALID_SEQUENCE_LENGTH = 0x0F_FF_FF_FF;

    private static final class DualStorage {
        Object sequenceStorage;
        Object mapValueStorage;
    }

    @GenerateUncached
    public static abstract class GetMetatableNode extends CraterNode {
        public abstract Object execute(CraterTable table);

        @Specialization(guards = "table.hasMap()")
        protected Object doMap(CraterTable table) {
            return castExact(table.slot0, Object[].class)[0];
        }

        @Fallback
        protected Object doNonMap(CraterTable table) {
            return table.slot0;
        }
    }

    @GenerateUncached
    protected static abstract class GetSequenceStorageNode extends CraterNode {
        public abstract Object execute(CraterTable table);

        @Specialization(guards = "table.hasMap()")
        protected Object doDualStorage(CraterTable table) {
            return ((DualStorage) table.slot1).sequenceStorage;
        }

        @Fallback
        protected Object doSequenceOnly(CraterTable table) {
            return table.slot1;
        }
    }

    // @GenerateUncached
    // protected static abstract class KeyHashCodeNode extends CraterNode  {
    //     public abstract Object execute(Object key);
    // }


    // @GenerateUncached
    // public static abstract class IndexNode extends CraterNode {
    //     public abstract Object execute(CraterTable table, Object key);

    //     @Specialization(guards = {"table.hasSequence()", "index >= 1", "index <= table.getLength()"})
    //     protected Object doExecute(
    //         CraterTable table,
    //         long key,
    //         @Cached GetSequenceStorageNode getSequenceStorageNode,
    //         @Cached IndexSequenceNode indexSequenceNode
    //     ) {
    //         return indexSequenceNode.execute((int) key, getSequenceStorageNode.execute(table));
    //     }

    //     @Specialization
    //     protected Object doMapLookup(
    //         CraterTable table,
    //         Object key,
    //         @Cached @Shared("hasMapProfile") ConditionProfile hasMapProfile,
    //         @Cached IndexMapNode indexMapNode
    //     ) {
    //         Object[] keyStorage;
    //         Object valueStorage;

    //         switch (table.getTag()) {
    //             case TAG_EMPTY -> {
    //                 tagProfile.execute(TAG_EMPTY);
    //                 return CraterNil.getInstance();
    //             }

    //             case TAG_PURE_SEQUENCE -> {
    //                 tagProfile.execute(TAG_PURE_SEQUENCE);
    //                 return CraterNil.getInstance();
    //             }

    //             case TAG_PURE_MAP -> {
    //                 tagProfile.execute(TAG_PURE_MAP);
    //                 keyStorage = castExact(table.slot0, Object[].class);
    //                 valueStorage = table.slot1;
    //             }

    //             case TAG_COMBINED_SEQUENCE_AND_MAP -> {
    //                 tagProfile.execute(TAG_COMBINED_SEQUENCE_AND_MAP);
    //                 keyStorage = castExact(table.slot0, Object[].class);
    //                 valueStorage = ((ExtendedData) table.slot1).mapValueStorage;
    //             }

    //             default -> throw shouldNotReachHere();
    //         }

    //         return indexMapNode.execute(key, keyStorage, valueStorage);
    //     }

    //     @GenerateUncached
    //     protected static abstract class IndexSequenceNode extends CraterNode {
    //         protected abstract Object execute(int index, Object storage);

    //         @Specialization
    //         protected boolean doBooleans(int index, byte[] storage) {
    //             return (storage[index / 8] >> (index % 8)) != 0;
    //         }

    //         @Specialization
    //         protected long doLongs(int index, long[] storage) {
    //             return storage[index];
    //         }

    //         @Specialization
    //         protected double doDoubles(int index, double[] storage) {
    //             return storage[index];
    //         }

    //         @Specialization
    //         protected Object doObjects(int index, Object[] storage) {
    //             return storage[index];
    //         }
    //     }

    //     @GenerateUncached
    //     protected static abstract class IndexMapNode extends CraterNode {
    //         protected abstract Object execute(Object key, Object[] keyStorage, Object valueStorage);
    //     }
    // }
}
