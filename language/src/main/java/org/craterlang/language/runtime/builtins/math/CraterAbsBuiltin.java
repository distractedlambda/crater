package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltin;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public final class CraterAbsBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterAbsBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object invokeUncached(Object[] arguments) {
        return CraterAbsBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        Object doExecute(Object[] arguments, @Cached DispatchNode dispatchNode) {
            if (arguments.length == 0) {
                transferToInterpreter();
                throw error("");
            }

            return dispatchNode.execute(arguments[0]);
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
