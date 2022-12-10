package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;

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
    double doString(TruffleString value, @Cached TruffleString.ParseDoubleNode parseDoubleNode) {
        try {
            return parseDoubleNode.execute(value);
        }
        catch (TruffleString.NumberFormatException exception) {
            transferToInterpreter();
            throw error("");
        }
    }

    @Fallback
    double doInvalid(Object value) {
        transferToInterpreter();
        throw error("");
    }
}
