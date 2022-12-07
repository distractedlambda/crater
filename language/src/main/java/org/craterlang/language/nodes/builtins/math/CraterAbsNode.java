package org.craterlang.language.nodes.builtins.math;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.builtins.CraterUnaryBuiltinBodyNode;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterAbsNode extends CraterUnaryBuiltinBodyNode {
    @Specialization
    long doLong(long argument) {
        return Math.abs(argument);
    }

    @Specialization
    double doDouble(double argument) {
        return Math.abs(argument);
    }

    @Fallback
    Object doInvalid(Object argument) {
        transferToInterpreter();
        throw error("");
    }
}
