package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.runtime.CraterMath;

@ImportStatic(CraterMath.class)
public abstract class CraterBitwiseNotExpressionNode extends CraterUnaryExpressionNode {
    @Specialization
    long doLong(long operand) {
        return ~operand;
    }

    @Specialization(guards = "hasExactLongValue(operand)")
    long doDouble(double operand) {
        return ~((long) operand);
    }
}
