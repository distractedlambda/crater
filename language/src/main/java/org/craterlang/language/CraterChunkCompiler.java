package org.craterlang.language;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.craterlang.language.CraterParser.AddSubExpressionContext;
import org.craterlang.language.CraterParser.AndExpressionContext;
import org.craterlang.language.CraterParser.AssignmentStatementContext;
import org.craterlang.language.CraterParser.BitShiftExpressionContext;
import org.craterlang.language.CraterParser.BitwiseAndExpressionContext;
import org.craterlang.language.CraterParser.BitwiseOrExpressionContext;
import org.craterlang.language.CraterParser.BitwiseXOrExpressionContext;
import org.craterlang.language.CraterParser.BlockContext;
import org.craterlang.language.CraterParser.BlockStatementContext;
import org.craterlang.language.CraterParser.BreakStatementContext;
import org.craterlang.language.CraterParser.CallExpressionContext;
import org.craterlang.language.CraterParser.ComparisonExpressionContext;
import org.craterlang.language.CraterParser.ConcatExpressionContext;
import org.craterlang.language.CraterParser.EmptyStatementContext;
import org.craterlang.language.CraterParser.ExpressionContext;
import org.craterlang.language.CraterParser.ForEqualsStatementContext;
import org.craterlang.language.CraterParser.ForInStatementContext;
import org.craterlang.language.CraterParser.FunctionCallStatementContext;
import org.craterlang.language.CraterParser.FunctionDeclarationStatementContext;
import org.craterlang.language.CraterParser.FunctionExpressionContext;
import org.craterlang.language.CraterParser.GotoStatementContext;
import org.craterlang.language.CraterParser.IfStatementContext;
import org.craterlang.language.CraterParser.IndexExpressionContext;
import org.craterlang.language.CraterParser.IndexedVarContext;
import org.craterlang.language.CraterParser.LabelStatementContext;
import org.craterlang.language.CraterParser.LiteralExpressionContext;
import org.craterlang.language.CraterParser.LocalDeclarationStatementContext;
import org.craterlang.language.CraterParser.LocalFunctionDeclarationStatementContext;
import org.craterlang.language.CraterParser.MemberExpressionContext;
import org.craterlang.language.CraterParser.MemberVarContext;
import org.craterlang.language.CraterParser.MulDivRemExpressionContext;
import org.craterlang.language.CraterParser.NameExpressionContext;
import org.craterlang.language.CraterParser.NamedVarContext;
import org.craterlang.language.CraterParser.OrExpressionContext;
import org.craterlang.language.CraterParser.ParenthesizedExpressionContext;
import org.craterlang.language.CraterParser.PowerExpressionContext;
import org.craterlang.language.CraterParser.PrefixExpressionContext;
import org.craterlang.language.CraterParser.PrefixExpressionExpressionContext;
import org.craterlang.language.CraterParser.PrefixOpExpressionContext;
import org.craterlang.language.CraterParser.RepeatStatementContext;
import org.craterlang.language.CraterParser.ReturnStatementContext;
import org.craterlang.language.CraterParser.StatementContext;
import org.craterlang.language.CraterParser.TableExpressionContext;
import org.craterlang.language.CraterParser.VarContext;
import org.craterlang.language.CraterParser.WhileStatementContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CraterChunkCompiler {
    private final Source source;

    public CraterChunkCompiler(Source source) {
        this.source = source;
    }

    private SourceSection getSourceSection(Token token) {
        return source.createSection(token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1);
    }

    private SourceSection getSourceSection(ParserRuleContext context) {
        return source.createSection(
            context.getStart().getStartIndex(),
            context.getStop().getStopIndex() - context.getStart().getStartIndex() + 1
        );
    }

    private CraterParseException createParseException(Token token, String message, Throwable cause) {
        return CraterParseException.create(getSourceSection(token), message, cause);
    }

    private CraterParseException createParseException(Token token, String message) {
        return createParseException(token, message, null);
    }

    private CraterParseException createParseException(Token token, Throwable cause) {
        return createParseException(token, null, cause);
    }

    private CraterParseException createParseException(ParserRuleContext context, String message, Throwable cause) {
        return CraterParseException.create(getSourceSection(context), message, cause);
    }

    private CraterParseException createParseException(ParserRuleContext context, String message) {
        return createParseException(context, message, null);
    }

    private CraterParseException createParseException(ParserRuleContext context, Throwable cause) {
        return createParseException(context, null, cause);
    }

    private final class FunctionGraph {
        private final FunctionGraph parentFunction;
        private final Map<String, LocalVar> declaredVars = new HashMap<>();
        private final List<BasicBlock> basicBlocks = new ArrayList<>();
        private final Deque<BasicBlock> loopExits = new ArrayDeque<>();

        private BasicBlock currentBasicBlock;

        private FunctionGraph(FunctionGraph parentFunction) {
            this.parentFunction = parentFunction;
        }

        private BasicBlock getCurrentBlock() {
            if (currentBasicBlock == null) {
                currentBasicBlock = new BasicBlock();
            }

            return currentBasicBlock;
        }

        private void finishBlock() {
            if (currentBasicBlock != null && !currentBasicBlock.instructions.isEmpty()) {
                basicBlocks.add(currentBasicBlock);
            }

            currentBasicBlock = null;
        }

        private Instruction process(ExpressionContext context) {
            if (context instanceof LiteralExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof FunctionExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof PrefixExpressionExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof TableExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof PowerExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof PrefixOpExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof MulDivRemExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof AddSubExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof ConcatExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof BitShiftExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof BitwiseAndExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof BitwiseXOrExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof BitwiseOrExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof ComparisonExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof AndExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof OrExpressionContext ctx) {
                return process(ctx);
            }
            else {
                throw new ClassCastException();
            }
        }

        private Instruction process(PrefixExpressionContext context) {
            if (context instanceof NameExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof IndexExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof MemberExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof CallExpressionContext ctx) {
                return process(ctx);
            }
            else if (context instanceof ParenthesizedExpressionContext ctx) {
                return process(ctx);
            }
            else {
                throw new ClassCastException();
            }
        }

        private Consumer<Instruction> process(VarContext context) {
            if (context instanceof NamedVarContext ctx) {
                return process(ctx);
            }
            else if (context instanceof IndexedVarContext ctx) {
                return process(ctx);
            }
            else if (context instanceof MemberVarContext ctx) {
                return process(ctx);
            }
            else {
                throw new ClassCastException();
            }
        }

        private void process(BlockContext context) {
            for (var statement : context.statements) {
                process(statement);
            }

            if (context.ret != null) {
                process(context.ret);
            }
        }

        private void process(ReturnStatementContext context) {
            var instruction = new ReturnInstruction();
            instruction.setSource(context);
            instruction.values = context.values.stream().map(this::process).toList();
            currentBasicBlock.append(instruction);
        }

        private void process(StatementContext context) {
            if (context instanceof EmptyStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof AssignmentStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof FunctionCallStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof LabelStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof BreakStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof GotoStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof BlockStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof WhileStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof RepeatStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof IfStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof ForEqualsStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof ForInStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof FunctionDeclarationStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof LocalFunctionDeclarationStatementContext ctx) {
                process(ctx);
            }
            else if (context instanceof LocalDeclarationStatementContext ctx) {
                process(ctx);
            }
            else {
                throw new ClassCastException();
            }
        }

        private void process(EmptyStatementContext context) {
            // Nothing to do
        }

        private void process(AssignmentStatementContext context) {
            // TODO
        }

        private void process(FunctionCallStatementContext context) {
            // TODO
        }

        private void process(LabelStatementContext context) {
            // TODO
        }

        private void process(BreakStatementContext context) {
            if (loopExits.isEmpty()) {
                throw createParseException(context, "\"break\" statement outside of a loop");
            }

            var instruction = new BranchInstruction();
            instruction.setSource(context);
            instruction.target = loopExits.getLast();
            currentBasicBlock.append(instruction);
        }

        private void process(GotoStatementContext context) {
            // TODO
        }

        private void process(BlockStatementContext context) {
            process(context.body);
        }

        private void process(WhileStatementContext context) {
            // TODO
        }

        private void process(RepeatStatementContext context) {
            // TODO
        }

        private void process(IfStatementContext context) {
            // TODO
        }

        private void process(ForEqualsStatementContext context) {
            // TODO
        }

        private void process(ForInStatementContext context) {
            // TODO
        }

        private void process(FunctionDeclarationStatementContext context) {
            // TODO
        }

        private void process(LocalFunctionDeclarationStatementContext context) {
            // TODO
        }

        private void process(LocalDeclarationStatementContext context) {
            // TODO
        }
    }

    private static final class LocalVar {}

    private static final class BasicBlock {
        private final List<Instruction> instructions = new ArrayList<>();

        private void append(Instruction instruction) {
            instructions.add(instruction);
        }
    }

    private static abstract sealed class Instruction {
        private int sourceStart = -1;
        private int sourceLength = 0;

        protected final void setSource(Token token) {
            sourceStart = token.getStartIndex();
            sourceLength = token.getStopIndex() - token.getStartIndex() + 1;
        }

        protected final void setSource(ParserRuleContext context) {
            sourceStart = context.getStart().getStartIndex();
            sourceLength = context.getStop().getStopIndex() - context.getStart().getStartIndex() + 1;
        }
    }

    private static final class GetArgumentInstruction extends Instruction {
        private int argumentIndex;
    }

    private static final class NewClosureInstruction extends Instruction {
        private FunctionGraph function;
    }

    private static final class ReturnInstruction extends Instruction {
        private List<Instruction> values;
    }

    private static final class LoadInstruction extends Instruction {
        private LocalVar localVar;
    }

    private static final class StoreInstruction extends Instruction {
        private LocalVar localVar;
        private Instruction value;
    }

    private static final class ConstantInstruction extends Instruction {
        private Object value;
    }

    private static final class MergeInstruction extends Instruction {
        private Instruction first, second;
    }

    private static final class BranchInstruction extends Instruction {
        private BasicBlock target;
    }

    private static abstract sealed class ConditionalBranchInstruction extends Instruction {
        private Instruction condition;
        private BasicBlock target;
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
