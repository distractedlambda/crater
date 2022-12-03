package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerDirectives.castExact;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.System.identityHashCode;

public final class CraterTable implements TruffleObject {
    private int header = 0;
    private Object metatableOrKeys = CraterNil.getInstance();
    private Object mapValues = null;
    private Object optimizedSequenceValues = null;

    boolean hasMap() {
        return metatableOrKeys.getClass() == Object[].class;
    }

    boolean hasOptimizedSequence() {
        return optimizedSequenceValues != null;
    }

    boolean sequenceOptimizationIsDisabled() {
        return (header & 0x1) != 0;
    }

    boolean keysAreInterned() {
        return (header & 0x2) != 0;
    }

    public Object[] getKeys() {
        return castExact(metatableOrKeys, Object[].class);
    }

    int getLength() {
        return header >>> HEADER_LENGTH_SHIFT;
    }

    static final int HEADER_LENGTH_SHIFT = 2;
    static final int INVALID_SEQUENCE_LENGTH = -1 >>> HEADER_LENGTH_SHIFT;

    @GenerateUncached
    public static abstract class GetMetatableNode extends CraterNode {
        public abstract Object execute(CraterTable table);

        @Specialization
        Object doExecute(CraterTable table, @Cached ExtractNode extractNode) {
            return extractNode.execute(table);
        }

        @GenerateUncached
        static abstract class ExtractNode extends CraterNode {
            abstract Object execute(Object metatableOrKeys);

            @Specialization
            Object doKeys(Object[] keys) {
                return keys[0];
            }

            @Fallback
            Object doMetatable(Object metatable) {
                return metatable;
            }
        }
    }

    @GenerateUncached
    @ImportStatic(Double.class)
    public static abstract class RawIndexNode extends CraterNode {
        public abstract Object execute(CraterTable table, Object key);

        @Specialization
        CraterNil doNilKey(CraterTable table, CraterNil key) {
            return CraterNil.getInstance();
        }

        @Specialization(guards = "isNaN(key)")
        CraterNil doNanKey(CraterTable table, double key) {
            return CraterNil.getInstance();
        }

        @Fallback
        Object doNormalizedKey(
            CraterTable table,
            Object key,
            @Cached NormalizeKeyNode normalizeKeyNode,
            @Cached RawIndexWithNormalizedKeyNode withNormalizedKeyNode
        ) {
            return withNormalizedKeyNode.execute(table, normalizeKeyNode.execute(key));
        }
    }

    @GenerateUncached
    static abstract class RawIndexWithNormalizedKeyNode extends CraterNode {
        abstract Object execute(CraterTable table, Object normalizedKey);

        @Specialization(guards = {"table.hasOptimizedSequence()", "key > 0", "key <= table.getLength()"})
        Object doOptimizedSequence(
            CraterTable table,
            long key,
            @Cached IndexVectorNode optimizedSequenceNode
        ) {
            return optimizedSequenceNode.execute(table.optimizedSequenceValues, (int) key);
        }

        @Fallback
        Object doMap(CraterTable table, Object key) {
            throw new UnsupportedOperationException("TODO");
        }
    }

    @GenerateUncached
    static abstract class IndexVectorNode extends CraterNode {
        abstract Object execute(Object optimizedSequenceValues, int index);

        @Specialization
        boolean doBooleans(boolean[] values, int index) {
            return values[index];
        }

        @Specialization
        long doLongs(long[] values, int index) {
            return values[index];
        }

        @Specialization
        double doDoubles(double[] values, int index) {
            return values[index];
        }

        @Specialization
        Object doGeneric(Object[] values, int index) {
            return values[index];
        }
    }

    @GenerateUncached
    static abstract class FindEntryNode extends CraterNode {
        abstract int execute(CraterTable table, Object key);

        @Specialization(guards = "!table.hasMap()")
        int doEmpty(CraterTable table, Object key) {
            return -1;
        }

        @Specialization(guards = "table.hasMap()")
        int doBooleanKey(CraterTable table, boolean key) {

        }
    }

    @GenerateUncached
    static abstract class FindDynamicEntryNode extends CraterNode {
        abstract int execute(Object[] keys, Object key);
    }

    // @GenerateUncached
    // protected static abstract class RawIndexMapNode extends CraterNode {
    //     protected abstract Object execute(CraterTable table, Object normalizedKey);

    //     @Specialization(guards = "!table.hasMap()")
    //     protected CraterNil doEmpty(CraterTable table, Object key) {
    //         return CraterNil.getInstance();
    //     }

    //     @Fallback
    //     protected Object doNonEmpty(
    //         CraterTable table,
    //         Object key,
    //         @Cached KeyHashNode keyHashNode,
    //         @Cached RawIndexNonEmptyMapNode rawIndexNonEmptyMapNode
    //     ) {
    //         return rawIndexNonEmptyMapNode.execute(table, key, keyHashNode.execute(key));
    //     }
    // }

    // @GenerateUncached
    // protected static abstract class RawIndexNonEmptyMapNode extends CraterNode {
    //     protected abstract Object execute(CraterTable table, Object key, int hashCode);

    //     @Specialization(guards = {"table.keysAreInterned()", "keys == cachedKeys"})
    //     protected Object doInternedKeys(
    //         CraterTable table,
    //         @Cached(value = "table.getKeys()", dimensions = 1) Object[] cachedKeys,
    //
    //     )
    // }

    @GenerateUncached
    static abstract class NormalizeKeyNode extends CraterNode {
        abstract Object execute(Object key);

        @Specialization(rewriteOn = UnexpectedResultException.class)
        long doDoubleWithExactLongValue(double key) throws UnexpectedResultException {
            return CraterMath.expectExactLongValue(key);
        }

        @Specialization(replaces = "doDoubleWithExactLongValue")
        Object doDouble(double key) {
            return CraterMath.coerceExactLongValue(key);
        }

        @Fallback
        Object doOther(Object value) {
            return value;
        }
    }

    @GenerateUncached
    static abstract class KeyHashNode extends CraterNode {
        abstract int execute(Object key);

        @Specialization
        int doBoolean(boolean key) {
            return key ? TRUE_HASH_CODE : FALSE_HASH_CODE;
        }

        @Specialization
        int doLong(long key) {
            return hash64(key);
        }

        @Specialization
        int doDouble(double key) {
            return hash64(doubleToRawLongBits(key));
        }

        @Fallback
        int doIdentity(Object key) {
            return identityHashCode(key);
        }

        private static int hash64(long bits) {
            return scramble32((int) bits ^ (int) (bits >>> 32));
        }

        // https://github.com/skeeto/hash-prospector/issues/19
        private static int scramble32(int x) {
            x ^= x >>> 16;
            x *= 0x21f0aaad;
            x ^= x >>> 15;
            x *= 0xd35a2d97;
            x ^= x >>> 15;
            return x;
        }

        private static final int FALSE_HASH_CODE = scramble32(0);
        private static final int TRUE_HASH_CODE = scramble32(1);
    }
}
