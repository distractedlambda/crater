package org.craterlang.language.nodes.builtins;

import org.craterlang.language.CraterNode;

public abstract class CraterBuiltinBodyNode extends CraterNode {
    public abstract Object execute(Object[] arguments, int argumentsStart, int argumentsLength);
}
