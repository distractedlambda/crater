package org.craterlang.language.runtime;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;

public final class CraterTable extends DynamicObject implements TruffleObject {
    public CraterTable(Shape shape) {
        super(shape);
    }

    public static HiddenKey getMetatableKey() {
        return METATABLE_KEY;
    }

    public static HiddenKey getSequenceStorageKey() {
        return SEQUENCE_STORAGE_KEY;
    }

    public static HiddenKey getSequenceLengthKey() {
        return SEQUENCE_LENGTH_KEY;
    }

    private static final HiddenKey METATABLE_KEY = new HiddenKey("metatable");
    private static final HiddenKey SEQUENCE_STORAGE_KEY = new HiddenKey("sequenceStorage");
    private static final HiddenKey SEQUENCE_LENGTH_KEY = new HiddenKey("sequenceLength");
}
