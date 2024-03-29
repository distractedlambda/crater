package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterMath;

@GeneratePackagePrivate
@ImportStatic(CraterMath.class)
public abstract class BitwiseOrExpressionNode extends BinaryExpressionNode {
    public static BitwiseOrExpressionNode create(ExpressionNode lhsNode, ExpressionNode rhsNode) {
        return BitwiseOrExpressionNodeGen.create(lhsNode, rhsNode);
    }

    @Override public ExpressionNode cloneUninitialized() {
        return create(getLhsNode(), getRhsNode());
    }

    @Specialization
    long doLongLong(long lhs, long rhs) {
        return lhs | rhs;
    }

    @Specialization(guards = "hasExactLongValue(rhs)")
    long doLongDouble(long lhs, double rhs) {
        return lhs | (long) rhs;
    }

    @Specialization(guards = "hasExactLongValue(lhs)")
    long doDoubleLong(double lhs, long rhs) {
        return (long) lhs | rhs;
    }

    @Specialization(guards = {"hasExactLongValue(lhs)", "hasExactLongValue(rhs)"})
    long doDoubleDouble(double lhs, double rhs) {
        return (long) lhs | (long) rhs;
    }

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached BinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getBorMetamethodKey());
    }
}
