package org.craterlang.language.nodes.builtins;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterUnaryBuiltinBodyNode extends CraterBuiltinFunctionBodyNode {
    @Override public final Object execute(Object[] arguments, int argumentsStart, int argumentsLength) {
        if (argumentsLength < 1) {
            transferToInterpreter();
            throw error("");
        }

        return executeUnary(arguments[argumentsStart]);
    }

    protected abstract Object executeUnary(Object argument);
}
