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
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

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

    private SourceSection getSourceSection(Instruction instruction) {
        if (instruction.sourceStart < 0) {
            return source.createUnavailableSection();
        }
        else {
            return source.createSection(instruction.sourceStart, instruction.sourceLength);
        }
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

    private CraterParseException createParseException(Instruction instruction, String message, Throwable cause) {
        return CraterParseException.create(getSourceSection(instruction), message, cause);
    }

    private CraterParseException createParseException(Instruction instruction, String message) {
        return createParseException(instruction, message, null);
    }

    private CraterParseException createParseException(Instruction instruction, Throwable cause) {
        return createParseException(instruction, null, cause);
    }

    private final class FunctionInstruction extends Instruction {
        static final class BlockScope {
            final BlockScope parentScope;
            final EconomicMap<String, LocalVar> declaredLocals = EconomicMap.create();
            final EconomicMap<String, BasicBlock> labels = EconomicMap.create();
            final EconomicMap<String, List<GotoInstruction>> unresolvedGotos = EconomicMap.create();

            BlockScope(BlockScope parentScope) {
                this.parentScope = parentScope;
            }
        }

        final FunctionInstruction parentFunction;
        final Deque<BasicBlock> loopExits = new ArrayDeque<>();

        BlockScope currentBlockScope;

        final List<BasicBlock> blocks = new ArrayList<>();

        private FunctionInstruction(ParserRuleContext context, FunctionInstruction parentFunction) {
            super(context);
            this.parentFunction = parentFunction;
            blocks.add(new BasicBlock());
        }

        private BasicBlock currentBlock() {
            return blocks.get(blocks.size() - 1);
        }

        private <I extends Instruction> I append(I instruction) {
            return currentBlock().append(instruction);
        }

        private BasicBlock addBasicBlock(BasicBlock block) {
            if (!currentBlock().terminated) {
                currentBlock().addSuccessor(block);
            }

            blocks.add(block);
            return block;
        }

        private void pushBlockScope() {
            currentBlockScope = new BlockScope(currentBlockScope);
        }

        private void popBlockScope() {
            for (var unresolved : currentBlockScope.unresolvedGotos.getValues()) {
                throw createParseException(unresolved.get(0), "No matching label found");
            }

            currentBlockScope = currentBlockScope.parentScope;
        }

        private Operand process(ExpressionContext context) {
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

        private Operand process(PrefixExpressionContext context) {
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

        private Consumer<Operand> process(VarContext context) {
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
            append(new ReturnInstruction(context, context.values.stream().map(this::process).toList()));
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
            var nameString = context.name.getText();
            for (var scope = currentBlockScope; scope != null; scope = scope.parentScope) {
                if (scope.labels.containsKey(nameString)) {
                    throw createParseException(context, "An identically-named label is already in scope");
                }
            }

            var labeledBlock = addBasicBlock(new BasicBlock());
            currentBlockScope.labels.put(nameString, labeledBlock);

            var unresolved = currentBlockScope.unresolvedGotos.removeKey(nameString);
            if (unresolved != null) {
                for (var instruction : unresolved) {
                    instruction.linkTarget(labeledBlock);
                }
            }
        }

        private void process(BreakStatementContext context) {
            if (loopExits.isEmpty()) {
                throw createParseException(context, "\"break\" statement outside of a loop");
            }

            append(new JumpInstruction(context, loopExits.getLast()));
        }

        private void process(GotoStatementContext context) {
            var instruction = append(new GotoInstruction(context));
            var targetName = context.target.getText();
            var targetBlock = currentBlockScope.labels.get(targetName);
            if (targetBlock == null) {
                var unresolved = currentBlockScope.unresolvedGotos.get(targetName);

                if (unresolved == null) {
                    unresolved = new ArrayList<>();
                    currentBlockScope.unresolvedGotos.put(targetName, unresolved);
                }

                unresolved.add(instruction);
            }
            else {
                instruction.linkTarget(targetBlock);
            }
        }

        private void process(BlockStatementContext context) {
            pushBlockScope();
            process(context.body);
            popBlockScope();
        }

        private void process(WhileStatementContext context) {
            var loopBlock = addBasicBlock(new BasicBlock());
            var exitBlock = new BasicBlock();

            append(new WhileConditionInstruction(context.condition, process(context.condition), exitBlock));

            pushBlockScope();
            loopExits.push(exitBlock);
            process(context.body);
            loopExits.pop();
            popBlockScope();

            append(new JumpInstruction(context, loopBlock));

            addBasicBlock(exitBlock);
        }

        private void process(RepeatStatementContext context) {
            var loopBlock = addBasicBlock(new BasicBlock());
            var exitBlock = new BasicBlock();

            pushBlockScope();
            loopExits.push(exitBlock);
            process(context.body);
            loopExits.pop();
            append(new RepeatConditionInstruction(context.condition, process(context.condition), loopBlock));
            popBlockScope();

            addBasicBlock(exitBlock);
        }

        private void process(IfStatementContext context) {
            var endBlock = new BasicBlock();

            for (var i = 0; i < context.conditions.size(); i++) {
                var alternateBlock = new BasicBlock();

                var conditionContext = context.conditions.get(i);
                append(new IfConditionInstruction(conditionContext, process(conditionContext), alternateBlock));

                pushBlockScope();
                process(context.consequents.get(i));
                popBlockScope();

                append(new JumpInstruction(context, endBlock));

                addBasicBlock(alternateBlock);
            }

            if (context.alternate != null) {
                pushBlockScope();
                process(context.alternate);
                popBlockScope();
            }

            addBasicBlock(endBlock);
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

    private static abstract sealed class Operand {
        abstract void addUse(Instruction user);
    }

    private static final class Constant extends Operand {
        final Object value;

        Constant(Object value) {
            this.value = requireNonNull(value);
        }

        @Override void addUse(Instruction user) {
            // Nothing to do
        }
    }

    private static abstract sealed class Var {
        final EconomicSet<LoadInstruction> loads = EconomicSet.create();
        final EconomicSet<StoreInstruction> stores = EconomicSet.create();
        final EconomicSet<CapturedVar> captures = EconomicSet.create();
    }

    private static final class LocalVar extends Var {
    }

    private static final class CapturedVar extends Var {
        final Var source;

        CapturedVar(Var source) {
            this.source = requireNonNull(source);
            source.captures.add(this);
        }
    }

    private static final class BasicBlock {
        final List<Instruction> instructions = new ArrayList<>();
        final EconomicSet<BasicBlock> predecessors = EconomicSet.create();
        final EconomicSet<BasicBlock> successors = EconomicSet.create();

        boolean terminated;

        <I extends Instruction> I append(I instruction) {
            instruction.visitBranchTargets(this::addSuccessor);

            if (instruction.isTerminator()) {
                terminated = true;
            }

            instructions.add(instruction);
            instruction.block = this;

            return instruction;
        }

        void addSuccessor(BasicBlock successor) {
            successors.add(successor);
            successor.predecessors.add(this);
        }
    }

    private static abstract sealed class Instruction extends Operand {
        BasicBlock block;

        final EconomicSet<Instruction> uses = EconomicSet.create();
        final int sourceStart;
        final int sourceLength;

        Instruction(ParserRuleContext context) {
            if (context != null) {
                sourceStart = context.getStart().getStartIndex();
                sourceLength = context.getStop().getStopIndex() - context.getStart().getStartIndex() + 1;
            }
            else {
                sourceStart = -1;
                sourceLength = 0;
            }
        }

        @Override final void addUse(Instruction user) {
            uses.add(user);
        }

        boolean isTerminator() {
            return false;
        }

        void visitBranchTargets(Consumer<BasicBlock> visitor) {
            // Default impl does nothing
        }
    }

    private static final class GetArgumentInstruction extends Instruction {
        final int argumentIndex;

        GetArgumentInstruction(ParserRuleContext context, int argumentIndex) {
            super(context);
            this.argumentIndex = argumentIndex;
        }
    }

    private static final class ReturnInstruction extends Instruction {
        final List<Operand> values;

        ReturnInstruction(ParserRuleContext context, List<Operand> values) {
            super(context);
            this.values = requireNonNull(values);
            for (var value : values) {
                value.addUse(this);
            }
        }

        @Override boolean isTerminator() {
            return true;
        }
    }

    private static final class LoadInstruction extends Instruction {
        final Var var;

        LoadInstruction(ParserRuleContext context, Var var) {
            super(context);
            this.var = requireNonNull(var);
            var.loads.add(this);
        }
    }

    private static final class StoreInstruction extends Instruction {
        final Var var;
        final Operand value;

        StoreInstruction(ParserRuleContext context, Var var, Operand value) {
            super(context);

            this.var = requireNonNull(var);
            this.value = requireNonNull(value);

            var.stores.add(this);
            value.addUse(this);
        }
    }

    private static final class MergeInstruction extends Instruction {
        final Operand first, second;

        MergeInstruction(ParserRuleContext context, Operand first, Operand second) {
            super(context);

            this.first = requireNonNull(first);
            this.second = requireNonNull(second);

            first.addUse(this);
            second.addUse(this);
        }
    }

    private static final class GotoInstruction extends Instruction {
        // May not be immediately known, so this has to be special-cased
        BasicBlock target;

        GotoInstruction(ParserRuleContext context) {
            super(context);
        }

        @Override boolean isTerminator() {
            return true;
        }

        void linkTarget(BasicBlock resolvedTarget) {
            target = resolvedTarget;
            block.addSuccessor(resolvedTarget);
        }
    }

    private static final class JumpInstruction extends Instruction {
        final BasicBlock target;

        JumpInstruction(ParserRuleContext context, BasicBlock target) {
            super(context);
            this.target = requireNonNull(target);
        }

        @Override void visitBranchTargets(Consumer<BasicBlock> visitor) {
            visitor.accept(target);
        }

        @Override boolean isTerminator() {
            return true;
        }
    }

    private static final class WhileConditionInstruction extends Instruction {
        final Operand condition;
        final BasicBlock exitBlock;

        WhileConditionInstruction(ParserRuleContext context, Operand condition, BasicBlock exitBlock) {
            super(context);

            this.condition = requireNonNull(condition);
            this.exitBlock = requireNonNull(exitBlock);

            condition.addUse(this);
        }

        @Override void visitBranchTargets(Consumer<BasicBlock> visitor) {
            visitor.accept(exitBlock);
        }
    }

    private static final class RepeatConditionInstruction extends Instruction {
        final Operand condition;
        final BasicBlock loopBlock;

        RepeatConditionInstruction(ParserRuleContext context, Operand condition, BasicBlock loopBlock) {
            super(context);

            this.condition = requireNonNull(condition);
            this.loopBlock = requireNonNull(loopBlock);

            condition.addUse(this);
        }

        @Override void visitBranchTargets(Consumer<BasicBlock> visitor) {
            visitor.accept(loopBlock);
        }
    }

    private static final class IfConditionInstruction extends Instruction {
        final Operand condition;
        final BasicBlock alternateBlock;

        IfConditionInstruction(ParserRuleContext context, Operand condition, BasicBlock alternateBlock) {
            super(context);

            this.condition = requireNonNull(condition);
            this.alternateBlock = requireNonNull(alternateBlock);

            condition.addUse(this);
        }

        @Override void visitBranchTargets(Consumer<BasicBlock> visitor) {
            visitor.accept(alternateBlock);
        }
    }

    private static final class NewindexInstruction extends Instruction {
        final Operand receiver;
        final Operand key;
        final Operand value;

        NewindexInstruction(ParserRuleContext context, Operand receiver, Operand key, Operand value) {
            super(context);

            this.receiver = requireNonNull(receiver);
            this.key = requireNonNull(key);
            this.value = requireNonNull(value);

            receiver.addUse(this);
            key.addUse(this);
            value.addUse(this);
        }
    }

    private static abstract sealed class CallInstruction extends Instruction {
        final Operand callee;
        final List<Operand> arguments;

        CallInstruction(ParserRuleContext context, Operand callee, List<Operand> arguments) {
            super(context);

            this.callee = requireNonNull(callee);
            this.arguments = requireNonNull(arguments);

            callee.addUse(this);
            for (var argument : arguments) {
                argument.addUse(this);
            }
        }
    }

    private static final class NonTailCallInstruction extends CallInstruction {
        NonTailCallInstruction(ParserRuleContext context, Instruction callee, List<Operand> arguments) {
            super(context, callee, arguments);
        }
    }

    private static final class TailCallInstruction extends CallInstruction {
        TailCallInstruction(ParserRuleContext context, Instruction callee, List<Operand> arguments) {
            super(context, callee, arguments);
        }

        @Override boolean isTerminator() {
            return true;
        }
    }

    private static final class UnopInstruction extends Instruction {
        final Op op;
        final Operand operand;

        UnopInstruction(ParserRuleContext context, Op op, Operand operand) {
            super(context);

            this.op = requireNonNull(op);
            this.operand = requireNonNull(operand);

            operand.addUse(this);
        }

        private enum Op {
            BNOT,
            LEN,
            NOT,
            UNM,
        }
    }

    private static final class BinopInstruction extends Instruction {
        final Op op;
        final Operand lhs;
        final Operand rhs;

        BinopInstruction(ParserRuleContext context, Op op, Instruction lhs, Instruction rhs) {
            super(context);

            this.op = requireNonNull(op);
            this.lhs = requireNonNull(lhs);
            this.rhs = requireNonNull(rhs);

            lhs.addUse(this);
            rhs.addUse(this);
        }

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
