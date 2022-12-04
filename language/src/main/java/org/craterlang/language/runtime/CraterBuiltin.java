package org.craterlang.language.runtime;

import com.oracle.truffle.api.interop.TruffleObject;
import org.craterlang.language.CraterNode;

public abstract class CraterBuiltin implements TruffleObject {
    private Object metatable = CraterNil.getInstance();

    public final Object getMetatable() {
        return metatable;
    }

    public final void setMetatable(Object metatable) {
        assert metatable != null;
        this.metatable = metatable;
    }

    public abstract BodyNode createBodyNode();

    public BodyNode createTailBodyNode() {
        return createBodyNode();
    }

    public abstract Object callUncached(Object continuationFrame, Object[] arguments);

    public Object tailCallUncached(Object continuationFrame, Object[] arguments) {
        return callUncached(continuationFrame, arguments);
    }

    public abstract static class BodyNode extends CraterNode {
        public abstract Object execute(Object continuationFrame, Object[] arguments);
    }
}
