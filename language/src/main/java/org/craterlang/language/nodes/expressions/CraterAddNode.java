package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "Add")
public abstract class CraterAddNode extends CraterBinaryExpressionNode {
    @Specialization
    protected long doLongLong(long lhs, long rhs) {
        return lhs + rhs;
    }

    @Specialization
    protected double doLongDouble(long lhs, double rhs) {
        return lhs + rhs;
    }

    @Specialization
    protected double doDoubleLong(double lhs, long rhs) {
        return lhs + rhs;
    }

    @Specialization
    protected double doDoubleDouble(double lhs, double rhs) {
        return lhs + rhs;
    }
}
