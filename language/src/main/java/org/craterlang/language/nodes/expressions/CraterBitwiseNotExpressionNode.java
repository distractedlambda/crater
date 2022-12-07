package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterMath;

@GeneratePackagePrivate
@ImportStatic(CraterMath.class)
public abstract class CraterBitwiseNotExpressionNode extends CraterUnaryExpressionNode {
    public static CraterBitwiseNotExpressionNode create(CraterExpressionNode operandNode) {
        return CraterBitwiseNotExpressionNodeGen.create(operandNode);
    }

    @Override public CraterExpressionNode cloneUninitialized() {
        return create(getOperandNode());
    }

    @Specialization
    long doLong(long operand) {
        return ~operand;
    }

    @Specialization(guards = "hasExactLongValue(operand)")
    long doDouble(double operand) {
        return ~((long) operand);
    }
}
