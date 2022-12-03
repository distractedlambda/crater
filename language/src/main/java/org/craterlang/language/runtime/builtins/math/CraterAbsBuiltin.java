package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltin;

public final class CraterAbsBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterAbsBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object arguments) {
        return CraterAbsBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        Object doExecute(
            Object arguments,
            @Cached CraterExtractSingleNumericArgumentNode extractSingleNumericArgumentNode,
            @Cached DispatchNode dispatchNode
        ) {
            return dispatchNode.execute(extractSingleNumericArgumentNode.execute(arguments));
        }
    }

    @GenerateUncached
    static abstract class DispatchNode extends CraterNode {
        abstract Object execute(Object value);

        @Specialization
        long doLong(long value) {
            return Math.abs(value);
        }

        @Specialization
        double doDouble(double value) {
            return Math.abs(value);
        }
    }
}
