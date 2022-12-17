package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;

@GeneratePackagePrivate
public abstract class NegationExpressionNode extends UnaryExpressionNode {
    public static NegationExpressionNode create(ExpressionNode operandNode) {
        return NegationExpressionNodeGen.create(operandNode);
    }

    @Override public ExpressionNode cloneUninitialized() {
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
