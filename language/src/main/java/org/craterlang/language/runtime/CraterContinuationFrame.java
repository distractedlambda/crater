package org.craterlang.language.runtime;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.Shape;

public final class CraterContinuationFrame extends DynamicObject {
    private int codeOffset;

    public CraterContinuationFrame(Shape shape) {
        super(shape);
    }

    public int getCodeOffset() {
        return codeOffset;
    }

    public void setCodeOffset(int codeOffset) {
        this.codeOffset = codeOffset;
    }
}
