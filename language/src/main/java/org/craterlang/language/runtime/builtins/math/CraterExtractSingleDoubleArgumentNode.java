package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMultipleValues;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
abstract class CraterExtractSingleDoubleArgumentNode extends CraterNode {
    abstract double execute(Object arguments);

    @Specialization
    double doLong(long argument) {
        return argument;
    }

    @Specialization
    double doDouble(double argument) {
        return argument;
    }

    @Specialization(guards = "arguments.isLong(0)")
    double doMultipleValuesLong(CraterMultipleValues arguments) {
        return arguments.getLong(0);
    }

    @Specialization(guards = "arguments.isDouble(0)")
    double doMultipleValuesDouble(CraterMultipleValues arguments) {
        return arguments.getDouble(0);
    }

    @Fallback
    double doInvalid(Object arguments) {
        transferToInterpreter();
        throw error("");
    }
}