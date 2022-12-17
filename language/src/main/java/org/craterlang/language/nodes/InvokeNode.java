package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.values.PrependValueNode;
import org.craterlang.language.runtime.CraterFunction;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.hasNextTier;
import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static com.oracle.truffle.api.nodes.LoopNode.reportLoopCount;
import static org.craterlang.language.CraterTypeSystem.isNil;

@GenerateUncached
@GeneratePackagePrivate
public abstract class InvokeNode extends CraterNode {
    public abstract Object execute(Object callee, Object[] arguments);

    public static InvokeNode create() {
        return InvokeNodeGen.create();
    }

    public static InvokeNode getUncached() {
        return InvokeNodeGen.getUncached();
    }

    @Specialization
    Object doExecute(
        Object callee,
        Object[] arguments,
        @Cached DispatchNode dispatchNode,
        @Cached BranchProfile tailCallProfile
    ) {
        Object result;
        var tailCallCount = 0L;

        for (;;) {
            try {
                result = dispatchNode.execute(callee, arguments);
                break;
            }
            catch (TailCallException exception) {
                tailCallProfile.enter();

                callee = exception.getCallee();
                arguments = exception.getArguments();

                if (hasNextTier()) {
                    tailCallCount++;
                }
            }
        }

        if (tailCallCount != 0) {
            reportLoopCount(this, tailCallCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) tailCallCount);
        }

        return result;
    }

    @GenerateUncached
    static abstract class DispatchNode extends CraterNode {
        abstract Object execute(Object callee, Object[] arguments);

        @Specialization
        Object doFunction(
            CraterFunction callee,
            Object[] arguments,
            @Cached CraterFunction.InvokeNode functionInvokeNode
        ) {
            return functionInvokeNode.execute(callee, arguments);
        }

        @Fallback
        Object doMetamethod(
            Object callee,
            Object[] arguments,
            @Cached GetMetatableNode getMetatableNode,
            @Cached CraterTable.RawGetNode getMetamethodNode,
            @Cached PrependValueNode prependCalleeNode,
            @Cached DispatchNode metamethodDispatchNode
        ) {
            var metatable = getMetatableNode.execute(callee);

            if (!(metatable instanceof CraterTable metatableTable)) {
                transferToInterpreter();
                throw error("");
            }

            var callMetamethod = getMetamethodNode.execute(metatableTable, getLanguage().getCallMetamethodKey());

            if (isNil(callMetamethod)) {
                transferToInterpreter();
                throw error("");
            }

            return metamethodDispatchNode.execute(callMetamethod, prependCalleeNode.execute(callee, arguments));
        }
    }
}
