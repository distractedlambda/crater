package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.interop.TruffleObject;
import org.craterlang.language.CraterNode;

public final class CraterBuiltinFunction implements TruffleObject {
    private final CraterString name;
    private final NodeFactory<ExecutorNode> nodeFactory;
    private final NodeFactory<ExecutorNode> tailNodeFactory;

    public CraterBuiltinFunction(
        CraterString name,
        NodeFactory<ExecutorNode> nodeFactory,
        NodeFactory<ExecutorNode> tailNodeFactory
    ) {
        this.name = name;
        this.nodeFactory = nodeFactory;
        this.tailNodeFactory = tailNodeFactory;
    }

    public CraterBuiltinFunction(CraterString name, NodeFactory<ExecutorNode> nodeFactory) {
        this(name, nodeFactory, nodeFactory);
    }

    public CraterString getName() {
        return name;
    }

    public ExecutorNode createExecutorNode() {
        return nodeFactory.createNode();
    }

    public ExecutorNode createTailExecutorNode() {
        return tailNodeFactory.createNode();
    }

    public Object executeUncached(Object[] arguments) {
        return nodeFactory.getUncachedInstance().execute(arguments);
    }

    public Object tailExecuteUncached(Object[] arguments) {
        return tailNodeFactory.getUncachedInstance().execute(arguments);
    }

    @GenerateNodeFactory
    public abstract static class ExecutorNode extends CraterNode {
        public abstract Object execute(Object[] arguments);
    }
}
