package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild(value = "lhsNode", type = ExpressionNode.class)
@NodeChild(value = "rhsNode", type = ExpressionNode.class)
abstract class BinaryExpressionNode extends ExpressionNode {
    protected abstract ExpressionNode getLhsNode();

    protected abstract ExpressionNode getRhsNode();
}
