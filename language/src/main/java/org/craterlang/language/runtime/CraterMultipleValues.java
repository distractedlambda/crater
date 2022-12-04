package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.ValueType;
import com.oracle.truffle.api.interop.TruffleObject;

import java.util.Arrays;

@ValueType
public final class CraterMultipleValues implements TruffleObject {
    @CompilationFinal(dimensions = 1) private final Object[] values;

    public CraterMultipleValues(Object... values) {
        assert values.length > 1;
        this.values = values;
    }

    public int getLength() {
        return values.length;
    }

    public Object get(int index) {
        return values[index];
    }

    @TruffleBoundary(allowInlining = true)
    public Object[] getCopyOfValues() {
        return Arrays.copyOf(values, values.length, Object[].class);
    }

    public boolean isBoolean(int index) {
        return values[index] instanceof Boolean;
    }

    public boolean getBoolean(int index) {
        return (boolean) values[index];
    }

    public boolean isLong(int index) {
        return values[index] instanceof Long;
    }

    public long getLong(int index) {
        return (long) values[index];
    }

    public boolean isDouble(int index) {
        return values[index] instanceof Double;
    }

    public double getDouble(int index) {
        return (double) values[index];
    }

    public static CraterMultipleValues getEmpty() {
        return EMPTY;
    }

    private static final CraterMultipleValues EMPTY = new CraterMultipleValues();
}
