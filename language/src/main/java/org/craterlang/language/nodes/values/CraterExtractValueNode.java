package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterMultipleValues;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterNoValues;

@GenerateUncached
public abstract class CraterExtractValueNode extends CraterNode {
    public abstract Object execute(Object values, int index);

    @Specialization
    CraterNil doNoValues(CraterNoValues values, int index) {
        return CraterNil.getInstance();
    }

    @Specialization
    Object doMultipleValues(CraterMultipleValues values, int index) {
        return index <= values.getLength() ? values.get(index) : CraterNil.getInstance();
    }

    @Fallback
    Object doScalar(Object values, int index) {
        return index == 0 ? values : CraterNil.getInstance();
    }
}
