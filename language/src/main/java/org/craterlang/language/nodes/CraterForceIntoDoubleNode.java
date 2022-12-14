package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterString;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterForceIntoDoubleNode extends CraterNode {
    public abstract double execute(Object value);

    public static CraterForceIntoDoubleNode create() {
        return CraterForceIntoDoubleNodeGen.create();
    }

    @Specialization
    double doLong(long value) {
        return value;
    }

    @Specialization
    double doDouble(double value) {
        return value;
    }

    @Specialization
    double doString(CraterString value, @Cached CraterString.ParseDoubleNode parseDoubleNode) {
        return parseDoubleNode.execute(value);
    }

    @Fallback
    double doInvalid(Object value) {
        transferToInterpreter();
        throw error("");
    }
}
