package org.craterlang.language.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.craterlang.language.CraterNode;

public abstract class InstructionNode extends CraterNode {
    public abstract int execute(VirtualFrame frame);

    public abstract InstructionNode cloneUninitialized();
}
