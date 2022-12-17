package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterNil;

@GeneratePackagePrivate
@ReportPolymorphism.Exclude
public abstract class LocalWriteNode extends CraterNode {
    private final int slot;

    LocalWriteNode(int slot) {
        assert slot >= 0;
        this.slot = slot;
    }

    public static LocalWriteNode create(int slot) {
        return LocalWriteNodeGen.create(slot);
    }

    public abstract void execute(VirtualFrame frame, boolean value);

    public abstract void execute(VirtualFrame frame, long value);

    public abstract void execute(VirtualFrame frame, double value);

    public abstract void execute(VirtualFrame frame, Object value);

    @Specialization(guards = "isIllegal(frame)")
    void doNoOp(VirtualFrame frame, CraterNil value) {
    }

    @Specialization(guards = "isBooleanOrIllegal(frame)")
    void doBoolean(VirtualFrame frame, boolean value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Boolean);
        frame.setBoolean(slot, value);
    }

    @Specialization(guards = "isLongOrIllegal(frame)")
    void doLong(VirtualFrame frame, long value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Long);
        frame.setLong(slot, value);
    }

    @Specialization(guards = "isDoubleOrIllegal(frame)")
    void doDouble(VirtualFrame frame, double value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Double);
        frame.setDouble(slot, value);
    }

    @Specialization(replaces = {"doNoOp", "doBoolean", "doLong", "doDouble"})
    void doGeneric(VirtualFrame frame, Object value) {
        frame.getFrameDescriptor().setSlotKind(slot, FrameSlotKind.Object);
        frame.setObject(slot, value);
    }

    boolean isIllegal(VirtualFrame frame) {
        return getSlotKind(frame) == FrameSlotKind.Illegal;
    }

    boolean isBooleanOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Boolean -> true;
            default -> false;
        };
    }

    boolean isLongOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Long -> true;
            default -> false;
        };
    }

    boolean isDoubleOrIllegal(VirtualFrame frame) {
        return switch (getSlotKind(frame)) {
            case Illegal, Double -> true;
            default -> false;
        };
    }

    private FrameSlotKind getSlotKind(VirtualFrame frame) {
        return frame.getFrameDescriptor().getSlotKind(slot);
    }
}
