package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild(value = "operandNode", type = CraterExpressionNode.class)
public abstract class CraterUnaryExpressionNode extends CraterExpressionNode {
}
