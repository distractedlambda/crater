package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.TruffleObject;

@ValueType
public final class CraterMultipleValues implements TruffleObject {
    @CompilationFinal(dimensions = 1) private final Object[] values;

    public CraterMultipleValues(Object... values) {
        assert values.length != 1;
        this.values = values;
    }

    public int getLength() {
        return values.length;
    }

    public Object get(int index) {
        return values[index];
    }

    public static CraterMultipleValues getEmpty() {
        return EMPTY;
    }

    private static final CraterMultipleValues EMPTY = new CraterMultipleValues();
}
