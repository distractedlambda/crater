package org.craterlang.language.runtime.builtins.table;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterMultipleValues;
import org.craterlang.language.runtime.CraterNoValues;
import org.craterlang.language.runtime.CraterTable;

public final class CraterTablePackBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterTablePackBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object arguments) {
        return CraterTablePackBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        CraterTable doExecute(
            Object arguments,
            @CachedLibrary(limit = "5") DynamicObjectLibrary tables,
            @Cached ImplWithLibraryNode implWithLibraryNode
        ) {
            return implWithLibraryNode.execute(arguments, tables);
        }
    }

    @GenerateUncached
    static abstract class ImplWithLibraryNode extends CraterNode {
        abstract CraterTable execute(Object arguments, DynamicObjectLibrary tables);

        @Specialization
        CraterTable doEmpty(CraterNoValues arguments, DynamicObjectLibrary tables) {
            return createTable(0, tables);
        }

        @Specialization
        CraterTable doSingleBoolean(boolean argument, DynamicObjectLibrary tables) {
            return createOptimizedArrayTable(new boolean[]{argument}, 1, tables);
        }

        @Specialization
        CraterTable doSingleLong(long argument, DynamicObjectLibrary tables) {
            return createOptimizedArrayTable(new long[]{argument}, 1, tables);
        }

        @Specialization
        CraterTable doSingleDouble(double argument, DynamicObjectLibrary tables) {
            return createOptimizedArrayTable(new double[]{argument}, 1, tables);
        }

        @Specialization(guards = "arguments.getLength() == cachedLength")
        CraterTable doConstantLength(
            CraterMultipleValues arguments,
            DynamicObjectLibrary tables,
            @Cached("arguments.getLength()") int cachedLength,
            @Cached CreateConstantLengthStorageNode createConstantLengthStorageNode
        ) {
            var storage = createConstantLengthStorageNode.execute(arguments, cachedLength);
            return createOptimizedArrayTable(storage, cachedLength, tables);
        }

        @Specialization(replaces = "doConstantLength")
        CraterTable doDynamicLength(CraterMultipleValues arguments, DynamicObjectLibrary tables) {
            var storage = createDynamicStorage(arguments);
            return createOptimizedArrayTable(storage, arguments.getLength(), tables);
        }

        @Fallback
        CraterTable doSingleObject(Object argument, DynamicObjectLibrary tables) {
            return createOptimizedArrayTable(new Object[]{argument}, 1, tables);
        }

        @TruffleBoundary
        private static Object createDynamicStorage(CraterMultipleValues arguments) {
            primitive: for (;;) {
                if (arguments.isBoolean(0)) {
                    for (var i = 1; i < arguments.getLength(); i++) {
                        if (!arguments.isBoolean(i)) {
                            break primitive;
                        }
                    }

                    var storage = new boolean[arguments.getLength()];

                    for (var i = 0; i < arguments.getLength(); i++) {
                        storage[i] = arguments.getBoolean(i);
                    }

                    return storage;
                }
                else if (arguments.isLong(0)) {
                    for (var i = 1; i < arguments.getLength(); i++) {
                        if (!arguments.isLong(i)) {
                            break primitive;
                        }
                    }

                    var storage = new long[arguments.getLength()];

                    for (var i = 0; i < arguments.getLength(); i++) {
                        storage[i] = arguments.getLong(i);
                    }

                    return storage;
                }
                else if (arguments.isDouble(0)) {
                    for (var i = 1; i < arguments.getLength(); i++) {
                        if (!arguments.isDouble(i)) {
                            break primitive;
                        }
                    }

                    var storage = new double[arguments.getLength()];

                    for (var i = 0; i < arguments.getLength(); i++) {
                        storage[i] = arguments.getDouble(i);
                    }

                    return storage;
                }
                else {
                    break;
                }
            }

            return arguments.getCopyOfValues();
        }

        private CraterTable createTable(long n, DynamicObjectLibrary tables) {
            var table = getLanguage().createTable();
            tables.putLong(table, getLanguage().getLowercaseLetterNString(), n);
            return table;
        }

        private CraterTable createOptimizedArrayTable(Object storage, long n, DynamicObjectLibrary tables) {
            var table = createTable(n, tables);
            table.setOptimizedArray(storage, n, tables);
            return table;
        }
    }

    @GenerateUncached
    static abstract class CreateConstantLengthStorageNode extends CraterNode {
        abstract Object execute(CraterMultipleValues arguments, int length);

        @ExplodeLoop
        @Specialization(guards = "isAllBooleans(arguments, length)")
        boolean[] doAllBooleans(CraterMultipleValues arguments, int length) {
            var storage = new boolean[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments.getBoolean(i);
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllLongs(arguments, length)")
        long[] doAllLongs(CraterMultipleValues arguments, int length) {
            var storage = new long[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments.getLong(i);
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllDoubles(arguments, length)")
        double[] doAllDoubles(CraterMultipleValues arguments, int length) {
            var storage = new double[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments.getDouble(i);
            }

            return storage;
        }

        @Fallback
        @ExplodeLoop
        Object[] doGeneric(CraterMultipleValues arguments, int length) {
            var storage = new Object[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments.get(i);
            }

            return storage;
        }

        @ExplodeLoop
        static boolean isAllBooleans(CraterMultipleValues arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!arguments.isBoolean(i)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllLongs(CraterMultipleValues arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!arguments.isLong(i)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllDoubles(CraterMultipleValues arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!arguments.isDouble(i)) {
                    return false;
                }
            }

            return true;
        }
    }
}
