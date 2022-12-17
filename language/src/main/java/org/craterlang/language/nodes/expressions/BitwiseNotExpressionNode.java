package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterMath;

@GeneratePackagePrivate
@ImportStatic(CraterMath.class)
public abstract class BitwiseNotExpressionNode extends UnaryExpressionNode {
    public static BitwiseNotExpressionNode create(ExpressionNode operandNode) {
        return BitwiseNotExpressionNodeGen.create(operandNode);
    }

    @Override public ExpressionNode cloneUninitialized() {
        return create(getOperandNode());
    }

    @Specialization
    long doLong(long operand) {
        return ~operand;
    }

    @Specialization(guards = "hasExactLongValue(operand)")
    long doDouble(double operand) {
        return ~((long) operand);
    }
}
