package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterString;

@GeneratePackagePrivate
public abstract class CraterConcatExpressionNode extends CraterBinaryExpressionNode {
    public static CraterConcatExpressionNode create(CraterExpressionNode lhsNode, CraterExpressionNode rhsNode) {
        return CraterConcatExpressionNodeGen.create(lhsNode, rhsNode);
    }

    @Override public CraterExpressionNode cloneUninitialized() {
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
    Object doMetamethod(Object lhs, Object rhs, @Cached CraterBinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getConcatMetamethodKey());
    }
}
