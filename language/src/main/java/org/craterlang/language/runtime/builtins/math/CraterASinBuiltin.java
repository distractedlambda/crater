package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.CraterForceIntoDoubleNode;
import org.craterlang.language.runtime.CraterBuiltin;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public final class CraterASinBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterASinBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object invokeUncached(Object[] arguments) {
        return CraterASinBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        double doExecute(Object[] arguments, @Cached CraterForceIntoDoubleNode forceIntoDoubleNode) {
            if (arguments.length == 0) {
                transferToInterpreter();
                throw error("");
            }

            return boundary(forceIntoDoubleNode.execute(arguments[0]));
        }

        @TruffleBoundary(allowInlining = true)
        private static double boundary(double x) {
            return Math.asin(x);
        }
    }
}
