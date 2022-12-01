package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.craterlang.language.CraterNode;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.System.identityHashCode;

public final class CraterTable implements TruffleObject {
    private int header = 0;
    private Object metatableOrKeys = CraterNil.getInstance();
    private Object mapValues = null;
    private Object sequenceValues = null;

    boolean sequenceOptimizationIsDisabled() {
        return (header & 0x1) != 0;
    }

    boolean keysAreInterned() {
        return (header & 0x2) != 0;
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
        protected static abstract class ExtractNode extends CraterNode {
            protected abstract Object execute(Object metatableOrKeys);

            @Specialization
            protected Object doKeys(Object[] keys) {
                return keys[0];
            }

            @Fallback
            protected Object doMetatable(Object metatable) {
                return metatable;
            }
        }
    }

    @GenerateUncached
    public static abstract class NormalizeKeyNode extends CraterNode {
        public abstract Object execute(Object key);

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected long doDoubleWithExactLongValue(double key) throws UnexpectedResultException {
            return CraterMath.expectExactLongValue(key);
        }

        @Specialization(replaces = "doDoubleWithExactLongValue")
        protected Object doDouble(double key) {
            return CraterMath.coerceExactLongValue(key);
        }

        @Fallback
        protected Object doOther(Object value) {
            return value;
        }
    }

    @GenerateUncached
    static abstract class KeyHashNode extends CraterNode {
        public abstract int execute(Object key);

        @Specialization
        protected int doBoolean(boolean key) {
            return key ? TRUE_HASH_CODE : FALSE_HASH_CODE;
        }

        @Specialization
        protected int doLong(long key) {
            return hash64(key);
        }

        @Specialization
        protected int doDouble(double key) {
            return hash64(doubleToRawLongBits(key));
        }

        @Specialization
        protected int doString(byte[] key, @Cached CraterStrings.HashCodeNode hashCodeNode) {
            return hashCodeNode.execute(key);
        }

        @Fallback
        protected int doIdentity(Object key) {
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
