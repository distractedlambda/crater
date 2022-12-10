package org.craterlang.language.nodes.builtins.math;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.nodes.builtins.CraterUnaryBuiltinBodyNode;

import java.util.function.Supplier;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GeneratePackagePrivate
public abstract class CraterAbsNode extends CraterUnaryBuiltinBodyNode {
    // FIXME: handle numeric string arguments

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

    public static Supplier<CraterAbsNode> getFactory() {
        return CraterAbsNodeGen::create;
    }
}
