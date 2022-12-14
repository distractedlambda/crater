package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterGetMetatableNode;
import org.craterlang.language.nodes.CraterInvokeNode;
import org.craterlang.language.nodes.values.CraterAdjustToOneValueNode;
import org.craterlang.language.runtime.CraterString;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.CraterTypeSystem.isNil;

abstract class CraterBinaryMetamethodInvokeNode extends CraterNode {
    public abstract Object execute(Object lhs, Object rhs, CraterString metamethodKey);

    @Specialization
    Object doExecute(
        Object lhs,
        Object rhs,
        CraterString metamethodKey,
        @Cached CraterGetMetatableNode getLhsMetatableNode,
        @Cached CraterTable.RawGetNode getLhsMetamethodNode,
        @Cached CraterInvokeNode lhsMetamethodInvokeNode,
        @Cached CraterAdjustToOneValueNode lhsAdjustToOneValueNode,
        @Cached CraterGetMetatableNode getRhsMetatableNode,
        @Cached CraterTable.RawGetNode getRhsMetamethodNode,
        @Cached CraterInvokeNode rhsMetamethodInvokeNode,
        @Cached CraterAdjustToOneValueNode rhsAdjustToOneValueNode
    ) {
        if (getLhsMetatableNode.execute(lhs) instanceof CraterTable table) {
            var metamethod = getLhsMetamethodNode.execute(table, metamethodKey);
            if (!isNil(metamethod)) {
                var results = lhsMetamethodInvokeNode.execute(metamethod, new Object[]{lhs, rhs});
                return lhsAdjustToOneValueNode.execute(results);
            }
        }

        if (getRhsMetatableNode.execute(rhs) instanceof CraterTable table) {
            var metamethod = getRhsMetamethodNode.execute(table, metamethodKey);
            if (!isNil(metamethod)) {
                var results = rhsMetamethodInvokeNode.execute(metamethod, new Object[]{lhs, rhs});
                return rhsAdjustToOneValueNode.execute(results);
            }
        }

        transferToInterpreter();
        throw error("");
    }
}
