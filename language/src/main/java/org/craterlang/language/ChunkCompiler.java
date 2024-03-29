package org.craterlang.language;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
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
import org.craterlang.language.runtime.CraterNil;
import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.EconomicSet;
import org.graalvm.collections.Pair;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Character.isHighSurrogate;
import static java.lang.Character.isLowSurrogate;
import static java.lang.Character.toCodePoint;
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.util.Objects.requireNonNull;

public class ChunkCompiler {
    private final CraterLanguage language;
    private final Source source;

    public ChunkCompiler(CraterLanguage language, Source source) {
        this.language = language;
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
        final class BlockScope {
            final BlockScope parentScope;

            Object declaredLocals, labels, unresolvedGotos;
            // final EconomicMap<String, LocalVar> declaredLocals = EconomicMap.create();
            // final EconomicMap<String, BasicBlock> labels = EconomicMap.create();
            // final EconomicMap<String, List<GotoInstruction>> unresolvedGotos = EconomicMap.create();

            BlockScope(BlockScope parentScope) {
                this.parentScope = parentScope;
            }

            void checkNoUnresolvedGoto() {
                Object instructions;

                if (unresolvedGotos == null) {
                    return;
                }
                else if (unresolvedGotos instanceof Pair<?,?> pair) {
                    instructions = pair.getRight();
                }
                else {
                    instructions = ((EconomicMap<?, ?>) unresolvedGotos).getValues().iterator().next();
                }

                GotoInstruction instruction;

                if (instructions == null) {
                    return;
                }
                else if (instructions instanceof List<?> instructionList) {
                    instruction = (GotoInstruction) instructionList.get(0);
                }
                else {
                    instruction = (GotoInstruction) instructions;
                }

                throw createParseException(instruction, "No matching label found");
            }

            @SuppressWarnings("unchecked")
            boolean hasLabel(String name) {
                if (labels == null) {
                    return false;
                }
                else if (labels instanceof Pair<?,?> pair) {
                    return name.equals(pair.getLeft());
                }
                else {
                    return ((EconomicMap<String, BasicBlock>) labels).containsKey(name);
                }
            }

            @SuppressWarnings("unchecked")
            void declareLabel(String name, BasicBlock target) {
                if (labels == null) {
                    labels = Pair.create(name, target);
                }
                else if (labels instanceof Pair<?,?> pair) {
                    EconomicMap<String, BasicBlock> labelsMap = EconomicMap.create();
                    labelsMap.put((String) pair.getLeft(), (BasicBlock) pair.getRight());
                    labelsMap.put(name, target);
                    labels = labelsMap;
                }
                else {
                    ((EconomicMap<String, BasicBlock>) labels).put(name, target);
                }

                Object gotoInstructions;

                if (unresolvedGotos == null) {
                    return;
                }
                else if (unresolvedGotos instanceof Pair<?,?> pair) {
                    if (name.equals(pair.getLeft())) {
                        gotoInstructions = pair.getRight();
                    }
                    else {
                        return;
                    }
                }
                else {
                    gotoInstructions = ((EconomicMap<String, ?>) unresolvedGotos).get(name);
                }

                if (gotoInstructions == null) {
                    return;
                }
                else if (gotoInstructions instanceof List<?> gotoInstructionList) {
                    for (var instruction : gotoInstructionList) {
                        ((GotoInstruction) instruction).linkTarget(target);
                    }
                }
                else {
                    ((GotoInstruction) gotoInstructions).linkTarget(target);
                }
            }

            void resolveGoto(GotoInstruction instruction, String labelName) {
                // TODO
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
                currentBlock().linkSuccessor(block);
            }

            blocks.add(block);
            return block;
        }

        private void pushBlockScope() {
            currentBlockScope = new BlockScope(currentBlockScope);
        }

        private void popBlockScope() {
            currentBlockScope.checkNoUnresolvedGoto();
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

        private Operand process(LiteralExpressionContext context) {
            Object constantValue;

            switch (context.token.getType()) {
                case CraterParser.KwNil -> constantValue = CraterNil.getInstance();
                case CraterParser.KwFalse -> constantValue = false;
                case CraterParser.KwTrue -> constantValue = true;
                case CraterParser.DecInteger -> constantValue = parseDecInteger(context.token);
                case CraterParser.HexInteger -> constantValue = parseHexInteger(context.token);
                case CraterParser.DecFloat, CraterParser.HexFloat -> constantValue = parseFloatingPoint(context.token);
                case CraterParser.ShortString -> constantValue = parseShortString(context.token);
                case CraterParser.LongString -> constantValue = parseLongString(context.token);

                case CraterParser.Dot3 -> {
                    return currentBlock().append(new GetVarargsInstruction(context));
                }

                default -> throw new AssertionError();
            }

            return new Constant(constantValue);
        }

        private Object parseDecInteger(Token token) {
            var text = token.getText();
            try {
                return Long.parseLong(text, 10);
            }
            catch (NumberFormatException exception) {
                return Double.parseDouble(text);
            }
        }

        private long parseHexInteger(Token token) {
            var text = token.getText();
            var value = 0L;

            for (var i = 2; i < text.length(); i++) {
                value *= 16;
                value += hexDigitValue(text.charAt(i));
            }

            return value;
        }

        private static byte hexDigitValue(char digit) {
            return switch (digit) {
                case '0' -> 0;
                case '1' -> 1;
                case '2' -> 2;
                case '3' -> 3;
                case '4' -> 4;
                case '5' -> 5;
                case '6' -> 6;
                case '7' -> 7;
                case '8' -> 8;
                case '9' -> 9;
                case 'a', 'A' -> 10;
                case 'b', 'B' -> 11;
                case 'c', 'C' -> 12;
                case 'd', 'D' -> 13;
                case 'e', 'E' -> 14;
                case 'f', 'F' -> 15;
                default -> throw new NumberFormatException();
            };
        }

        private static byte decDigitValue(char digit) {
            return (byte) (digit - '0');
        }

        private static boolean isDecDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private double parseFloatingPoint(Token token) {
            return Double.parseDouble(token.getText());
        }

        private TruffleString parseShortString(Token token) {
            var text = token.getText();
            var builder = TruffleStringBuilder.create(TruffleString.Encoding.BYTES);

            parseNext: for (var i = 1; i < text.length() - 1;) {
                if (text.charAt(i++) == '\\') {
                    switch (text.charAt(i++)) {
                        case 'a' -> builder.appendByteUncached((byte) '\7');
                        case 'b' -> builder.appendByteUncached((byte) '\b');
                        case 'f' -> builder.appendByteUncached((byte) '\f');
                        case 'n', '\n' -> builder.appendByteUncached((byte) '\n');
                        case 'r' -> builder.appendByteUncached((byte) '\r');
                        case 't' -> builder.appendByteUncached((byte) '\t');
                        case 'v' -> builder.appendByteUncached((byte) '\u000b');
                        case '\\' -> builder.appendByteUncached((byte) '\\');
                        case '"' -> builder.appendByteUncached((byte) '"');
                        case '\'' -> builder.appendByteUncached((byte) '\'');

                        case '\r' -> {
                            if (text.charAt(i) == '\n') {
                                i++;
                            }

                            builder.appendByteUncached((byte) '\n');
                        }

                        case 'z' -> {
                            for (; ; ) {
                                switch (text.charAt(i)) {
                                    case ' ', '\f', '\n', '\r', '\t', '\u000b' -> i++;
                                    default -> {
                                        continue parseNext;
                                    }
                                }
                            }
                        }

                        case 'x' -> {
                            var high = hexDigitValue(text.charAt(i++));
                            var low = hexDigitValue(text.charAt(i++));
                            builder.appendByteUncached((byte) (high * 16 + low));
                        }

                        case 'd' -> {
                            int value;

                            var first = decDigitValue(text.charAt(i++));
                            if (isDecDigit(text.charAt(i))) {
                                var second = decDigitValue(text.charAt(i++));
                                if (isDecDigit(text.charAt(i))) {
                                    var third = decDigitValue(text.charAt(i++));
                                    value = first * 100 + second * 10 + third;
                                }
                                else {
                                    value = first * 10 + second;
                                }
                            }
                            else {
                                value = first;
                            }

                            if (value > 255) {
                                throw createParseException(token, "Invalid decimal escape sequence");
                            }

                            builder.appendByteUncached((byte) value);
                        }

                        case 'u' -> {
                            i++; // skip '{';

                            var value = 0;

                            try {
                                while (text.charAt(i) != '}') {
                                    value = multiplyExact(value, 16);
                                    value = addExact(value, hexDigitValue(text.charAt(i++)));
                                }
                            }
                            catch (ArithmeticException exception) {
                                throw createParseException(token, "Invalid UTF8 escape sequence");
                            }

                            i++; // skip '}'

                            appendRawCodePoint(builder, value);
                        }

                        default -> throw new AssertionError();
                    }
                }
                else {
                    int codePoint;

                    if (isHighSurrogate(text.charAt(i - 1)) && isLowSurrogate(text.charAt(i))) {
                        codePoint = toCodePoint(text.charAt(i - 1), text.charAt(i++));
                    }
                    else {
                        codePoint = text.charAt(i - 1);
                    }

                    appendRawCodePoint(builder, codePoint);
                }
            }

            // TODO
            return null;
            // return language.getInternedString(builder.toStringUncached());
        }

        private static void appendRawCodePoint(TruffleStringBuilder builder, int codePoint) {
            if (codePoint >= 0x80) {
                if (codePoint >= 0x800) {
                    if (codePoint >= 0x1_0000) {
                        if (codePoint >= 0x20_0000) {
                            if (codePoint >= 0x400_0000) {
                                builder.appendByteUncached((byte) (0xFC | (codePoint >>> 30)));
                                builder.appendByteUncached((byte) (0x80 | ((codePoint >> 24) & 0x3F)));
                            }
                            else {
                                builder.appendByteUncached((byte) (0xF8 | (codePoint >>> 24)));
                            }
                            builder.appendByteUncached((byte) (0x80 | ((codePoint >> 18) & 0x3F)));
                        }
                        else {
                            builder.appendByteUncached((byte) (0xF0 | (codePoint >>> 18)));
                        }
                        builder.appendByteUncached((byte) (0x80 | ((codePoint >> 12) & 0x3F)));
                    }
                    else {
                        builder.appendByteUncached((byte) (0xE0 | (codePoint >>> 12)));
                    }
                    builder.appendByteUncached((byte) (0x80 | ((codePoint >> 6) & 0x3F)));
                }
                else {
                    builder.appendByteUncached((byte) (0xC0 | (codePoint >>> 6)));
                }
                builder.appendByteUncached((byte) (0x80 | (codePoint & 0x3F)));
            }
            else {
                builder.appendByteUncached((byte) codePoint);
            }
        }

        private TruffleString parseLongString(Token token) {
            // TODO
            return null;
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

        private List<Operand> process(CraterParser.ArgsContext context) {
            // TODO
            return null;
        }

        private void process(ReturnStatementContext context) {
            if (context.values.size() == 1) {
                if (context.values.get(0) instanceof PrefixExpressionExpressionContext expressionContext) {
                    if (expressionContext.child instanceof CallExpressionContext callContext) {
                        append(new TailCallInstruction(context, process(callContext), process(callContext.arguments)));
                        return;
                    }
                }
            }

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
                if (scope.hasLabel(nameString)) {
                    throw createParseException(context, "An identically-named label is already in scope");
                }
            }

            var labeledBlock = addBasicBlock(new BasicBlock());
            currentBlockScope.declareLabel(nameString, labeledBlock);
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
            currentBlockScope.resolveGoto(instruction, targetName);
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
        Object loads, stores, captures;

        @SuppressWarnings("unchecked")
        void addLoad(LoadInstruction instruction) {
            if (loads == null) {
                loads = instruction;
            }
            else if (loads instanceof EconomicSet<?> loadSet) {
                ((EconomicSet<LoadInstruction>) loadSet).add(instruction);
            }
            else if (instruction != loads) {
                EconomicSet<LoadInstruction> loadSet = EconomicSet.create();
                loadSet.add((LoadInstruction) loads);
                loadSet.add(instruction);
                loads = loadSet;
            }
        }

        @SuppressWarnings("unchecked")
        void addStore(StoreInstruction instruction) {
            if (stores == null) {
                stores = instruction;
            }
            else if (stores instanceof EconomicSet<?> storeSet) {
                ((EconomicSet<StoreInstruction>) storeSet).add(instruction);
            }
            else if (instruction != stores) {
                EconomicSet<StoreInstruction> storeSet = EconomicSet.create();
                storeSet.add((StoreInstruction) stores);
                storeSet.add(instruction);
                stores = storeSet;
            }
        }

        @SuppressWarnings("unchecked")
        void addCapture(CapturedVar capture) {
            if (captures == null) {
                captures = capture;
            }
            else if (captures instanceof EconomicSet<?> captureSet) {
                ((EconomicSet<CapturedVar>) captureSet).add(capture);
            }
            else if (capture != captures) {
                EconomicSet<CapturedVar> captureSet = EconomicSet.create();
                captureSet.add((CapturedVar) captures);
                captureSet.add(capture);
                captures = captureSet;
            }
        }
    }

    private static final class LocalVar extends Var {
    }

    private static final class CapturedVar extends Var {
        final Var source;

        CapturedVar(Var source) {
            this.source = requireNonNull(source);
            source.addCapture(this);
        }
    }

    private static final class BasicBlock {
        Object instructions, predecessors, successors;

        boolean terminated;

        @SuppressWarnings("unchecked")
        <I extends Instruction> I append(I instruction) {
            instruction.visitBranchTargets(this::linkSuccessor);

            if (instruction.isTerminator()) {
                terminated = true;
            }

            if (instructions == null) {
                instructions = instruction;
            }
            else if (instructions instanceof List<?> instructionList) {
                ((List<Instruction>) instructionList).add(instruction);
            }
            else {
                List<Instruction> instructionList = new ArrayList<>();
                instructionList.add((Instruction) instructions);
                instructionList.add(instruction);
                instructions = instructionList;
            }

            instruction.block = this;

            return instruction;
        }

        @SuppressWarnings("unchecked")
        void linkSuccessor(BasicBlock successor) {
            if (successors == null) {
                successors = successor;
            }
            else if (successors instanceof EconomicSet<?> successorSet) {
                ((EconomicSet<BasicBlock>) successorSet).add(successor);
            }
            else if (successor != successors) {
                EconomicSet<BasicBlock> successorSet = EconomicSet.create();
                successorSet.add((BasicBlock) successors);
                successorSet.add(successor);
                successors = successorSet;
            }

            if (successor.predecessors == null) {
                successor.predecessors = this;
            }
            else if (successor.predecessors instanceof EconomicSet<?> predecessorSet) {
                ((EconomicSet<BasicBlock>) predecessorSet).add(this);
            }
            else if (this != successor.predecessors) {
                EconomicSet<BasicBlock> predecessorSet = EconomicSet.create();
                predecessorSet.add((BasicBlock) successor.predecessors);
                predecessorSet.add(this);
                successor.predecessors = predecessorSet;
            }
        }
    }

    private static abstract sealed class Instruction extends Operand {
        BasicBlock block;
        Object uses;

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

        @SuppressWarnings("unchecked")
        @Override final void addUse(Instruction user) {
            if (uses == null) {
                uses = user;
            }
            else if (uses instanceof EconomicSet<?> useSet) {
                ((EconomicSet<Instruction>) useSet).add(user);
            }
            else if (user != uses) {
                EconomicSet<Instruction> useSet = EconomicSet.create();
                useSet.add((Instruction) uses);
                useSet.add(user);
                uses = useSet;
            }
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

    private static final class GetVarargsInstruction extends Instruction {
        GetVarargsInstruction(ParserRuleContext context) {
            super(context);
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
            var.addLoad(this);
        }
    }

    private static final class StoreInstruction extends Instruction {
        final Var var;
        final Operand value;

        StoreInstruction(ParserRuleContext context, Var var, Operand value) {
            super(context);

            this.var = requireNonNull(var);
            this.value = requireNonNull(value);

            var.addStore(this);
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
            block.linkSuccessor(resolvedTarget);
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
        NonTailCallInstruction(ParserRuleContext context, Operand callee, List<Operand> arguments) {
            super(context, callee, arguments);
        }
    }

    private static final class TailCallInstruction extends CallInstruction {
        TailCallInstruction(ParserRuleContext context, Operand callee, List<Operand> arguments) {
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

    private static final class NewTableInstruction extends Instruction {
        NewTableInstruction(ParserRuleContext context) {
            super(context);
        }
    }
}
