package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CraterTailCallException extends ControlFlowException {
    private final CallTarget callee;
    @CompilationFinal(dimensions = 1) private final Object[] arguments;

    public CraterTailCallException(CallTarget callee, Object[] arguments) {
        assert callee != null;
        assert arguments != null;
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
