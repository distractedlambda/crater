package org.craterlang.language.nodes.builtins;

import org.craterlang.language.CraterNode;

public abstract class BuiltinFunctionBodyNode extends CraterNode {
    public abstract Object execute(Object[] arguments, int argumentsStart, int argumentsLength);
}
