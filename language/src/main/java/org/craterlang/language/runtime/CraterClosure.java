package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.TruffleObject;

public final class CraterClosure implements TruffleObject {
    private final CallTarget callTarget;
    @CompilationFinal(dimensions = 1) private final CraterUpvalue[] upvalues;
    private Object metatable = CraterNil.getInstance();

    public CraterClosure(CallTarget callTarget, CraterUpvalue[] upvalues) {
        assert callTarget != null;
        assert upvalues != null;
        this.callTarget = callTarget;
        this.upvalues = upvalues;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    public CraterUpvalue getUpvalue(int index) {
        return upvalues[index];
    }

    public Object getMetatable() {
        return metatable;
    }

    public void setMetatable(Object metatable) {
        assert metatable != null;
        this.metatable = metatable;
    }
}
