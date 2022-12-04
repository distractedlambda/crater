package org.craterlang.language.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CraterTailCallException extends ControlFlowException {
    private final Object callee;
    @CompilationFinal(dimensions = 1) private final Object[] arguments;

    public CraterTailCallException(Object callee, Object[] arguments) {
        this.callee = callee;
        this.arguments = arguments;
    }

    public Object getCallee() {
        return callee;
    }

    public Object[] getArguments() {
        return arguments;
    }
}
