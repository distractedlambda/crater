package org.craterlang.language.runtime;

public final class CraterResumptionPoint {
    private CraterResumptionPoint() {}

    public static CraterResumptionPoint create() {
        return new CraterResumptionPoint();
    }
}
