package org.craterlang.language.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import org.craterlang.language.CraterNode;

@GenerateUncached
@GeneratePackagePrivate
public abstract class DispatchedCallNode extends CraterNode {
    public abstract Object execute(CallTarget callTarget, Object[] arguments);

    public static DispatchedCallNode create() {
        return DispatchedCallNodeGen.create();
    }

    public static DispatchedCallNode getUncached() {
        return DispatchedCallNodeGen.getUncached();
    }

    @Specialization(guards = "callTarget == directCallNode.getCallTarget()")
    protected Object doDirect(
        CallTarget callTarget,
        Object[] arguments,
        @Cached(parameters = "callTarget") DirectCallNode directCallNode
    ) {
        return directCallNode.call(arguments);
    }

    @Specialization(replaces = "doDirect")
    protected Object doIndirect(CallTarget callTarget, Object[] arguments, @Cached IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(callTarget, arguments);
    }
}
