package org.craterlang.language.runtime;

import com.oracle.truffle.api.interop.TruffleObject;

public final class CraterNoValues implements TruffleObject {
    private CraterNoValues() {}

    public static CraterNoValues getInstance() {
        return INSTANCE;
    }

    private static final CraterNoValues INSTANCE = new CraterNoValues();
}
