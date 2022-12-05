package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

public abstract class CraterPowerExpressionNode extends CraterBinaryExpressionNode {
    @Specialization
    double doLongLong(long lhs, long rhs) {
        return boundary(lhs, rhs);
    }

    @Specialization
    double doLongDouble(long lhs, double rhs) {
        return boundary(lhs, rhs);
    }

    @Specialization
    double doDoubleLong(double lhs, long rhs) {
        return boundary(lhs, rhs);
    }

    @Specialization
    double doDoubleDouble(double lhs, double rhs) {
        return boundary(lhs, rhs);
    }

    @TruffleBoundary(allowInlining = true)
    private static double boundary(double x, double p) {
        return Math.pow(x, p);
    }

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached CraterBinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getPowMetamethodKey());
    }
}
