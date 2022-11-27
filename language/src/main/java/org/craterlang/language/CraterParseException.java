package org.craterlang.language;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.EncapsulatingNodeReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public final class CraterParseException extends AbstractTruffleException {
    private final SourceSection sourceSection;

    private CraterParseException(SourceSection sourceSection, String message, Throwable cause, Node location) {
        super(message, cause, AbstractTruffleException.UNLIMITED_STACK_TRACE, location);
        this.sourceSection = sourceSection;
    }

    public static CraterParseException create(SourceSection sourceSection, String message, Throwable cause) {
        return new CraterParseException(sourceSection, message, cause, EncapsulatingNodeReference.getCurrent().get());
    }

    public static CraterParseException create(SourceSection sourceSection, String message) {
        return create(sourceSection, message, null);
    }

    public static CraterParseException create(SourceSection sourceSection, Throwable cause) {
        return create(sourceSection, null, cause);
    }
}
