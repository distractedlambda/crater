package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMath;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
public abstract class CraterForceIntoIntegerNode extends CraterNode {
    public abstract long execute(Object index);

    @Specialization
    long doLong(long index) {
        return index;
    }

    @Specialization
    long doDouble(double index) {
        if (!CraterMath.hasExactLongValue(index)) {
            transferToInterpreter();
            throw error("");
        }

        return (long) index;
    }

    @Fallback
    long doOther(Object index) {
        transferToInterpreter();
        throw error("");
    }
}
