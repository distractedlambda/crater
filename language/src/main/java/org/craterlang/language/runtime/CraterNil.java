package org.craterlang.language.runtime;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import org.craterlang.language.CraterLanguage;

@ExportLibrary(InteropLibrary.class)
public final class CraterNil implements TruffleObject {
    private CraterNil() {}

    private static final CraterNil INSTANCE = new CraterNil();

    private static final int HASH_CODE = INSTANCE.hashCode();

    public static CraterNil getInstance() {
        return INSTANCE;
    }

    @Override public boolean equals(Object obj) {
        return obj == INSTANCE;
    }

    @Override public int hashCode() {
        return HASH_CODE;
    }

    @Override public String toString() {
        return "nil";
    }

    @ExportMessage
    static boolean hasLanguage(CraterNil receiver) {
        return true;
    }

    @ExportMessage
    static Class<CraterLanguage> getLanguage(CraterNil receiver) {
        return CraterLanguage.class;
    }

    @ExportMessage
    static String toDisplayString(CraterNil receiver, boolean allowSideEffects) {
        return "nil";
    }

    @ExportMessage
    static TriState isIdenticalOrUndefined(CraterNil receiver, Object other) {
        return TriState.valueOf(other == getInstance());
    }

    @ExportMessage
    static int identityHashCode(CraterNil receiver) {
        return HASH_CODE;
    }
}
