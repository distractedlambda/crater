package org.craterlang.language.runtime;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;

public final class CraterClosure extends DynamicObject implements TruffleObject {
    public CraterClosure(Shape shape) {
        super(shape);
    }

    public HiddenKey getMetatableKey() {
        return CraterTable.getMetatableKey();
    }
}
