package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterDivExpressionNode extends CraterBinaryExpressionNode {
    @Specialization
    double doLongLong(long lhs, long rhs) {
        return (double) lhs / (double) rhs;
    }

    @Specialization
    double doLongDouble(long lhs, double rhs) {
        return lhs / rhs;
    }

    @Specialization
    double doDoubleLong(double lhs, long rhs) {
        return lhs / rhs;
    }

    @Specialization
    double doDoubleDouble(double lhs, double rhs) {
        return lhs / rhs;
    }

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached CraterBinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getDivMetamethodKey());
    }
}
