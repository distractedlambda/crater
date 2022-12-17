package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild(value = "operandNode", type = ExpressionNode.class)
abstract class UnaryExpressionNode extends ExpressionNode {
    protected abstract ExpressionNode getOperandNode();
}
