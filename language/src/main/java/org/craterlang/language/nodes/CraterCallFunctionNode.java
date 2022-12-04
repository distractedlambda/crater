package org.craterlang.language.nodes;

import org.craterlang.language.CraterNode;

public abstract class CraterCallFunctionNode extends CraterNode {
    public abstract Object execute(Object continuation, Object[] arguments);
}
