package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

@GenerateUncached
@GeneratePackagePrivate
public abstract class AdjustToOneValueNode extends CraterNode {
    public abstract Object execute(Object values);

    public static AdjustToOneValueNode create() {
        return AdjustToOneValueNodeGen.create();
    }

    public static AdjustToOneValueNode getUncached() {
        return AdjustToOneValueNodeGen.getUncached();
    }

    @Specialization(guards = "values.length == 0")
    CraterNil doEmpty(Object[] values) {
        return CraterNil.getInstance();
    }

    @Specialization(guards = "values.length > 0")
    Object doNonEmpty(Object[] values) {
        return values[0];
    }

    @Fallback
    Object doScalar(Object value) {
        return value;
    }
}
