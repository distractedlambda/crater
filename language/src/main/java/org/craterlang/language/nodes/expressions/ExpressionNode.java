package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.craterlang.language.CraterNode;
import org.craterlang.language.CraterTypeSystemGen;

public abstract class ExpressionNode extends CraterNode {
    public abstract ExpressionNode cloneUninitialized();

    public abstract Object executeGeneric(VirtualFrame frame);

    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return CraterTypeSystemGen.expectBoolean(executeGeneric(frame));
    }

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return CraterTypeSystemGen.expectLong(executeGeneric(frame));
    }

    public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
        return CraterTypeSystemGen.expectDouble(executeGeneric(frame));
    }
}
