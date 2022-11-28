package org.craterlang.language.runtime;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.EncapsulatingNodeReference;
import com.oracle.truffle.api.nodes.Node;

import static com.oracle.truffle.api.CompilerAsserts.partialEvaluationConstant;

public final class CraterErrorException extends AbstractTruffleException {
    private final Object payload;

    private CraterErrorException(Object payload, Node location) {
        super(location);
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public static CraterErrorException create(Object payload, Node location) {
        assert payload != null;
        partialEvaluationConstant(location);

        if (location == null || !location.isAdoptable()) {
            location = EncapsulatingNodeReference.getCurrent().get();
        }

        return new CraterErrorException(payload, location);
    }
}
