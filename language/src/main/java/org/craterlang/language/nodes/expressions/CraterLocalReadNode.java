package org.craterlang.language.nodes.expressions;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GeneratePackagePrivate
@ReportPolymorphism.Exclude
public abstract class CraterLocalReadNode extends CraterExpressionNode {
    final int slot;

    CraterLocalReadNode(int slot) {
        assert slot >= 0;
        this.slot = slot;
    }

    public static CraterLocalReadNode create(int slot) {
        return CraterLocalReadNodeGen.create(slot);
    }

    @Override public CraterExpressionNode cloneUninitialized() {
        return create(slot);
    }

    @Specialization(guards = "frame.isBoolean(slot)")
    boolean doBoolean(VirtualFrame frame) {
        return frame.getBoolean(slot);
    }

    @Specialization(guards = "frame.isLong(slot)")
    long doLong(VirtualFrame frame) {
        return frame.getLong(slot);
    }

    @Specialization(guards = "frame.isDouble(slot)")
    double doDouble(VirtualFrame frame) {
        return frame.getDouble(slot);
    }

    @Specialization(replaces = {"doBoolean", "doLong", "doDouble"})
    Object doGeneric(VirtualFrame frame) {
        if (frame.isObject(slot)) {
            return frame.getObject(slot);
        }
        else {
            transferToInterpreter();
            var value = frame.getValue(slot);
            frame.setObject(slot, value);
            return value;
        }
    }
}
