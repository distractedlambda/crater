package org.craterlang.language.runtime;

import com.oracle.truffle.api.ArrayUtils;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.memory.ByteArraySupport;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterPathProfile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EmptyStackException;

import static com.oracle.truffle.api.CompilerDirectives.castExact;
import static com.oracle.truffle.api.CompilerDirectives.isExact;
import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;
import static java.lang.Double.isNaN;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Math.addExact;
import static java.lang.Math.decrementExact;
import static java.lang.Math.incrementExact;
import static java.lang.Math.multiplyExact;
import static java.lang.System.arraycopy;
import static org.craterlang.language.runtime.CraterMath.hasExactLongValue;

public final class CraterString implements TruffleObject {
    private Object objectData;
    private long primitiveData;

    private static final Object TAG_LAZY_LONG = new Object();
    private static final Object TAG_LAZY_DOUBLE = new Object();

    private static final byte TAG_IMMEDIATE = 0;
    private static final byte TAG_LAZY_CONCAT = 1;
    private static final byte TAG_LAZY_REPEAT = 2;
    private static final byte TAG_LAZY_REVERSE = 3;

    public static final class NumberFormatException extends ControlFlowException {
        private NumberFormatException() {}

        private static final NumberFormatException INSTANCE = new NumberFormatException();
    }

    @TruffleBoundary
    public void forceUncached() {
        ForceNode.getUncached().execute(this);
    }

    @TruffleBoundary(allowInlining = true)
    @Override public boolean equals(Object obj) {
        if (!(obj instanceof CraterString other)) {
            return false;
        }

        return EqualsNode.getUncached().execute(this, other);
    }

    @Override public int hashCode() {
        return HashCodeNode.getUncached().execute(this);
    }

    @TruffleBoundary
    @Override public String toString() {
        forceUncached();
        return new String(getImmediateBytes(), StandardCharsets.UTF_8);
    }

    boolean isLazyLong() {
        return objectData == TAG_LAZY_LONG;
    }

    long getLazyLongValue() {
        assert isLazyLong();
        return primitiveData;
    }

    boolean isLazyDouble() {
        return objectData == TAG_LAZY_DOUBLE;
    }

    double getLazyDoubleValue() {
        assert isLazyDouble();
        return longBitsToDouble(primitiveData);
    }

    boolean isImmediate() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_IMMEDIATE;
    }

    byte[] getImmediateBytes() {
        assert isImmediate();
        return (byte[]) objectData;
    }

    int getImmediateLength() {
        assert isImmediate();
        return getImmediateBytes().length;
    }

    boolean hasImmediateHashCode() {
        assert isImmediate();
        return primitiveData >= 0;
    }

    int getImmediateHashCode() {
        assert hasImmediateHashCode();
        return (int) (primitiveData >>> 8);
    }

    void setImmediateHashCode(int value) {
        assert isImmediate();
        primitiveData = (Integer.toUnsignedLong(value) << 8) | TAG_IMMEDIATE;
    }

    void setImmediate(byte[] bytes) {
        objectData = bytes;
        primitiveData = Long.MAX_VALUE | TAG_IMMEDIATE;
    }

    boolean isLazyConcat() {
        return isExact(objectData, CraterString[].class) && (byte) primitiveData == TAG_LAZY_CONCAT;
    }

    CraterString[] getLazyConcatSubstrings() {
        assert isLazyConcat();
        return castExact(objectData, CraterString[].class);
    }

    int getLazyConcatLength() {
        assert isLazyConcat();
        return (int) (primitiveData >>> 8);
    }

    boolean isLazyRepeat() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_LAZY_REPEAT;
    }

    int getLazyRepeatCount() {
        assert isLazyRepeat();
        return (int) (primitiveData >>> 40);
    }

    int getLazyRepeatLength() {
        assert isLazyRepeat();
        return (int) (primitiveData >>> 8);
    }

    CraterString getLazyRepeatChild() {
        assert isLazyRepeat();
        return (CraterString) objectData;
    }

    boolean isLazyReverse() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_LAZY_REVERSE;
    }

    int getLazyReverseLength() {
        assert isLazyReverse();
        return (int) (primitiveData >>> 8);
    }

    CraterString getLazyReverseChild() {
        assert isLazyReverse();
        return (CraterString) objectData;
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class LengthNode extends CraterNode {
        public abstract int execute(CraterString string);

        public static LengthNode create() {
            return CraterStringFactory.LengthNodeGen.create();
        }

        public static LengthNode getUncached() {
            return CraterStringFactory.LengthNodeGen.getUncached();
        }

        @Specialization(guards = "string.isImmediate()")
        int doImmediate(CraterString string) {
            return string.getImmediateLength();
        }

        @Specialization(guards = "string.isLazyConcat()")
        int doLazyConcat(CraterString string) {
            return string.getLazyConcatLength();
        }

        @Specialization(guards = "string.isLazyRepeat()")
        int doLazyRepeat(CraterString string) {
            return string.getLazyRepeatLength();
        }

        @Specialization(guards = "string.isLazyReverse()")
        int doLazyReverse(CraterString string) {
            return string.getLazyReverseLength();
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class HashCodeNode extends CraterNode {
        public abstract int execute(CraterString string);

        public static HashCodeNode create() {
            return CraterStringFactory.HashCodeNodeGen.create();
        }

        public static HashCodeNode getUncached() {
            return CraterStringFactory.HashCodeNodeGen.getUncached();
        }

        @Specialization
        int doExecute(CraterString string, @Cached ForceNode forceNode, @Cached BranchProfile missingHashCodeProfile) {
            forceNode.execute(string);

            if (!string.hasImmediateHashCode()) {
                missingHashCodeProfile.enter();
                string.setImmediateHashCode(hashCodeBoundary(string.getImmediateBytes()));
            }

            return string.getImmediateHashCode();
        }

        @TruffleBoundary(allowInlining = true)
        private static int hashCodeBoundary(byte[] bytes) {
            return Arrays.hashCode(bytes);
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class ForceNode extends CraterNode {
        public abstract void execute(CraterString string);

        public static ForceNode create() {
            return CraterStringFactory.ForceNodeGen.create();
        }

        public static ForceNode getUncached() {
            return CraterStringFactory.ForceNodeGen.getUncached();
        }

        @Specialization(guards = "string.isImmediate()")
        void doImmediate(CraterString string) {}

        @Fallback
        void doOther(
            CraterString string,
            @Cached LengthNode lengthNode,
            @Cached IntValueProfile lengthProfile,
            @Cached AppendSubstringNode appendSubstringNode
        ) {
            var length = lengthProfile.profile(lengthNode.execute(string));
            var bytes = new byte[length];
            var writtenLength = appendSubstringNode.execute(string, bytes, 0);
            assert writtenLength == length;
            string.setImmediate(bytes);
        }
    }

    @GenerateUncached
    static abstract class AppendSubstringNode extends CraterNode {
        public abstract int execute(CraterString substring, byte[] destination, int offset);

        static AppendSubstringNode create() {
            return CraterStringFactory.AppendSubstringNodeGen.create();
        }

        @Specialization(guards = "string.isImmediate()")
        int doImmediate(CraterString string, byte[] destination, int offset) {
            var bytes = string.getImmediateBytes();
            arraycopy(bytes, 0, destination, offset, bytes.length);
            return offset + bytes.length;
        }

        @ExplodeLoop
        @Specialization(
            guards = {
                "string.isLazyConcat()",
                "substrings.length == appendSubstringNodes.length",
            },
            limit = "1"
        )
        int doExplodedLazyConcat(
            CraterString string,
            byte[] destination,
            int offset,
            @Bind("string.getLazyConcatSubstrings()") CraterString[] substrings,
            @Cached("createAppendSubstringNodes(substrings.length)") AppendSubstringNode[] appendSubstringNodes
        ) {
            for (var i = 0; i < appendSubstringNodes.length; i++) {
                offset = appendSubstringNodes[i].execute(substrings[i], destination, offset);
            }

            return offset;
        }

        @Specialization(guards = "string.isLazyConcat()", replaces = "doExplodedLazyConcat")
        int doLazyConcat(
            CraterString string,
            byte[] destination,
            int offset,
            @Cached AppendSubstringNode appendSubstringNode
        ) {
            for (var substring : string.getLazyConcatSubstrings()) {
                offset = appendSubstringNode.execute(substring, destination, offset);
            }

            return offset;
        }

        @Specialization(guards = "string.isLazyRepeat()")
        int doLazyRepeat(
            CraterString string,
            byte[] destination,
            int offset,
            @Cached AppendSubstringNode innerAppendSubstringNode,
            @Cached IntValueProfile repeatCountProfile
        ) {
            var repeatCount = repeatCountProfile.profile(string.getLazyRepeatCount());

            for (var i = 0; i < repeatCount; i++) {
                offset = innerAppendSubstringNode.execute((CraterString) string.objectData, destination, offset);
            }

            return offset;
        }

        @Specialization(guards = "string.isLazyReverse()")
        int doLazyReverse(
            CraterString string,
            byte[] destination,
            int offset,
            @Cached AppendSubstringNode innerAppendSubstringNode,
            @Cached IntValueProfile reversedLengthProfile
        ) {
            var start = offset;
            offset = innerAppendSubstringNode.execute((CraterString) string.objectData, destination, offset);
            var reversedLength = reversedLengthProfile.profile(offset - start);

            for (var i = 0; i < reversedLength / 2; i++) {
                var tmp = destination[start + i];
                destination[start + i] = destination[offset - 1 - i];
                destination[offset - 1 - i] = tmp;
            }

            return offset;
        }

        static AppendSubstringNode[] createAppendSubstringNodes(int length) {
            var nodes = new AppendSubstringNode[length];
            for (var i = 0; i < nodes.length; i++) nodes[i] = AppendSubstringNode.create();
            return nodes;
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class EqualsNode extends CraterNode {
        public abstract boolean execute(CraterString lhs, CraterString rhs);

        public static EqualsNode create() {
            return CraterStringFactory.EqualsNodeGen.create();
        }

        public static EqualsNode getUncached() {
            return CraterStringFactory.EqualsNodeGen.getUncached();
        }

        @Specialization(guards = "lhs == rhs")
        boolean doIdentical(CraterString lhs, CraterString rhs) {
            return true;
        }

        @Fallback
        boolean doDeep(CraterString lhs, CraterString rhs, @Cached DeepEqualsNode deepEqualsNode) {
            return deepEqualsNode.execute(lhs, rhs);
        }
    }

    @GenerateUncached
    static abstract class DeepEqualsNode extends CraterNode {
        abstract boolean execute(CraterString lhs, CraterString rhs);

        @Specialization(guards = {"lhs.isLazyLong()", "rhs.isLazyLong()"})
        boolean doLazyLongs(CraterString lhs, CraterString rhs) {
            return lhs.getLazyLongValue() == rhs.getLazyLongValue();
        }

        @Specialization(guards = {"lhs.isLazyDouble()", "rhs.isLazyDouble()"})
        boolean doLazyDoubles(CraterString lhs, CraterString rhs) {
            if (isNaN(lhs.getLazyDoubleValue()) && isNaN(rhs.getLazyDoubleValue())) {
                return true;
            }

            return lhs.getLazyDoubleValue() == rhs.getLazyDoubleValue();
        }

        @Specialization(guards = {"lhs.isLazyLong()", "rhs.isLazyDouble()"})
        boolean doLazyLongAndLazyDouble(CraterString lhs, CraterString rhs) {
            return hasExactLongValue(rhs.getLazyDoubleValue())
                && lhs.getLazyLongValue() == (long) rhs.getLazyDoubleValue();
        }

        @Specialization(guards = {"lhs.isLazyDouble()", "rhs.isLazyLong()"})
        boolean doLazyDoubleAndLazyLong(CraterString lhs, CraterString rhs) {
            return hasExactLongValue(lhs.getLazyDoubleValue())
                && (long) lhs.getLazyDoubleValue() == rhs.getLazyLongValue();
        }

        @ExplodeLoop
        @Specialization(
            guards = {
                "lhs.isLazyConcat()",
                "rhs.isLazyConcat()",
                "lhs.getLazyConcatLength() == rhs.getLazyConcatLength()",
                "lhsSubstrings.length == rhsSubstrings.length",
                "lhsSubstrings.length == innerEqualsNodes.length",
            },
            limit = "1"
        )
        boolean doExplodedLazyConcats(
            CraterString lhs,
            CraterString rhs,
            @Bind("lhs.getLazyConcatSubstrings()") CraterString[] lhsSubstrings,
            @Bind("rhs.getLazyConcatSubstrings()") CraterString[] rhsSubstrings,
            @Cached("createInnerEqualsNodes(lhsSubstrings.length)") EqualsNode[] innerEqualsNodes
        ) {
            for (var i = 0; i < innerEqualsNodes.length; i++) {
                if (!innerEqualsNodes[i].execute(lhsSubstrings[i], rhsSubstrings[i])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(
            guards = {
                "lhs.isLazyConcat()",
                "rhs.isLazyConcat()",
                "lhs.getLazyConcatLength() == rhs.getLazyConcatLength()",
                "lhsSubstrings.length == rhsSubstrings.length"
            },
            replaces = "doExplodedLazyConcats"
        )
        boolean doLazyConcats(
            CraterString lhs,
            CraterString rhs,
            @Bind("lhs.getLazyConcatSubstrings()") CraterString[] lhsSubstrings,
            @Bind("rhs.getLazyConcatSubstrings()") CraterString[] rhsSubstrings,
            @Cached EqualsNode innerEqualsNode
        ) {
            for (var i = 0; i < lhsSubstrings.length; i++) {
                if (!innerEqualsNode.execute(lhsSubstrings[i], rhsSubstrings[i])) {
                    return false;
                }
            }

            return true;
        }

        @Specialization(guards = {
            "lhs.isLazyRepeat()",
            "rhs.isLazyRepeat()",
            "lhs.getLazyRepeatLength() == rhs.getLazyRepeatLength()",
            "lhs.getLazyRepeatCount() == rhs.getLazyRepeatCount()",
        })
        boolean doLazyRepeats(CraterString lhs, CraterString rhs, @Cached EqualsNode innerEqualsNode) {
            return innerEqualsNode.execute(lhs.getLazyRepeatChild(), rhs.getLazyRepeatChild());
        }

        @Specialization(guards = {
            "lhs.isLazyReverse()",
            "rhs.isLazyReverse()",
            "lhs.getLazyReverseLength() == rhs.getLazyReverseLength()"
        })
        boolean doLazyReversals(CraterString lhs, CraterString rhs, @Cached EqualsNode innerEqualsNode) {
            return innerEqualsNode.execute(lhs.getLazyReverseChild(), rhs.getLazyRepeatChild());
        }

        @Fallback
        boolean doBytewise(
            CraterString lhs,
            CraterString rhs,
            @Cached LengthNode lhsLengthNode,
            @Cached LengthNode rhsLengthNode,
            @Cached ConditionProfile lengthMismatchProfile,
            @Cached ForceNode lhsForceNode,
            @Cached ForceNode rhsForceNode
        ) {
            var lhsLength = lhsLengthNode.execute(lhs);
            var rhsLength = rhsLengthNode.execute(rhs);

            if (lengthMismatchProfile.profile(lhsLength != rhsLength)) {
                return false;
            }

            lhsForceNode.execute(lhs);
            rhsForceNode.execute(rhs);

            return Arrays.equals(lhs.getImmediateBytes(), rhs.getImmediateBytes());
        }

        static EqualsNode[] createInnerEqualsNodes(int length) {
            var nodes = new EqualsNode[length];
            for (var i = 0; i < nodes.length; i++) nodes[i] = EqualsNode.create();
            return nodes;
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class ParseNumberNode extends CraterNode {
        public abstract Object execute(CraterString string);

        @Specialization(guards = "string.isLazyLong()")
        long doLazyLong(CraterString string) {
            return string.getLazyLongValue();
        }

        @Specialization(guards = "string.isLazyDouble()")
        double doLazyDouble(CraterString string) {
            return string.getLazyDoubleValue();
        }
    }

    private static boolean isWhitespace(byte b) {
        return switch (b) {
            case ' ', '\t', '\r', '\n', '\f', '\u000b' -> true;
            default -> false;
        };
    }

    private static boolean isDecDigit(byte b) {
        return b >= '0' && b <= '9';
    }

    private static boolean isHexDigit(byte b) {
        return (b >= '0' && b <= '9') || (b >= 'a' && b <= 'f') || (b >= 'A' && b <= 'F');
    }

    private static int decDigitValue(byte b) {
        return b - '0';
    }

    private static int hexDigitValue(byte b) {
        return switch (b) {
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
            default -> throw shouldNotReachHere();
        };
    }

    private static long parseLong(
        byte[] bytes,
        int offset,
        boolean negative
    ) throws NumberFormatException {
        var value = 0L;
        var empty = true;

        if (bytes.length - offset >= 2) {
            if (bytes[offset] == '0' && (bytes[offset + 1] == 'x' || bytes[offset + 1] == 'X')) {
                offset += 2;
                for (; offset < bytes.length && isHexDigit(bytes[offset]); offset++) {
                    value = value * 16 + hexDigitValue(bytes[offset]);
                    empty = false;
                }
            }
        }
        else for (; offset < bytes.length && isDecDigit(bytes[offset]); offset++) {
            var digitValue = decDigitValue(bytes[offset]);

            try {
                value = addExact(multiplyExact(value, 10), negative ? -digitValue : digitValue);
            }
            catch (ArithmeticException exception) {
                throw NumberFormatException.INSTANCE;
            }

            empty = false;
        }

        if (empty) {
            throw NumberFormatException.INSTANCE;
        }

        while (offset < bytes.length && isWhitespace(bytes[offset])) {
            offset++;
        }

        if (offset != bytes.length) {
            throw NumberFormatException.INSTANCE;
        }

        return value;
    }

    private static double parseDouble(byte[] bytes, int offset, boolean negative) {
        Double.parseDouble()
    }

    // private static double parseDouble(
    //     byte[] bytes,
    //     int offset,
    //     boolean negative
    // ) throws NumberFormatException {
    //     var specialCharOffset = ArrayUtils.indexOf(
    //         bytes,
    //         offset,
    //         bytes.length,
    //         (byte) '.', (byte) 'x', (byte) 'X', (byte) 'n', (byte) 'N'
    //     );

    //     if (specialCharOffset != -1) switch (bytes[specialCharOffset]) {
    //         case 'n', 'N' -> {
    //             throw NumberFormatException.INSTANCE;
    //         }

    //         case 'x', 'X' -> {
    //             return parseHexDouble(bytes, offset, negative);
    //         }
    //     }

    //     return parseDecDouble(bytes, offset, negative);
    // }

    // private static double parseDecDouble(byte[] bytes, int offset, boolean negative) {

    // }

    // private static double parseHexDouble(byte[] bytes, int offset, boolean negative) {
    //     var result = 0.0;
    //     var significantDigits = 0;
    //     var nonSignificantDigits = 0;
    //     var exponentCorrection = 0;
    //     boolean hasDot = false;

    //     if (bytes.length - offset < 2) {
    //         throw NumberFormatException.INSTANCE;
    //     }

    //     if (bytes[offset++] != '0') {
    //         throw NumberFormatException.INSTANCE;
    //     }

    //     if (bytes[offset] != 'x' && bytes[offset] != 'X') {
    //         throw NumberFormatException.INSTANCE;
    //     }

    //     offset++;

    //     for (; offset < bytes.length; offset++) {
    //         if (bytes[offset] == '.') {
    //             if (hasDot) {
    //                 break;
    //             }
    //             else {
    //                 hasDot = true;
    //             }
    //         }
    //         else if (isHexDigit(bytes[offset])) {
    //             if (significantDigits == 0 && bytes[offset] == '0') {
    //                 nonSignificantDigits = incrementExact(nonSignificantDigits);
    //             }
    //             else {
    //                 significantDigits = incrementExact(significantDigits);
    //                 if (significantDigits <= MAX_SIGNIFICANT_DIGITS) {
    //                     result = result * 16.0 + hexDigitValue(bytes[offset]);
    //                 }
    //                 else {
    //                     exponentCorrection = incrementExact(exponentCorrection);
    //                 }
    //             }

    //             if (hasDot) {
    //                 exponentCorrection = decrementExact(exponentCorrection);
    //             }
    //         }
    //         else {
    //             break;
    //         }
    //     }

    //     if (nonSignificantDigits == 0 && significantDigits == 0) {
    //         throw NumberFormatException.INSTANCE;
    //     }

    //     exponentCorrection = multiplyExact(exponentCorrection, 4);

    //     if (offset < bytes.length && (bytes[offset] == 'p' || bytes[offset] == 'P')) {
    //         var exponentValue = 0;
    //         var
    //     }

    // }

    private static int MAX_SIGNIFICANT_DIGITS = 30;

    @CompilationFinal(dimensions = 1)
    private static final byte[] DOUBLE_SPECIAL_CHARS = new byte[]{'.', 'x', 'X', 'n', 'N'};

    private static final ByteArraySupport LE_SUPPORT = ByteArraySupport.littleEndian();
}
