package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterDispatchedCallNode;
import org.craterlang.language.nodes.values.CraterPrependValueNode;

public final class CraterFunction extends DynamicObject implements TruffleObject {
    public CraterFunction(Shape shape) {
        super(shape);
    }

    @GenerateUncached
    public static abstract class GetCallTargetNode extends CraterNode {
        public abstract CallTarget execute(CraterFunction function);

        @Specialization(guards = "function == cachedFunction")
        CallTarget doConstantFunction(
            CraterFunction function,
            @Cached(value = "function", weak = true) CraterFunction cachedFunction,
            @Cached(value = "function.getShape()", weak = true) Shape cachedShape
        ) {
            return (CallTarget) cachedShape.getDynamicType();
        }

        @Specialization(guards = "function.getShape() == cachedShape", replaces = "doConstantFunction")
        CallTarget doConstantShape(
            CraterFunction function,
            @Cached(value = "function.getShape()", weak = true) Shape cachedShape
        ) {
            return (CallTarget) cachedShape.getDynamicType();
        }

        @Specialization(replaces = "doConstantShape")
        CallTarget doDynamic(CraterFunction function) {
            return (CallTarget) function.getShape().getDynamicType();
        }
    }

    public static abstract class InvokeNode extends CraterNode {
        public abstract Object execute(CraterFunction function, Object[] arguments);

        @Specialization
        Object doExecute(
            CraterFunction callee,
            Object[] arguments,
            @Cached GetCallTargetNode getCallTargetNode,
            @Cached CraterPrependValueNode prependCalleeNode,
            @Cached CraterDispatchedCallNode dispatchedCallNode
        ) {
            var callTarget = getCallTargetNode.execute(callee);
            var callArguments = prependCalleeNode.execute(callee, arguments);
            return dispatchedCallNode.execute(callTarget, callArguments);
        }
    }
}
