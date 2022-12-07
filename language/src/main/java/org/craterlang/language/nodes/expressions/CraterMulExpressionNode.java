package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;

@GeneratePackagePrivate
public abstract class CraterMulExpressionNode extends CraterBinaryExpressionNode {
    public static CraterMulExpressionNode create(CraterExpressionNode lhsNode, CraterExpressionNode rhsNode) {
        return CraterMulExpressionNodeGen.create(lhsNode, rhsNode);
    }

    @Override public CraterExpressionNode cloneUninitialized() {
        return create(getLhsNode(), getRhsNode());
    }

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

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached CraterBinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getMulMetamethodKey());
    }
}
