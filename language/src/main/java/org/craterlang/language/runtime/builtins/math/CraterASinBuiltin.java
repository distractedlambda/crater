package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterBuiltin;

public final class CraterASinBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterASinBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object arguments) {
        return CraterASinBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        double doExecute(
            Object arguments,
            @Cached CraterExtractSingleDoubleArgumentNode extractSingleDoubleArgumentNode
        ) {
            return boundary(extractSingleDoubleArgumentNode.execute(arguments));
        }

        @TruffleBoundary(allowInlining = true)
        private static double boundary(double x) {
            return Math.asin(x);
        }
    }
}
