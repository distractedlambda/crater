package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.IntValueProfile;
import org.craterlang.language.CraterLanguage;
import org.craterlang.language.nodes.FunctionRootNode;

import java.util.function.Supplier;

public final class BuiltinFunctionRootNode extends FunctionRootNode {
    private final Supplier<BuiltinFunctionBodyNode> bodyNodeFactory;
    @Child private BuiltinFunctionBodyNode bodyNode;
    private final IntValueProfile argumentsLengthProfile;

    public BuiltinFunctionRootNode(
        CraterLanguage language,
        String qualifiedName,
        String name,
        Supplier<BuiltinFunctionBodyNode> bodyNodeFactory
    ) {
        super(language, null, null, qualifiedName, name);
        this.bodyNodeFactory = bodyNodeFactory;
        this.bodyNode = bodyNodeFactory.get();
        this.argumentsLengthProfile = IntValueProfile.create();
    }

    @Override public Object execute(VirtualFrame frame) {
        var arguments = frame.getArguments();
        var profiledLength = argumentsLengthProfile.profile(arguments.length);
        return bodyNode.execute(arguments, ARGUMENTS_START, profiledLength - ARGUMENTS_START);
    }

    @Override protected RootNode cloneUninitialized() {
        return new BuiltinFunctionRootNode(
            getLanguage(CraterLanguage.class),
            getQualifiedName(),
            getName(),
            bodyNodeFactory
        );
    }
}
