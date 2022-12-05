package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterDoubleToStringNode;

public abstract class CraterConcatExpressionNode extends CraterBinaryExpressionNode {
    @Specialization(guards = {"isCoercibleToString(lhs)", "isCoercibleToString(rhs)"})
    TruffleString doStrings(
        Object lhs,
        Object rhs,
        @Cached CoerceToStringNode coerceLhsToStringNode,
        @Cached CoerceToStringNode coerceRhsToStringNode,
        @Cached TruffleString.ConcatNode concatNode
    ) {
        var lhsString = coerceLhsToStringNode.execute(lhs);
        var rhsString = coerceRhsToStringNode.execute(rhs);
        return concatNode.execute(lhsString, rhsString, TruffleString.Encoding.BYTES, true);
    }

    static boolean isCoercibleToString(Object value) {
        return value instanceof TruffleString || value instanceof Long || value instanceof Double;
    }

    @Fallback
    Object doMetamethod(Object lhs, Object rhs, @Cached CraterBinaryMetamethodInvokeNode metamethodInvokeNode) {
        return metamethodInvokeNode.execute(lhs, rhs, getLanguage().getConcatMetamethodKey());
    }

    static abstract class CoerceToStringNode extends CraterNode {
        abstract TruffleString execute(Object value);

        @Specialization
        TruffleString doString(TruffleString value) {
            return value;
        }

        @Specialization
        TruffleString doLong(long value, @Cached TruffleString.FromLongNode fromLongNode) {
            return fromLongNode.execute(value, TruffleString.Encoding.BYTES, true);
        }

        @Specialization
        TruffleString doDouble(double value, @Cached CraterDoubleToStringNode doubleToStringNode) {
            return doubleToStringNode.execute(value);
        }
    }
}
