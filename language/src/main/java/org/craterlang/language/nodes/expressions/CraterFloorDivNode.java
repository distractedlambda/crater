package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterFloorDivNode extends CraterBinaryExpressionNode {
    @Specialization
    protected long doLongLong(long lhs, long rhs) {
        try {
            return Math.floorDiv(lhs, rhs);
        }
        catch (ArithmeticException exception) {
            transferToInterpreter();
            throw error("Attempt to divide by zero");
        }
    }

    @Specialization
    protected double doLongDouble(long lhs, double rhs) {
        return doDoubleDouble(lhs, rhs);
    }

    @Specialization
    protected double doDoubleLong(double lhs, long rhs) {
        return doDoubleDouble(lhs, rhs);
    }

    @Specialization
    protected double doDoubleDouble(double lhs, double rhs) {
        return floorBoundary(lhs / rhs);
    }

    @TruffleBoundary(allowInlining = true)
    private static double floorBoundary(double x) {
        return Math.floor(x);
    }
}
