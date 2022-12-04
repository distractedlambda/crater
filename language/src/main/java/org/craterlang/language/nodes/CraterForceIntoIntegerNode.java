package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMath;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
public abstract class CraterForceIntoIntegerNode extends CraterNode {
    public abstract long execute(Object value);

    @Specialization
    long doLong(long value) {
        return value;
    }

    @Specialization
    long doDouble(double value) {
        if (!CraterMath.hasExactLongValue(value)) {
            transferToInterpreter();
            throw error("");
        }

        return (long) value;
    }

    @Fallback
    long doOther(Object value) {
        transferToInterpreter();
        throw error("");
    }
}
