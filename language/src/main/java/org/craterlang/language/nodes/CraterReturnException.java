package org.craterlang.language.nodes;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.nodes.ControlFlowException;

@ValueType
public final class CraterReturnException extends ControlFlowException {
    private final Object result;

    public CraterReturnException(Object result) {
        assert result != null;
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
