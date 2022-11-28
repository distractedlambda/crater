package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.PropertyGetter;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltinFunction;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;


import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
public abstract class NextBuiltinNode extends CraterBuiltinFunction.ExecutorNode {
    @Specialization
    Object doExecute(Object[] arguments, @CachedLibrary ImplNode implNode) {
        if (arguments.length == 0 || !(arguments[0] instanceof CraterTable table)) {
            transferToInterpreter();
            throw error("invalid arguments");
        }

        Object key;
        if (arguments.length == 1) {
            key = CraterNil.getInstance();
        }
        else {
            key = arguments[1];
        }

        return implNode.execute(table, key);
    }

    protected static abstract class ImplNode extends CraterNode {
        abstract Object execute(CraterTable table, Object key);

        @Specialization
        protected Object doNilKey(
            CraterTable table,
            CraterNil key,
            @CachedLibrary("table") DynamicObjectLibrary tables
        ) {
            var keys = tables.getKeyArray(table);
            if (keys.length == 0) {
                return CraterNil.getInstance();
            }
            else {
                return new Object[]{keys[0], tables.getOrDefault(table, keys[0], null)};
            }
        }

        // @TruffleBoundary(allowInlining = true)
        // private static Object lookUpKeySuccessor(UnmodifiableEconomicMap<Object, Object> keySuccessors, Object key) {
        //     return keySuccessors.get(key);
        // }

        // protected static PropertyGetter getSequenceLengthGetter(Shape shape) {
        //     return shape.makePropertyGetter(CraterTable.getSequenceLengthKey());
        // }

        // protected static UnmodifiableEconomicMap<Object, Object> getKeySuccessors(Shape shape) {
        //     var map = EconomicMap.create(shape.getPropertyCount());
        //     var keyIterator = shape.getKeys().iterator();

        //     if (keyIterator.hasNext()) {
        //         var key = keyIterator.next();
        //         map.put(CraterNil.getInstance(), key);

        //         while (keyIterator.hasNext()) {
        //             var nextKey = keyIterator.next();
        //             map.put(key, nextKey);
        //             key = nextKey;
        //         }

        //         map.put(key, CraterNil.getInstance());
        //     }

        //     return map;
        // }
    }
}
