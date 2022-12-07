package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

public abstract class CraterExtractValueNode extends CraterNode {
    public abstract Object execute(Object values, int index);

    @Specialization(guards = "values.length == 0")
    CraterNil doNoValues(Object[] values, int index) {
        return CraterNil.getInstance();
    }

    @Specialization(guards = "values.length > 0")
    Object doMultipleValues(Object[] values, int index) {
        return index <= values.length ? values[index] : CraterNil.getInstance();
    }

    @Fallback
    Object doScalar(Object values, int index) {
        return index == 0 ? values : CraterNil.getInstance();
    }
}
