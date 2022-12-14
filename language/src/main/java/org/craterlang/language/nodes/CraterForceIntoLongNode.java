package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMath;
import org.craterlang.language.runtime.CraterString;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterForceIntoLongNode extends CraterNode {
    public abstract long execute(Object value);

    public static CraterForceIntoLongNode create() {
        return CraterForceIntoLongNodeGen.create();
    }

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

    @Specialization
    long doString(CraterString value, @Cached CraterString.ParseLongNode parseLongNode) {
        return parseLongNode.execute(value);
    }

    @Fallback
    long doOther(Object value) {
        transferToInterpreter();
        throw error("");
    }
}
