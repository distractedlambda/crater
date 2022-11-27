package org.craterlang.language.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public abstract class CraterSingleResultInstructionNode extends CraterInstructionNode {
    @Child private CraterLocalWriteNode resultWriteNode;

    protected CraterSingleResultInstructionNode(int resultSlot) {
        resultWriteNode = CraterLocalWriteNodeGen.create(resultSlot);
    }

    protected final void result(VirtualFrame frame, boolean value) {
        resultWriteNode.execute(frame, value);
    }

    protected final void result(VirtualFrame frame, long value) {
        resultWriteNode.execute(frame, value);
    }

    protected final void result(VirtualFrame frame, double value) {
        resultWriteNode.execute(frame, value);
    }

    protected final void result(VirtualFrame frame, Object value) {
        resultWriteNode.execute(frame, value);
    }
}
