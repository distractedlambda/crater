package org.craterlang.language.nodes.builtins.math;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.ForceIntoDoubleNode;
import org.craterlang.language.nodes.builtins.UnaryBuiltinBodyNode;

import java.util.function.Supplier;

@GenerateUncached
@GeneratePackagePrivate
public abstract class ASinNode extends UnaryBuiltinBodyNode {
    @Specialization
    double doExecute(Object argument, @Cached ForceIntoDoubleNode forceIntoDoubleNode) {
        return boundary(forceIntoDoubleNode.execute(argument));
    }

    @TruffleBoundary(allowInlining = true)
    private static double boundary(double x) {
        return Math.asin(x);
    }

    public static Supplier<ASinNode> getFactory() {
        return ASinNodeGen::create;
    }
}
