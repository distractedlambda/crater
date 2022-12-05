package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

@GenerateUncached
public abstract class CraterAdjustToOneValueNode extends CraterNode {
    public abstract Object execute(Object values);

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
