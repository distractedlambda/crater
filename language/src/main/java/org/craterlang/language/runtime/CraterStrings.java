package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterPathProfile;

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
            @Cached CraterPathProfile pathProfile
        ) {
            if (string.isEmpty()) {
                pathProfile.enter(0);
                return string;
            }

            pathProfile.enter(1);

            materializeNode.execute(string, ENCODING);
            var length = string.byteLength(ENCODING);

            var lhs = 0;
            while (lhs < length) {
                pathProfile.enter(2);
                if (!isWhitespace((byte) readByteNode.execute(string, lhs++, ENCODING))) {
                    break;
                }
            }

            pathProfile.enter(3);

            var rhs = length;
            while (rhs > lhs) {
                pathProfile.enter(4);
                if (!isWhitespace((byte) readByteNode.execute(string, --rhs, ENCODING))) {
                    break;
                }
            }

            pathProfile.enter(5);

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
            @Cached CraterPathProfile pathProfile,
            @Cached IntValueProfile lengthProfile
        ) {
            string = trimWhitespaceNode.execute(string, true);

            if (string.isEmpty()) {
                pathProfile.enter(0);
                throw NumberFormatException.getInstance();
            }

            pathProfile.enter(1);

            try {
                var result = parseDecNode.execute(string, 10);
                pathProfile.enter(2);
                return result;
            }
            catch (TruffleString.NumberFormatException ignored) {
                pathProfile.enter(3);
            }

            var length = lengthProfile.profile(string.byteLength(ENCODING));
            var negative = false;
            var i = 0;

            switch (readByteNode.execute(string, i, ENCODING)) {
                case '-':
                    pathProfile.enter(4);
                    negative = true;
                case '+':
                    pathProfile.enter(5);
                    if (++i >= length) {
                        pathProfile.enter(6);
                        throw NumberFormatException.getInstance();
                    }
            }

            pathProfile.enter(7);

            if (readByteNode.execute(string, i++, ENCODING) != '0' || i >= length) {
                pathProfile.enter(8);
                throw NumberFormatException.getInstance();
            }

            switch (readByteNode.execute(string, i++, ENCODING)) {
                case 'x', 'X' -> {
                    pathProfile.enter(9);

                    var total = 0L;

                    if (i >= length) {
                        pathProfile.enter(10);
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
                                pathProfile.enter(11);
                                throw NumberFormatException.getInstance();
                            }
                        };

                        pathProfile.enter(12);

                        total *= 16;
                        total += negative ? -digitValue : digitValue;
                    }

                    pathProfile.enter(13);

                    return total;
                }

                default -> {
                    pathProfile.enter(14);
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
