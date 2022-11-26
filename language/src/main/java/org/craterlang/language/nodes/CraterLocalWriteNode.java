package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

@ReportPolymorphism.Exclude
public abstract class CraterLocalWriteNode extends CraterNode {
    private final int slot;

    protected CraterLocalWriteNode(int slot) {
        assert slot >= 0;
        this.slot = slot;
    }

    public abstract void execute(VirtualFrame frame, boolean value);

    public abstract void execute(VirtualFrame frame, long value);

    public abstract void execute(VirtualFrame frame, double value);

    public abstract void execute(VirtualFrame frame, Object value);

    @Specialization(guards = "isIllegal(frame)")
    protected void doNoOp(VirtualFrame frame, CraterNil value) {
    }

    @Specialization(guards = "isBooleanOrIllegal(frame)")
    protected void doBoolean(VirtualFrame frame, boolean value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
    }

    @Specialization(guards = "isLongOrIllegal(frame)")
    protected void doLong(VirtualFrame frame, long value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Long);
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDoubleOrIllegal(frame)")
    protected void doDouble(VirtualFrame frame, double value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Double);
        frame.setDouble(slot, value);
    }

    @Specialization(replaces = {"doNoOp", "doBoolean", "doLong", "doDouble"})
    protected void doGeneric(VirtualFrame frame, Object value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Object);
        frame.setObject(slot, value);
    }

    protected boolean isIllegal(VirtualFrame frame) {
        return getSlotKind(frame) == FrameSlotKind.Illegal;
    }

    protected boolean isBooleanOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Boolean -> true;
            default -> false;
        };
    }

    protected boolean isLongOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Long -> true;
            default -> false;
        };
    }

    protected boolean isDoubleOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Double -> true;
            default -> false;
        };
    }

    private FrameSlotKind getSlotKind(VirtualFrame frame) {
        return frame.getFrameDescriptor().getSlotKind(slot);
    }
}
