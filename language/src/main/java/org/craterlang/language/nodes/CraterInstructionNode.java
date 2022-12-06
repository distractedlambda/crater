package org.craterlang.language.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.craterlang.language.CraterNode;

public abstract class CraterInstructionNode extends CraterNode {
    public abstract int execute(VirtualFrame frame);

    public abstract CraterInstructionNode cloneUninitialized();
}
