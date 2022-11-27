package org.craterlang.language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CraterChunkCompiler {
    private static final class FunctionScope {
        private final Map<String, Var> declaredVars = new HashMap<>();
    }

    private static final class Var {}

    private static final class Block {
        private final List<Instruction> instructions = new ArrayList<>();
    }

    private static abstract sealed class Instruction {
    }

    private static final class ReturnInstruction extends Instruction {
        private List<Instruction> values;
    }

    private static final class LoadInstruction extends Instruction {
        private Var var;
    }

    private static final class StoreInstruction extends Instruction {
        private Var var;
        private Instruction value;
    }

    private static final class ConstantInstruction extends Instruction {
        private Object value;
    }

    private static final class MergeInstruction extends Instruction {
        private Instruction first, second;
    }

    private static final class BranchInstruction extends Instruction {
        private Block target;
    }

    private static abstract sealed class ConditionalBranchInstruction extends Instruction {
        private Instruction condition;
        private Block target;
    }

    private static final class BranchIfInstruction extends ConditionalBranchInstruction {
    }

    private static final class LoopIfInstruction extends ConditionalBranchInstruction {
    }

    private static final class NewindexInstruction extends Instruction {
        private Instruction receiver;
        private Instruction key;
        private Instruction value;
    }

    private static abstract sealed class CallInstruction extends Instruction {
        private Instruction callee;
        private List<Instruction> arguments;
    }

    private static final class NonTailCallInstruction extends CallInstruction {
    }

    private static final class TailCallInstruction extends CallInstruction {
    }

    private static final class UnopInstruction extends Instruction {
        private Op op;
        private Instruction operand;

        private enum Op {
            BNOT,
            LEN,
            NOT,
            UNM,
        }
    }

    private static final class BinopInstruction extends Instruction {
        private Op op;
        private Instruction lhs;
        private Instruction rhs;

        private enum Op {
            ADD,
            BAND,
            BOR,
            BXOR,
            CONCAT,
            DIV,
            EQ,
            GE,
            GT,
            IDIV,
            INDEX,
            LE,
            LT,
            MOD,
            MUL,
            NE,
            POW,
            SHL,
            SHR,
            SUB,
        }
    }
}
