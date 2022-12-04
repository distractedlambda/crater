package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterClosure;

import static com.oracle.truffle.api.CompilerDirectives.hasNextTier;
import static com.oracle.truffle.api.nodes.LoopNode.reportLoopCount;

@GenerateUncached
public abstract class CraterInvokeNode extends CraterNode {
    public abstract Object execute(Object callee, Object continuationFrame, Object[] arguments);

    @Specialization
    Object doExecute(
        Object callee,
        Object continuationFrame,
        Object[] arguments,
        @Cached DispatchNode dispatchNode,
        @Cached BranchProfile tailCallProfile
    ) {
        Object result;
        var tailCallCount = 0L;

        for (;;) {
            try {
                result = dispatchNode.execute(callee, continuationFrame, arguments);
                break;
            }
            catch (CraterTailCallException exception) {
                tailCallProfile.enter();

                callee = exception.getCallee();
                continuationFrame = exception.getContinuationFrame();
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
        abstract Object execute(Object callee, Object continuationFrame, Object[] arguments);

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
}
