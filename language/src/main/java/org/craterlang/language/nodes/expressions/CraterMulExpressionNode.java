package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterMulExpressionNode extends CraterBinaryExpressionNode {
    @Specialization
    long doLongLong(long lhs, long rhs) {
        return lhs * rhs;
    }

    @Specialization
    double doLongDouble(long lhs, double rhs) {
        return lhs * rhs;
    }

    @Specialization
    double doDoubleLong(double lhs, long rhs) {
        return lhs * rhs;
    }

    @Specialization
    double doDoubleDouble(double lhs, double rhs) {
        return lhs * rhs;
    }
}
