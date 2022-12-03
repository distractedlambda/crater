package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;

public final class CraterTable extends DynamicObject implements TruffleObject {
    public CraterTable(Shape shape) {
        super(shape);
    }

    boolean hasOptimizedArray(DynamicObjectLibrary tables) {
        return (tables.getShapeFlags(this) & OPTIMIZED_ARRAY_FLAG) != 0;
    }

    Object getOptimizedArrayStorage(DynamicObjectLibrary tables) {
        return tables.getOrDefault(this, OPTIMIZED_ARRAY_STORAGE_KEY, null);
    }

    long getOptimizedArrayLength(DynamicObjectLibrary tables) {
        try {
            return tables.getLongOrDefault(this, OPTIMIZED_ARRAY_LENGTH_KEY, null);
        }
        catch (UnexpectedResultException exception) {
            throw shouldNotReachHere(exception);
        }
    }

    private static final int OPTIMIZED_ARRAY_FLAG = 0x01;

    private static final HiddenKey LENGTH_KEY = new HiddenKey("length");
    private static final HiddenKey OPTIMIZED_ARRAY_STORAGE_KEY = new HiddenKey("optimizedSequenceStorage");
    private static final HiddenKey OPTIMIZED_ARRAY_LENGTH_KEY = new HiddenKey("optimizedSequenceLength");

    @GenerateUncached
    public static abstract class GetMetatableNode extends CraterNode {
        public abstract Object execute(CraterTable table);

        @Specialization(guards = "table.getShape() == cachedShape")
        Object doConstantShape(CraterTable table, @Cached("table.getShape()") Shape cachedShape) {
            return cachedShape.getDynamicType();
        }

        @Specialization(replaces = "doConstantShape")
        Object doDynamicShape(CraterTable table) {
            return table.getShape().getDynamicType();
        }
    }

    @GenerateUncached
    public static abstract class SetMetatableNode extends CraterNode {
        public abstract void execute(CraterTable table, Object metatable);

        @Specialization(limit = "3")
        void doExecute(CraterTable table, Object metatable, @CachedLibrary("table") DynamicObjectLibrary tables) {
            tables.setDynamicType(table, metatable);
        }
    }

    @GenerateUncached
    public static abstract class RawLengthNode extends CraterNode {
        public abstract long execute(CraterTable table);

        @Specialization(limit = "3")
        long doExecute(CraterTable table, @CachedLibrary("table") DynamicObjectLibrary tables) {
            try {
                return tables.getLongOrDefault(table, LENGTH_KEY, 0);
            }
            catch (UnexpectedResultException exception) {
                throw shouldNotReachHere(exception);
            }
        }
    }

    @GenerateUncached
    public static abstract class RawGetNode extends CraterNode {
        public abstract Object execute(CraterTable table, Object key);

        @Specialization
        Object doExecute(
            CraterTable table,
            Object key,
            @Cached NormalizeKeyNode normalizeKeyNode,
            @Cached RawGetWithNormalizedKeyNode rawGetWithNormalizedKeyNode
        ) {
            return rawGetWithNormalizedKeyNode.execute(table, normalizeKeyNode.execute(key));
        }
    }

    @GenerateUncached
    static abstract class RawGetWithNormalizedKeyNode extends CraterNode {
        abstract Object execute(CraterTable table, Object normalizedKey);

        @Specialization(limit = "3")
        Object doExecute(
            CraterTable table,
            Object key,
            @CachedLibrary("table") DynamicObjectLibrary tables,
            @Cached RawGetWithLibraryNode rawGetWithLibraryNode
        ) {
            return rawGetWithLibraryNode.execute(table, key, tables);
        }
    }

    @GenerateUncached
    static abstract class RawGetWithLibraryNode extends CraterNode {
        abstract Object execute(CraterTable table, Object normalizedKey, DynamicObjectLibrary tables);

        @Specialization(guards = {"table.hasOptimizedArray(tables)", "index > 0"})
        Object doOptimizedArray(
            CraterTable table,
            long index,
            DynamicObjectLibrary tables,
            @Cached("createCountingProfile()") ConditionProfile inBoundsProfile,
            @Cached ReadOptimizedArrayElementNode readOptimizedArrayElementNode
        ) {
            var length = table.getOptimizedArrayLength(tables);
            if (inBoundsProfile.profile(index <= length)) {
                var storage = table.getOptimizedArrayStorage(tables);
                return readOptimizedArrayElementNode.execute(storage, index);
            }
            else {
                return CraterNil.getInstance();
            }
        }

        @Fallback
        Object doOther(CraterTable table, Object key, DynamicObjectLibrary tables) {
            return tables.getOrDefault(table, key, CraterNil.getInstance());
        }
    }

    @GenerateUncached
    static abstract class ReadOptimizedArrayElementNode extends CraterNode {
        abstract Object execute(Object storage, long index);

        @Specialization
        boolean doBooleans(boolean[] storage, long index) {
            return storage[(int) index];
        }

        @Specialization
        long doLongs(long[] storage, long index) {
            return storage[(int) index];
        }

        @Specialization
        double doDoubles(double[] storage, long index) {
            return storage[(int) index];
        }

        @Specialization
        Object doObjects(Object[] storage, long index) {
            return storage[(int) index];
        }
    }

    @GenerateUncached
    @ImportStatic(CraterMath.class)
    static abstract class NormalizeKeyNode extends CraterNode {
        abstract Object execute(Object key);

        @Specialization(guards = "hasExactLongValue(key)")
        long doDoubleToLong(double key) {
            return (long) key;
        }

        @Fallback
        Object doPassThrough(Object key) {
            return key;
        }
    }
}
