package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.CraterDispatchedCallNode;
import org.craterlang.language.nodes.values.CraterPrependValueNode;
import org.craterlang.language.runtime.CraterClosure;

@NodeChild(value = "calleeNode", type = CraterExpressionNode.class)
@NodeChild(value = "argumentsNode", type = CraterExpressionNode.class)
public abstract class CraterCallExpressionNode extends CraterExpressionNode {
    @Specialization
    protected Object doClosure(
        CraterClosure callee,
        Object[] arguments,
        @Cached CraterPrependValueNode prependCalleeNode,
        @Cached CraterDispatchedCallNode dispatchedCallNode
    ) {
        var combinedArguments = prependCalleeNode.execute(callee, arguments);
        return dispatchedCallNode.execute(callee.getCallTarget(), combinedArguments);
    }
}
