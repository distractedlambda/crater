package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterString;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
@GeneratePackagePrivate
public abstract class ForceIntoDoubleNode extends CraterNode {
    public abstract double execute(Object value);

    public static ForceIntoDoubleNode create() {
        return ForceIntoDoubleNodeGen.create();
    }

    public static ForceIntoDoubleNode getUncached() {
        return ForceIntoDoubleNodeGen.getUncached();
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
