package org.craterlang.language.runtime.builtins.math;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;

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

    @Specialization(guards = {"arguments.length > 1", "firstValueIsLong(arguments)"})
    double doMultipleValuesLong(Object[] arguments) {
        return (long) arguments[0];
    }

    @Specialization(guards = {"arguments.length > 1", "firstValueIsDouble(arguments)"})
    double doMultipleValuesDouble(Object[] arguments) {
        return (double) arguments[0];
    }

    @Fallback
    double doInvalid(Object arguments) {
        transferToInterpreter();
        throw error("");
    }

    static boolean firstValueIsLong(Object[] values) {
        return values[0] instanceof Long;
    }

    static boolean firstValueIsDouble(Object[] values) {
        return values[0] instanceof Double;
    }
}
