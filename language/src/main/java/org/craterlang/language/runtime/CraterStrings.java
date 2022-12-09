package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterPathProfileNode;

public final class CraterStrings {
    private CraterStrings() {}

    private static final TruffleString.Encoding ENCODING = TruffleString.Encoding.BYTES;

    private static boolean isWhitespace(byte b) {
        return switch (b) {
            case ' ', '\f', '\n', '\r', '\t', 0x0b -> true;
            default -> false;
        };
    }

    public static final class NumberFormatException extends ControlFlowException {
        private NumberFormatException() {}

        public static NumberFormatException getInstance() {
            return INSTANCE;
        }

        private static final NumberFormatException INSTANCE = new NumberFormatException();
    }

    @GenerateUncached
    public static abstract class TrimWhitespaceNode extends CraterNode {
        public abstract TruffleString execute(TruffleString string, boolean lazy);

        @Specialization
        TruffleString doExecute(
            TruffleString string,
            boolean lazy,
            @Cached TruffleString.MaterializeNode materializeNode,
            @Cached TruffleString.ReadByteNode readByteNode,
            @Cached TruffleString.SubstringByteIndexNode substringNode,
            @Cached CraterPathProfileNode pathProfileNode
        ) {
            if (string.isEmpty()) {
                pathProfileNode.execute(0);
                return string;
            }

            pathProfileNode.execute(1);

            materializeNode.execute(string, ENCODING);
            var length = string.byteLength(ENCODING);

            var lhs = 0;
            while (lhs < length) {
                pathProfileNode.execute(2);
                if (!isWhitespace((byte) readByteNode.execute(string, lhs++, ENCODING))) {
                    break;
                }
            }

            pathProfileNode.execute(3);

            var rhs = length;
            while (rhs > lhs) {
                pathProfileNode.execute(4);
                if (!isWhitespace((byte) readByteNode.execute(string, --rhs, ENCODING))) {
                    break;
                }
            }

            pathProfileNode.execute(5);

            return substringNode.execute(string, lhs, rhs - lhs, ENCODING, lazy);
        }
    }

    @GenerateUncached
    public static abstract class ParseLongNode extends CraterNode {
        public abstract long execute(TruffleString string);

        @Specialization
        long doExecute(
            TruffleString string,
            @Cached TrimWhitespaceNode trimWhitespaceNode,
            @Cached TruffleString.ParseLongNode parseDecNode,
            @Cached TruffleString.ReadByteNode readByteNode,
            @Cached CraterPathProfileNode pathProfileNode,
            @Cached IntValueProfile lengthProfile
        ) {
            string = trimWhitespaceNode.execute(string, true);

            if (string.isEmpty()) {
                pathProfileNode.execute(0);
                throw NumberFormatException.getInstance();
            }

            pathProfileNode.execute(1);

            try {
                var result = parseDecNode.execute(string, 10);
                pathProfileNode.execute(2);
                return result;
            }
            catch (TruffleString.NumberFormatException ignored) {
                pathProfileNode.execute(3);
            }

            var length = lengthProfile.profile(string.byteLength(ENCODING));
            var negative = false;
            var i = 0;

            switch (readByteNode.execute(string, i, ENCODING)) {
                case '-':
                    pathProfileNode.execute(4);
                    negative = true;
                case '+':
                    pathProfileNode.execute(5);
                    if (++i >= length) {
                        pathProfileNode.execute(6);
                        throw NumberFormatException.getInstance();
                    }
            }

            pathProfileNode.execute(7);

            if (readByteNode.execute(string, i++, ENCODING) != '0' || i >= length) {
                pathProfileNode.execute(8);
                throw NumberFormatException.getInstance();
            }

            switch (readByteNode.execute(string, i++, ENCODING)) {
                case 'x', 'X' -> {
                    pathProfileNode.execute(9);

                    var total = 0L;

                    if (i >= length) {
                        pathProfileNode.execute(10);
                        throw NumberFormatException.getInstance();
                    }

                    while (i < length) {
                        var digitValue = switch (readByteNode.execute(string, i++, ENCODING)) {
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
                            default -> {
                                pathProfileNode.execute(11);
                                throw NumberFormatException.getInstance();
                            }
                        };

                        pathProfileNode.execute(12);

                        total *= 16;
                        total += negative ? -digitValue : digitValue;
                    }

                    pathProfileNode.execute(13);

                    return total;
                }

                default -> {
                    pathProfileNode.execute(14);
                    throw NumberFormatException.getInstance();
                }
            }
        }
    }

    @GenerateUncached
    public static abstract class ParseNumberNode extends CraterNode {
        public abstract Object execute(TruffleString string);

        @Specialization
        Object doExecute(
            TruffleString string,
            @Cached ParseLongNode parseLongNode,
            @Cached TruffleString.ParseDoubleNode parseDoubleNode
        ) {
            try {
                return parseLongNode.execute(string);
            }
            catch (NumberFormatException ignored) {}

            try {
                return parseDoubleNode.execute(string);
            }
            catch (TruffleString.NumberFormatException exception) {
                throw NumberFormatException.getInstance();
            }
        }
    }
}