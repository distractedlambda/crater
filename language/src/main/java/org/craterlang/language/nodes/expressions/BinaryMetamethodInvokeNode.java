package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.GetMetatableNode;
import org.craterlang.language.nodes.InvokeNode;
import org.craterlang.language.nodes.values.AdjustToOneValueNode;
import org.craterlang.language.runtime.CraterString;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.CraterTypeSystem.isNil;

@GenerateUncached
@GeneratePackagePrivate
abstract class BinaryMetamethodInvokeNode extends CraterNode {
    public abstract Object execute(Object lhs, Object rhs, CraterString metamethodKey);

    public static BinaryMetamethodInvokeNode create() {
        return BinaryMetamethodInvokeNodeGen.create();
    }

    public static BinaryMetamethodInvokeNode getUncached() {
        return BinaryMetamethodInvokeNodeGen.getUncached();
    }

    @Specialization
    Object doExecute(
        Object lhs,
        Object rhs,
        CraterString metamethodKey,
        @Cached GetMetatableNode getLhsMetatableNode,
        @Cached CraterTable.RawGetNode getLhsMetamethodNode,
        @Cached InvokeNode lhsMetamethodInvokeNode,
        @Cached AdjustToOneValueNode lhsAdjustToOneValueNode,
        @Cached GetMetatableNode getRhsMetatableNode,
        @Cached CraterTable.RawGetNode getRhsMetamethodNode,
        @Cached InvokeNode rhsMetamethodInvokeNode,
        @Cached AdjustToOneValueNode rhsAdjustToOneValueNode
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
