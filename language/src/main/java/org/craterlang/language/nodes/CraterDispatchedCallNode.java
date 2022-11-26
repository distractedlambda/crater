package org.craterlang.language.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import org.craterlang.language.CraterNode;

@GenerateUncached
public abstract class CraterDispatchedCallNode extends CraterNode {
    public abstract Object execute(CallTarget callTarget, Object[] arguments);

    @Specialization(guards = "callTarget == directCallNode.getCallTarget()")
    protected Object doDirect(
        CallTarget callTarget,
        Object[] arguments,
        @Cached(parameters = "callTarget") DirectCallNode directCallNode
    ) {
        return directCallNode.call(arguments);
    }

    @Megamorphic
    @Specialization(replaces = "doDirect")
    protected Object doIndirect(CallTarget callTarget, Object[] arguments, @Cached IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(callTarget, arguments);
    }
}
