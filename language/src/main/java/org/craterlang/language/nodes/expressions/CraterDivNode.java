package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterDivNode extends CraterBinaryExpressionNode {
    @Specialization
    protected double doLongLong(long lhs, long rhs) {
        return (double) lhs / (double) rhs;
    }

    @Specialization
    protected double doLongDouble(long lhs, double rhs) {
        return lhs / rhs;
    }

    @Specialization
    protected double doDoubleLong(double lhs, long rhs) {
        return lhs / rhs;
    }

    @Specialization
    protected double doDoubleDouble(double lhs, double rhs) {
        return lhs / rhs;
    }
}
