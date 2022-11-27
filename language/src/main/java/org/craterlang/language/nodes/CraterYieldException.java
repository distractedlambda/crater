package org.craterlang.language.nodes;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class CraterYieldException extends ControlFlowException {
    private CraterYieldException() {}

    private static final CraterYieldException INSTANCE = new CraterYieldException();

    public static CraterYieldException getInstance() {
        return INSTANCE;
    }
}
