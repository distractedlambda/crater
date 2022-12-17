package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

@GenerateUncached
@GeneratePackagePrivate
public abstract class ExtractValueNode extends CraterNode {
    public abstract Object execute(Object values, int index);

    public static ExtractValueNode create() {
        return ExtractValueNodeGen.create();
    }

    public static ExtractValueNode getUncached() {
        return ExtractValueNodeGen.getUncached();
    }

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
