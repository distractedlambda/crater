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
import org.craterlang.language.runtime.CraterTable;

import java.util.Arrays;

public final class CraterTablePackBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterTablePackBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object invokeUncached(Object continuationFrame, Object[] arguments) {
        return CraterTablePackBuiltinFactory.ImplNodeGen.getUncached().execute(continuationFrame, arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        CraterTable doExecute(
            Object continuationFrame,
            Object[] arguments,
            @CachedLibrary(limit = "5") DynamicObjectLibrary tables,
            @Cached ImplWithLibraryNode implWithLibraryNode
        ) {
            return implWithLibraryNode.execute(arguments, tables);
        }
    }

    @GenerateUncached
    static abstract class ImplWithLibraryNode extends CraterNode {
        abstract CraterTable execute(Object arguments, DynamicObjectLibrary tables);

        @Specialization(guards = "arguments.length == 0")
        CraterTable doEmpty(Object[] arguments, DynamicObjectLibrary tables) {
            return createTable(0, tables);
        }

        @Specialization(guards = {"arguments.length != 0", "arguments.length == cachedLength"})
        CraterTable doConstantLength(
            Object[] arguments,
            DynamicObjectLibrary tables,
            @Cached("arguments.length") int cachedLength,
            @Cached CreateConstantLengthStorageNode createConstantLengthStorageNode
        ) {
            var storage = createConstantLengthStorageNode.execute(arguments, cachedLength);
            return createOptimizedArrayTable(storage, cachedLength, tables);
        }

        @Specialization(guards = "arguments.length != 0", replaces = "doConstantLength")
        CraterTable doDynamicLength(Object[] arguments, DynamicObjectLibrary tables) {
            var storage = createDynamicStorage(arguments);
            return createOptimizedArrayTable(storage, arguments.length, tables);
        }

        @TruffleBoundary
        private static Object createDynamicStorage(Object[] arguments) {
            primitive: for (;;) {
                if (arguments[0] instanceof Boolean) {
                    for (var i = 1; i < arguments.length; i++) {
                        if (!(arguments[i] instanceof Boolean)) {
                            break primitive;
                        }
                    }

                    var storage = new boolean[arguments.length];

                    for (var i = 0; i < arguments.length; i++) {
                        storage[i] = (boolean) arguments[i];
                    }

                    return storage;
                }
                else if (arguments[0] instanceof Long) {
                    for (var i = 1; i < arguments.length; i++) {
                        if (!(arguments[i] instanceof Long)) {
                            break primitive;
                        }
                    }

                    var storage = new long[arguments.length];

                    for (var i = 0; i < arguments.length; i++) {
                        storage[i] = (long) arguments[i];
                    }

                    return storage;
                }
                else if (arguments[0] instanceof Double) {
                    for (var i = 1; i < arguments.length; i++) {
                        if (!(arguments[i] instanceof Double)) {
                            break primitive;
                        }
                    }

                    var storage = new double[arguments.length];

                    for (var i = 0; i < arguments.length; i++) {
                        storage[i] = (double) arguments[i];
                    }

                    return storage;
                }
                else {
                    break;
                }
            }

            return Arrays.copyOf(arguments, arguments.length);
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
        abstract Object execute(Object[] arguments, int length);

        @ExplodeLoop
        @Specialization(guards = "isAllBooleans(arguments, length)")
        boolean[] doAllBooleans(Object[] arguments, int length) {
            var storage = new boolean[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (boolean) arguments[i];
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllLongs(arguments, length)")
        long[] doAllLongs(Object[] arguments, int length) {
            var storage = new long[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (long) arguments[i];
            }

            return storage;
        }

        @ExplodeLoop
        @Specialization(guards = "isAllDoubles(arguments, length)")
        double[] doAllDoubles(Object[] arguments, int length) {
            var storage = new double[length];

            for (var i = 0; i < length; i++) {
                storage[i] = (double) arguments[i];
            }

            return storage;
        }

        @Fallback
        @ExplodeLoop
        Object[] doGeneric(Object[] arguments, int length) {
            var storage = new Object[length];

            for (var i = 0; i < length; i++) {
                storage[i] = arguments[i];
            }

            return storage;
        }

        @ExplodeLoop
        static boolean isAllBooleans(Object[] arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[i] instanceof Boolean)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllLongs(Object[] arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[i] instanceof Long)) {
                    return false;
                }
            }

            return true;
        }

        @ExplodeLoop
        static boolean isAllDoubles(Object[] arguments, int length) {
            for (var i = 0; i < length; i++) {
                if (!(arguments[i] instanceof Double)) {
                    return false;
                }
            }

            return true;
        }
    }
}
