package org.craterlang.language.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.craterlang.language.CraterLanguage;

public abstract class FunctionRootNode extends RootNode {
    private final SourceSection sourceSection;
    private final String qualifiedName;
    private final String name;

    public FunctionRootNode(
        CraterLanguage language,
        FrameDescriptor frameDescriptor,
        SourceSection sourceSection,
        String qualifiedName,
        String name
    ) {
        super(language, frameDescriptor);
        this.sourceSection = sourceSection;
        this.qualifiedName = qualifiedName;
        this.name = name;
    }

    @Override public final SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override public final String getQualifiedName() {
        return qualifiedName;
    }

    @Override public final String getName() {
        return name;
    }

    @Override protected final boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override protected abstract RootNode cloneUninitialized();

    protected static final int ARGUMENTS_START = 1;
}
