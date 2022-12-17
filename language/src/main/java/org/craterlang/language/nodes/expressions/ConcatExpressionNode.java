package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterString;

@GeneratePackagePrivate
public abstract class ConcatExpressionNode extends BinaryExpressionNode {
    public static ConcatExpressionNode create(ExpressionNode lhsNode, ExpressionNode rhsNode) {
        return ConcatExpressionNodeGen.create(lhsNode, rhsNode);
    }

    @Override public ExpressionNode cloneUninitialized() {
        return create(getLhsNode(), getRhsNode());
    }

    @Specialization(guards = {"isCoercibleToString(lhs)", "isCoercibleToString(rhs)"})
    CraterString doStrings(Object lhs, Object rhs) {
        // TODO
        return null;
    }

    static boolean isCoercibleToString(Object value) {
        return value instanceof CraterString || value instanceof Long || value instanceof Double;
    }

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached BinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getConcatMetamethodKey());
    }
}
