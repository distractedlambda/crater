package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterNegationExpressionNode extends CraterUnaryExpressionNode {
    @Specialization
    long doLong(long operand) {
        return -operand;
    }

    @Specialization
    double doDouble(double operand) {
        return -operand;
    }
}
