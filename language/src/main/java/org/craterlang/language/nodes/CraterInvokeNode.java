package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.values.CraterPrependValueNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterClosure;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.hasNextTier;
import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static com.oracle.truffle.api.nodes.LoopNode.reportLoopCount;
import static org.craterlang.language.CraterTypeSystem.isNil;

@GenerateUncached
public abstract class CraterInvokeNode extends CraterNode {
    public abstract Object execute(Object callee, Object[] arguments);

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
            catch (CraterTailCallException exception) {
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
        Object doClosure(
            CraterClosure callee,
            Object[] arguments,
            @Cached CraterClosure.InvokeNode closureInvokeNode
        ) {
            return closureInvokeNode.execute(callee, arguments);
        }

        @Specialization
        Object doBuiltin(
            CraterBuiltin callee,
            Object[] arguments,
            @Cached CraterBuiltin.InvokeNode builtinInvokeNode
        ) {
            return builtinInvokeNode.execute(callee, arguments);
        }

        @Fallback
        Object doMetamethod(
            Object callee,
            Object[] arguments,
            @Cached CraterGetMetatableNode getMetatableNode,
            @Cached CraterTable.RawGetNode getMetamethodNode,
            @Cached CraterPrependValueNode prependCalleeNode,
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
