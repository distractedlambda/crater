package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.NodeChild;

@NodeChild(value = "lhsNode", type = CraterExpressionNode.class)
@NodeChild(value = "rhsNode", type = CraterExpressionNode.class)
abstract class CraterBinaryExpressionNode extends CraterExpressionNode {
    protected abstract CraterExpressionNode getLhsNode();

    protected abstract CraterExpressionNode getRhsNode();
}
