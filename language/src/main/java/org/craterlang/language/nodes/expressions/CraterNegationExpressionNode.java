package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;

@GeneratePackagePrivate
public abstract class CraterNegationExpressionNode extends CraterUnaryExpressionNode {
    public static CraterNegationExpressionNode create(CraterExpressionNode operandNode) {
        return CraterNegationExpressionNodeGen.create(operandNode);
    }

    @Override public CraterExpressionNode cloneUninitialized() {
        return create(getOperandNode());
    }

    @Specialization
    long doLong(long operand) {
        return -operand;
    }

    @Specialization
    double doDouble(double operand) {
        return -operand;
    }
}
