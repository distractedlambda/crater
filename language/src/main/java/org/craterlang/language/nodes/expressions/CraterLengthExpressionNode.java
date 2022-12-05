package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.nodes.CraterGetMetatableNode;
import org.craterlang.language.nodes.CraterInvokeNode;
import org.craterlang.language.nodes.values.CraterAdjustToOneValueNode;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.CraterTypeSystem.isNil;

public abstract class CraterLengthExpressionNode extends CraterUnaryExpressionNode {
    @Specialization
    long doString(TruffleString operand) {
        return operand.byteLength(TruffleString.Encoding.BYTES);
    }

    @Fallback
    Object doOther(
        Object operand,
        @Cached CraterGetMetatableNode getMetatableNode,
        @Cached ConditionProfile hasMetatableProfile,
        @Cached CraterTable.RawGetNode getMetamethodNode,
        @Cached ConditionProfile hasMetamethodProfile,
        @Cached CraterInvokeNode metamethodInvokeNode,
        @Cached CraterAdjustToOneValueNode adjustToOneValueNode,
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
