package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterClosure;

@GenerateUncached
public abstract class CraterInvokeNode extends CraterNode {
    public abstract Object execute(Object callee, Object continuationFrame, Object[] arguments);

    @Specialization
    Object doClosure(
        CraterClosure callee,
        Object continuationFrame,
        Object[] arguments,
        @Cached CraterClosure.InvokeNode closureInvokeNode
    ) {
        return closureInvokeNode.execute(callee, continuationFrame, arguments);
    }

    @Specialization
    Object doBuiltin(
        CraterBuiltin callee,
        Object continuationFrame,
        Object[] arguments,
        @Cached CraterBuiltin.InvokeNode builtinInvokeNode
    ) {
        return builtinInvokeNode.execute(callee, continuationFrame, arguments);
    }
}
