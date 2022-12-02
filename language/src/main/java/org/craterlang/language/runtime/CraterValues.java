package org.craterlang.language.runtime;

public final class CraterValues {
    private CraterValues() {}

    public static Object[] getEmpty() {
        return EMPTY;
    }

    private static final Object[] EMPTY = new Object[0];
}
