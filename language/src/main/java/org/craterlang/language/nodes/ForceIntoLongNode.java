package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMath;
import org.craterlang.language.runtime.CraterString;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
@GeneratePackagePrivate
public abstract class ForceIntoLongNode extends CraterNode {
    public abstract long execute(Object value);

    public static ForceIntoLongNode create() {
        return ForceIntoLongNodeGen.create();
    }

    public static ForceIntoLongNode getUncached() {
        return ForceIntoLongNodeGen.getUncached();
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
