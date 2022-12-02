package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterSubNode extends CraterBinaryExpressionNode {
    @Specialization
    protected long doLongLong(long lhs, long rhs) {
        return lhs - rhs;
    }

    @Specialization
    protected double doLongDouble(long lhs, double rhs) {
        return lhs - rhs;
    }

    @Specialization
    protected double doDoubleLong(double lhs, long rhs) {
        return lhs - rhs;
    }

    @Specialization
    protected double doDoubleDouble(double lhs, double rhs) {
        return lhs - rhs;
    }
}
