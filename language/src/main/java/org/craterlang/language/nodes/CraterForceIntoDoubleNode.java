package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterForceIntoDoubleNode extends CraterNode {
    public abstract double execute(Object value);

    @Specialization
    double doLong(long value) {
        return value;
    }

    @Specialization
    double doDouble(double value) {
        return value;
    }

    @Fallback
    double doInvalid(Object value) {
        transferToInterpreter();
        throw error("");
    }
}
