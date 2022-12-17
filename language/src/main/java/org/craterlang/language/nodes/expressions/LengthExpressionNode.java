package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.nodes.GetMetatableNode;
import org.craterlang.language.nodes.InvokeNode;
import org.craterlang.language.nodes.values.AdjustToOneValueNode;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.CraterTypeSystem.isNil;

@GeneratePackagePrivate
public abstract class LengthExpressionNode extends UnaryExpressionNode {
    public static LengthExpressionNode create(ExpressionNode operandNode) {
        return LengthExpressionNodeGen.create(operandNode);
    }

    @Override public ExpressionNode cloneUninitialized() {
        return create(getOperandNode());
    }

    @Specialization
    long doString(TruffleString operand) {
        return operand.byteLength(TruffleString.Encoding.BYTES);
    }

    @Fallback
    Object doOther(
        Object operand,
        @Cached GetMetatableNode getMetatableNode,
        @Cached ConditionProfile hasMetatableProfile,
        @Cached CraterTable.RawGetNode getMetamethodNode,
        @Cached ConditionProfile hasMetamethodProfile,
        @Cached InvokeNode metamethodInvokeNode,
        @Cached AdjustToOneValueNode adjustToOneValueNode,
        @Cached CraterTable.RawLengthNode rawLengthNode
    ) {
        var metatable = getMetatableNode.execute(operand);

        if (hasMetatableProfile.profile(metatable instanceof CraterTable)) {
            var metamethod = getMetamethodNode.execute((CraterTable) metatable, getLanguage().getLenMetamethodKey());
            if (hasMetamethodProfile.profile(!isNil(metamethod))) {
                return adjustToOneValueNode.execute(metamethodInvokeNode.execute(metamethod, new Object[]{operand}));
            }
        }

        if (!(operand instanceof CraterTable table)) {
            transferToInterpreter();
            throw error("");
        }

        return rawLengthNode.execute(table);
    }
}
