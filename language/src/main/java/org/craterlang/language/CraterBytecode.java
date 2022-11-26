package org.craterlang.language;

import org.craterlang.language.nodes.CraterInstructionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;

public final class CraterBytecode {
    private CraterBytecode() {}

    public record Assembled(
        byte[] bytecode,
        CraterInstructionNode[] instructionNodes,
        int conditionProfileCount,
        int loopProfileCount
    ) {}

    public static Assembled assemble(List<Instruction> instructions) {
        var bytecodeSize = 0;

        for (var instruction : instructions) {
            bytecodeSize = addExact(bytecodeSize, instruction.getByteSize());
        }

        var context = new AssemblyContext(bytecodeSize);

        for (var instruction : instructions) {
            var instructionOffset = context.bytecodeOffset;
            instruction.assemble(context);
            instruction.assembledOffset = instructionOffset;

            if (instruction.unresolvedBranches != null) {
                for (var compressedBranchAddressOffset : instruction.unresolvedBranches) {
                    var branchAddressOffset = Short.toUnsignedInt(compressedBranchAddressOffset);
                    context.bytecode[branchAddressOffset] = (byte) instructionOffset;
                    context.bytecode[branchAddressOffset + 1] = (byte) (instructionOffset >> 8);
                }

                instruction.unresolvedBranches = null;
            }
        }

        assert context.bytecodeOffset == bytecodeSize;

        return new Assembled(
            context.bytecode,
            context.instructionNodes.toArray(CraterInstructionNode[]::new),
            context.conditionProfileCount,
            context.loopProfileCount
        );
    }

    /**
     * Format: <code>[OP_EXEC][nodeIndex: u16]</code>
     */
    public static final byte OP_EXEC = 0;

    /**
     * Format: <code>[OP_BR][target: u16]</code>
     */
    public static final byte OP_BR = 1;

    /**
     * Format: <code>[OP_BR_IF][profileIndex: u16][conditionSlot: u16][target: u16]</code>
     */
    public static final byte OP_BR_IF = 2;

    /**
     * Format: <code>[OP_BR_LOOP][profileIndex: u16][conditionSlot: u16][target: u16]</code>
     */
    public static final byte OP_BR_LOOP = 3;

    /**
     * Format: <code>[OP_CLEAR][slot: u16]</code>
     */
    public static final byte OP_CLEAR = 4;

    /**
     * Format: <code>[OP_COPY][srcSlot: u16][dstSlot: u16]</code>
     */
    public static final byte OP_COPY = 5;

    /**
     * Format: <code>[OP_SWAP][slotA: u16][slotB: u16]</code>
     */
    public static final byte OP_SWAP = 6;

    /**
     * Format: <code>[OP_RETURN_NIL]</code>
     */
    public static final byte OP_RETURN_NIL = 7;

    /**
     * Format: <code>[OP_RETURN_SINGLE][valueSlot: u16]</code>
     */
    public static final byte OP_RETURN_SINGLE = 8;

    /**
     * Format: <code>[OP_RETURN_MULTIPLE][valueCount: u16][valueSlots: u16 * valueCount]</code>
     */
    public static final byte OP_RETURN_MULTIPLE = 9;

    private static final class AssemblyContext {
        private final byte[] bytecode;
        private final List<CraterInstructionNode> instructionNodes = new ArrayList<>();

        private int bytecodeOffset;
        private int conditionProfileCount;
        private int loopProfileCount;

        private AssemblyContext(int bytecodeLength) {
            assert bytecodeLength >= 0 && bytecodeLength <= 65535;
            bytecode = new byte[bytecodeLength];
        }

        private AssemblyContext putByte(byte value) {
            bytecode[bytecodeOffset++] = value;
            return this;
        }

        private AssemblyContext putU16(int value) {
            if (value < 0 || value > 65535) {
                throw new UnsupportedOperationException("Value is out of range: " + value);
            }

            bytecode[bytecodeOffset++] = (byte) value;
            bytecode[bytecodeOffset++] = (byte) (value >> 8);
            return this;
        }

        private AssemblyContext putInstructionOffset(Instruction instruction) {
            if (instruction.assembledOffset >= 0) {
                putU16(instruction.assembledOffset);
            }
            else if (instruction.unresolvedBranches == null) {
                instruction.unresolvedBranches = new short[]{(short) bytecodeOffset};
            }
            else {
                var oldLength = instruction.unresolvedBranches.length;
                instruction.unresolvedBranches = Arrays.copyOf(instruction.unresolvedBranches, oldLength + 1);
                instruction.unresolvedBranches[oldLength] = (short) bytecodeOffset;
            }
            return this;
        }
    }

    public static abstract sealed class Instruction {
        private int assembledOffset = -1;
        private short[] unresolvedBranches;

        private int sourceStart = -1;
        private int sourceLength;

        protected abstract int getByteSize();

        protected abstract void assemble(AssemblyContext context);

        public final void setSourceRange(int start, int length) {
            sourceStart = start;
            sourceLength = length;
        }
    }

    public static final class Exec extends Instruction {
        private CraterInstructionNode node;

        @Override protected int getByteSize() {
            return 3;
        }

        @Override protected void assemble(AssemblyContext context) {
            assert node != null;
            context.instructionNodes.add(node);
            context.putByte(OP_EXEC).putU16(context.instructionNodes.size() - 1);
        }

        public void setNode(CraterInstructionNode node) {
            this.node = node;
        }
    }

    public static final class Br extends Instruction {
        private Instruction target;

        @Override protected int getByteSize() {
            return 3;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_BR).putInstructionOffset(target);
        }

        public void setTarget(Instruction target) {
            this.target = target;
        }
    }

    public static final class BrIf extends Instruction {
        private int conditionSlot = -1;
        private Instruction target;

        @Override protected int getByteSize() {
            return 7;
        }

        @Override protected void assemble(AssemblyContext context) {
            context
                .putByte(OP_BR_IF)
                .putU16(context.conditionProfileCount++)
                .putU16(conditionSlot)
                .putInstructionOffset(target);
        }

        public void setConditionSlot(int conditionSlot) {
            this.conditionSlot = conditionSlot;
        }

        public void setTarget(Instruction target) {
            this.target = target;
        }
    }

    public static final class BrLoop extends Instruction {
        private int conditionSlot = -1;
        private Instruction target;

        @Override protected int getByteSize() {
            return 7;
        }

        @Override protected void assemble(AssemblyContext context) {
            context
                .putByte(OP_BR_LOOP)
                .putU16(context.loopProfileCount++)
                .putU16(conditionSlot)
                .putInstructionOffset(target);
        }

        public void setConditionSlot(int conditionSlot) {
            this.conditionSlot = conditionSlot;
        }

        public void setTarget(Instruction target) {
            this.target = target;
        }
    }

    public static final class Clear extends Instruction {
        private int slot = -1;

        @Override protected int getByteSize() {
            return 3;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_CLEAR).putU16(slot);
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }
    }

    public static final class Copy extends Instruction {
        private int sourceSlot = -1;
        private int destinationSlot = -1;

        @Override protected int getByteSize() {
            return 5;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_COPY).putU16(sourceSlot).putU16(destinationSlot);
        }

        public void setSourceSlot(int sourceSlot) {
            this.sourceSlot = sourceSlot;
        }

        public void setDestinationSlot(int destinationSlot) {
            this.destinationSlot = destinationSlot;
        }
    }

    public static final class Swap extends Instruction {
        private int firstSlot = -1;
        private int secondSlot = -1;

        @Override protected int getByteSize() {
            return 5;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_SWAP).putU16(firstSlot).putU16(secondSlot);
        }

        public void setFirstSlot(int firstSlot) {
            this.firstSlot = firstSlot;
        }

        public void setSecondSlot(int secondSlot) {
            this.secondSlot = secondSlot;
        }
    }

    public static final class ReturnNil extends Instruction {
        @Override protected int getByteSize() {
            return 1;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_RETURN_NIL);
        }
    }

    public static final class ReturnSingle extends Instruction {
        private int slot = -1;

        @Override protected int getByteSize() {
            return 3;
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_RETURN_SINGLE).putU16(slot);
        }

        public void setSlot(int slot) {
            this.slot = slot;
        }
    }

    public static final class ReturnMultiple extends Instruction {
        private int[] slots;

        @Override protected int getByteSize() {
            return addExact(3, multiplyExact(slots.length, 2));
        }

        @Override protected void assemble(AssemblyContext context) {
            context.putByte(OP_RETURN_MULTIPLE).putU16(slots.length);
            for (var slot : slots) {
                context.putU16(slot);
            }
        }

        public void setSlots(int[] slots) {
            this.slots = slots;
        }
    }
}
