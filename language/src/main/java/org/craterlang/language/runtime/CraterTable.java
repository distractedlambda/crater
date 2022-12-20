package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.craterlang.language.CraterLanguage;
import org.craterlang.language.CraterNode;
import sun.misc.Unsafe;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;
import static com.oracle.truffle.api.CompilerAsserts.partialEvaluationConstant;
import static java.lang.System.identityHashCode;
import static org.craterlang.language.runtime.UnsafeAccess.getBooleanUnchecked;
import static org.craterlang.language.runtime.UnsafeAccess.getDoubleUnchecked;
import static org.craterlang.language.runtime.UnsafeAccess.getLongUnchecked;
import static org.craterlang.language.runtime.UnsafeAccess.getUnchecked;

public final class CraterTable implements TruffleObject {
    private Shape shape;
    private Object metatable;
    private byte[] primitiveMemberStorage;
    private Object[] objectMemberStorage;
    private Object sequenceStorage;
    private int cachedLength;

    public CraterTable(Shape shape) {
        this.shape = shape;
    }

    Shape getShape() {
        return shape;
    }

    public Object getMetatable() {
        return metatable;
    }

    public void setMetatable(Object metatable) {
        assert metatable != null;
        this.metatable = metatable;
    }

    boolean hasCachedLength() {
        return cachedLength >= 0;
    }

    int getCachedLength() {
        assert hasCachedLength();
        return cachedLength;
    }

    public static sealed abstract class Shape {
        private CachedMemberAddition[] cachedMemberAdditions;
        private CachedMemberRemoval[] cachedMemberRemovals;

        abstract AddedMember addMemberWithoutCache(Object key, byte type);

        abstract Shape removeMemberWithoutCache(Object key);

        abstract int getMemberLocation(Object key);

        @TruffleBoundary
        private AddedMember addMember(Object key, byte type) {
            int freeCacheSlot;

            if (cachedMemberAdditions == null) {
                cachedMemberAdditions = new CachedMemberAddition[4];
                freeCacheSlot = 0;
            }
            else {
                freeCacheSlot = -1;

                for (var i = 0; i < cachedMemberAdditions.length; i++) {
                    var entry = cachedMemberAdditions[i];
                    if (entry != null) {
                        var entryShape = entry.get();
                        if (entryShape == null) {
                            cachedMemberAdditions[i] = null;
                            if (freeCacheSlot < 0) {
                                freeCacheSlot = i;
                            }
                        }
                        else if (key.equals(entry.key) && type == (entry.location & 0b11)) {
                            return new AddedMember(entryShape, entry.location);
                        }
                    }
                    else if (freeCacheSlot < 0) {
                        freeCacheSlot = i;
                    }
                }

                if (freeCacheSlot < 0) {
                    freeCacheSlot = cachedMemberAdditions.length;
                    cachedMemberAdditions = Arrays.copyOf(cachedMemberAdditions, cachedMemberAdditions.length * 2);
                }
            }

            var uninternedResult = addMemberWithoutCache(key, type);
            var shape = CraterLanguage.get(null).getInternedTableShape(uninternedResult.shape);
            cachedMemberAdditions[freeCacheSlot] = new CachedMemberAddition(shape, key, uninternedResult.location);
            return new AddedMember(shape, uninternedResult.location);
        }

        @TruffleBoundary
        private Shape removeMember(Object key) {
            int freeCacheSlot;

            if (cachedMemberRemovals == null) {
                cachedMemberRemovals = new CachedMemberRemoval[4];
                freeCacheSlot = 0;
            }
            else {
                freeCacheSlot = -1;

                for (var i = 0; i < cachedMemberRemovals.length; i++) {
                    var entry = cachedMemberRemovals[i];
                    if (entry != null) {
                        var entryShape = entry.get();
                        if (entryShape == null) {
                            cachedMemberRemovals[i] = null;
                            if (freeCacheSlot < 0) {
                                freeCacheSlot = i;
                            }
                        }
                        else if (key.equals(entry.key)) {
                            return entryShape;
                        }
                    }
                    else if (freeCacheSlot < 0) {
                        freeCacheSlot = i;
                    }
                }

                if (freeCacheSlot < 0) {
                    freeCacheSlot = cachedMemberRemovals.length;
                    cachedMemberRemovals = Arrays.copyOf(cachedMemberRemovals, cachedMemberRemovals.length * 2);
                }
            }

            var shape = CraterLanguage.get(null).getInternedTableShape(removeMemberWithoutCache(key));
            cachedMemberRemovals[freeCacheSlot] = new CachedMemberRemoval(shape, key);
            return shape;
        }

        @ValueType
        record AddedMember(Shape shape, int location) {}

        private static final class CachedMemberAddition extends WeakReference<Shape> {
            private final Object key;
            private final int location;

            private CachedMemberAddition(Shape resultingShape, Object key, int location) {
                super(resultingShape);
                this.key = key;
                this.location = location;
            }
        }

        private static final class CachedMemberRemoval extends WeakReference<Shape> {
            private final Object key;

            private CachedMemberRemoval(Shape resultingShape, Object key) {
                super(resultingShape);
                this.key = key;
            }
        }

        private static final byte TYPE_BOOLEAN = 0;
        private static final byte TYPE_LONG = 1;
        private static final byte TYPE_DOUBLE = 2;
        private static final byte TYPE_OBJECT = 3;
    }

    private static final class RootShape extends Shape {
        @Override AddedMember addMemberWithoutCache(Object key, byte type) {
            neverPartOfCompilation();
            return new AddedMember(new AddedMemberShape(this, key, type), type);
        }

        @Override Shape removeMemberWithoutCache(Object key) {
            neverPartOfCompilation();
            throw new UnsupportedOperationException();
        }

        @Override int getMemberLocation(Object key) {
            neverPartOfCompilation();
            return -1;
        }
    }

    private static final class AddedMemberShape extends Shape {
        private final Shape base;
        private final Object key;
        private final int location;

        private AddedMemberShape(Shape base, Object key, int location) {
            this.base = base;
            this.key = key;
            this.location = location;
        }

        @Override AddedMember addMemberWithoutCache(Object key, byte type) {
            // TODO
            neverPartOfCompilation();
            return null;
        }

        @Override Shape removeMemberWithoutCache(Object key) {
            // TODO
            neverPartOfCompilation();
            return null;
        }

        @Override int getMemberLocation(Object key) {
            neverPartOfCompilation();
            if (key.equals(this.key)) {
                return location;
            }
            else {
                return base.getMemberLocation(key);
            }
        }

        @Override public boolean equals(Object obj) {
            neverPartOfCompilation();

            if (!(obj instanceof AddedMemberShape other)) {
                return false;
            }

            return base == other.base
                && key.equals(other.key)
                && location == other.location;
        }

        @Override public int hashCode() {
            neverPartOfCompilation();
            var result = identityHashCode(base);
            result = 31 * result + key.hashCode();
            result = 31 * result + Integer.hashCode(location);
            return result;
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class RawLengthNode extends CraterNode {
        public abstract long execute(CraterTable table);

        public static RawLengthNode create() {
            return CraterTableFactory.RawLengthNodeGen.create();
        }

        public static RawLengthNode getUncached() {
            return CraterTableFactory.RawLengthNodeGen.getUncached();
        }

        @Specialization(guards = "table.hasCachedLength()")
        long doCachedLength(CraterTable table) {
            return table.getCachedLength();
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    @ImportStatic({CraterMath.class, Double.class})
    public static abstract class RawGetNode extends CraterNode {
        public abstract Object execute(CraterTable table, Object key);

        public static RawGetNode create() {
            return CraterTableFactory.RawGetNodeGen.create();
        }

        public static RawGetNode getUncached() {
            return CraterTableFactory.RawGetNodeGen.getUncached();
        }

        @Specialization
        CraterNil doNilKey(CraterTable table, CraterNil key) {
            return CraterNil.getInstance();
        }

        @Specialization(guards = "isNaN(key)")
        CraterNil doNanKey(CraterTable table, double key) {
            return CraterNil.getInstance();
        }

        @Specialization
        Object doLongKey(CraterTable table, long key, @Cached WithLongKeyNode rawGetWithLongKeyNode) {
            return rawGetWithLongKeyNode.execute(table.sequenceStorage, table.cachedLength, key);
        }

        @Specialization(guards = "hasExactLongValue(key)")
        Object doDoubleAsLongKey(CraterTable table, double key, @Cached WithLongKeyNode rawGetWithLongKeyNode) {
            return rawGetWithLongKeyNode.execute(table.sequenceStorage, table.cachedLength, (long) key);
        }

        @Fallback
        Object doShapeSensitiveKey(CraterTable table, Object key, @Cached ShapeDispatchNode shapeDispatchNode) {
            return shapeDispatchNode.execute(table, key);
        }

        private static Object readMember(CraterTable table, int location) {
            partialEvaluationConstant(location);
            var type = location & 0b11;
            var index = location >>> 2;
            return switch (type) {
                case Shape.TYPE_BOOLEAN -> getBooleanUnchecked(table.primitiveMemberStorage, index);
                case Shape.TYPE_LONG -> getLongUnchecked(table.primitiveMemberStorage, index);
                case Shape.TYPE_DOUBLE -> getDoubleUnchecked(table.primitiveMemberStorage, index);
                case Shape.TYPE_OBJECT -> getUnchecked(table.objectMemberStorage, index);
                default -> throw new AssertionError();
            };
        }

        @GenerateUncached
        static abstract class WithLongKeyNode extends CraterNode {
            abstract Object execute(Object sequenceStorage, int cachedLength, long key);

            @Specialization
            Object doSomething(Object sequenceStorage, int cachedLength, long key) {
                // TODO
                return null;
            }
        }

        @GenerateUncached
        static abstract class ShapeDispatchNode extends CraterNode {
            abstract Object execute(CraterTable table, Object key);

            @Specialization(guards = "table.getShape() == cachedShape")
            Object doConstantShape(
                CraterTable table,
                Object key,
                @Cached("table.getShape()") Shape cachedShape,
                @Cached KeyDispatchNode keyDispatchNode
            ) {
                return keyDispatchNode.execute(table, key, cachedShape);
            }

            @Specialization(replaces = "doConstantShape")
            Object doDynamicShape(CraterTable table, Object key) {
                // TODO
                return null;
            }
        }

        @GenerateUncached
        static abstract class KeyDispatchNode extends CraterNode {
            abstract Object execute(CraterTable table, Object key, Shape shape);

            @Specialization
            Object doSomething(CraterTable table, Object key, Shape shape) {
                // TODO
                return null;
            }
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
