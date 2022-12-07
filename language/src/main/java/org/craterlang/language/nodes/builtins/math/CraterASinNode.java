package org.craterlang.language.nodes.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.CraterForceIntoDoubleNode;
import org.craterlang.language.nodes.builtins.CraterUnaryBuiltinBodyNode;

public abstract class CraterASinNode extends CraterUnaryBuiltinBodyNode {
    @Specialization
    double doExecute(Object argument, @Cached CraterForceIntoDoubleNode forceIntoDoubleNode) {
        return boundary(forceIntoDoubleNode.execute(argument));
    }

    @TruffleBoundary(allowInlining = true)
    private static double boundary(double x) {
        return Math.asin(x);
    }
}
