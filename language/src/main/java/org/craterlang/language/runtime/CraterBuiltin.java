package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
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

    public abstract Object invokeUncached(Object[] arguments);

    public static abstract class BodyNode extends CraterNode {
        public abstract Object execute(Object[] arguments);
    }

    @GenerateUncached
    public static abstract class InvokeNode extends CraterNode {
        public abstract Object execute(CraterBuiltin builtin, Object[] arguments);

        @Specialization(guards = "builtin == cachedBuiltin")
        Object doCached(
            CraterBuiltin builtin,
            Object[] arguments,
            @Cached("builtin") CraterBuiltin cachedBuiltin,
            @Cached("cachedBuiltin.createBodyNode()") BodyNode bodyNode
        ) {
            return bodyNode.execute(arguments);
        }

        @Specialization(replaces = "doCached")
        Object doUncached(CraterBuiltin builtin, Object[] arguments) {
            return builtin.invokeUncached(arguments);
        }
    }
}
