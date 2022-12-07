package org.craterlang.language.nodes.builtins.table;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.builtins.CraterBuiltinFunctionBodyNode;
import org.craterlang.language.runtime.CraterTable;

import java.util.Arrays;
import java.util.function.Supplier;

@GeneratePackagePrivate
public abstract class CraterTablePackNode extends CraterBuiltinFunctionBodyNode {
    public static Supplier<CraterTablePackNode> getFactory() {
        return CraterTablePackNodeGen::create;
    }

    @Specialization
    CraterTable doExecute(
        Object[] arguments,
        int argumentsStart,
        int argumentsLength,
        @CachedLibrary(limit = "5") DynamicObjectLibrary tables,
        @Cached ImplWithLibraryNode implWithLibraryNode
    ) {
        return implWithLibraryNode.execute(arguments, argumentsStart, argumentsLength, tables);
    }

    static abstract class ImplWithLibraryNode extends CraterNode {
        abstract CraterTable execute(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            DynamicObjectLibrary tables
        );

        @Specialization(guards = "argumentsLength == 0")
        CraterTable doEmpty(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            DynamicObjectLibrary tables
        ) {
            return createTable(0, tables);
        }

        @Specialization(guards = {"argumentsLength > 0", "argumentsLength == cachedLength"})
        CraterTable doConstantLength(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            DynamicObjectLibrary tables,
            @Cached("argumentsLength") int cachedLength,
            @Cached CreateConstantLengthStorageNode createConstantLengthStorageNode
        ) {
            var storage = createConstantLengthStorageNode.execute(arguments, argumentsStart, cachedLength);
            return createOptimizedArrayTable(storage, cachedLength, tables);
        }

        @Specialization(guards = "argumentsLength > 0", replaces = "doConstantLength")
        CraterTable doDynamicLength(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            DynamicObjectLibrary tables
        ) {
            var storage = createDynamicStorage(arguments, argumentsStart, argumentsLength);
            return createOptimizedArrayTable(storage, argumentsLength, tables);
        }

        @TruffleBoundary
        private static Object createDynamicStorage(Object[] arguments, int start, int length) {
            primitive: for (;;) {
                if (arguments[start] instanceof Boolean) {
                    for (var i = 1; i < length; i++) {
                        if (!(arguments[start + i] instanceof Boolean)) {
                            break primitive;
                        }
                    }

                    var storage = new boolean[length];

                    for (var i = 0; i < length; i++) {
                        storage[i] = (boolean) arguments[start + i];
                    }

                    return storage;
                }
                else if (arguments[start] instanceof Long) {
                    for (var i = 1; i < length; i++) {
                        if (!(arguments[start + i] instanceof Long)) {
                            break primitive;
                        }
                    }

                    var storage = new long[length];

                    for (var i = 0; i < length; i++) {
                        storage[i] = (long) arguments[start + i];
                    }

                    return storage;
                }
                else if (arguments[start] instanceof Double) {
                    for (var i = 1; i < length; i++) {
                        if (!(arguments[i] instanceof Double)) {
                            break primitive;
                        }
                    }

                    var storage = new double[length];

                    for (var i = 0; i < length; i++) {
                        storage[i] = (double) arguments[start + i];
                    }

                    return storage;
                }
                else {
                    break;
                }
            }

            return Arrays.copyOfRange(arguments, start, length, Object[].class);
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

    static abstract class CreateConstantLengthStorageNode extends CraterNode {
        abstract Object execute(Object[] arguments, int start, int length);

        @ExplodeLoop
        @Specialization(guards = "isAllBooleans(arguments, start, length)")
        boolean[] doAllBooleans(Object[] arguments, int start, int length) {
            var storage = new boolean[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (boolean) arguments[start + i];
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllLongs(arguments, start, length)")
        long[] doAllLongs(Object[] arguments, int start, int length) {
            var storage = new long[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (long) arguments[start + i];
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllDoubles(arguments, start, length)")
        double[] doAllDoubles(Object[] arguments, int start, int length) {
            var storage = new double[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (double) arguments[start + i];
            }

            return storage;
        }

        @Fallback
        @ExplodeLoop
        Object[] doGeneric(Object[] arguments, int start, int length) {
            var storage = new Object[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments[start + i];
            }

            return storage;
        }

        @ExplodeLoop
        static boolean isAllBooleans(Object[] arguments, int start, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[start + i] instanceof Boolean)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllLongs(Object[] arguments, int start, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[start + i] instanceof Long)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllDoubles(Object[] arguments, int start, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[start + i] instanceof Double)) {
                    return false;
                }
            }

            return true;
        }
    }
}
