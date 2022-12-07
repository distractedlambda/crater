package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild(value = "operandNode", type = CraterExpressionNode.class)
abstract class CraterUnaryExpressionNode extends CraterExpressionNode {
    protected abstract CraterExpressionNode getOperandNode();
}
