package org.craterlang.language.runtime;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterPathProfile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.oracle.truffle.api.ArrayUtils.indexOf;
import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;
import static com.oracle.truffle.api.CompilerDirectives.castExact;
import static com.oracle.truffle.api.CompilerDirectives.isExact;
import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.isFinite;
import static java.lang.Double.isNaN;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Math.addExact;
import static java.lang.Math.multiplyExact;
import static java.lang.Math.subtractExact;
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

        @Fallback
        int doOther(CraterString string, @Cached ForceNode forceNode) {
            forceNode.execute(string);
            return string.getImmediateLength();
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
    @ImportStatic(CraterString.class)
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

        @Specialization(guards = {"string.isLazyLong()", "string.getLazyLongValue() == cachedValue"})
        void doConstantLazyLong(
            CraterString string,
            @Cached("string.getLazyLongValue()") long cachedValue,
            @Cached(value = "longToString(cachedValue)", dimensions = 1) byte[] cachedBytes
        ) {
            string.setImmediate(cachedBytes);
        }

        @Specialization(guards = "string.isLazyLong()", replaces = "doConstantLazyLong")
        void doLazyLong(CraterString string) {
            string.setImmediate(longToString(string.getLazyLongValue()));
        }

        @Specialization(guards = {"string.isLazyDouble()", "string.getLazyDoubleValue() == cachedValue"})
        void doConstantLazyDouble(
            CraterString string,
            @Cached("string.getLazyDoubleValue()") double cachedValue,
            @Cached(value = "doubleToString(cachedValue)", dimensions = 1) byte[] cachedBytes
        ) {
            string.setImmediate(cachedBytes);
        }

        @Specialization(guards = "string.isLazyDouble()", replaces = "doConstantLazyDouble")
        void doLazyDouble(CraterString string) {
            string.setImmediate(doubleToString(string.getLazyDoubleValue()));
        }

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

        public static ParseNumberNode create() {
            return CraterStringFactory.ParseNumberNodeGen.create();
        }

        public static ParseNumberNode getUncached() {
            return CraterStringFactory.ParseNumberNodeGen.getUncached();
        }

        @Specialization(guards = "string.isLazyLong()")
        long doLazyLong(CraterString string) {
            return string.getLazyLongValue();
        }

        @Specialization(guards = "string.isLazyDouble()")
        double doLazyDouble(CraterString string) {
            return string.getLazyDoubleValue();
        }

        @Fallback
        Object doForceAndParse(CraterString string, @Cached ForceAndParseNode forceAndParseNode) {
            return forceAndParseNode.execute(string);
        }

        @GenerateUncached
        static abstract class ForceAndParseNode extends CraterNode {
            abstract Object execute(CraterString string);

            @Specialization(guards = "string == cachedString")
            Object doConstant(
                CraterString string,
                @Cached(value = "string", weak = true) CraterString cachedString,
                @Cached("parseForCache(string)") Object parsed
            ) {
                return parsed;
            }

            @Specialization(replaces = "doConstant")
            Object doDynamic(CraterString string, @Cached ForceNode forceNode) {
                forceNode.execute(string);

                var result = parseNumber(string.getImmediateBytes());

                if (result == null) {
                    transferToInterpreter();
                    throw error("");
                }

                return result;
            }

            Object parseForCache(CraterString string) {
                neverPartOfCompilation();

                string.forceUncached();
                var result = parseNumber(string.getImmediateBytes());

                if (result == null) {
                    throw error("");
                }

                return result;
            }
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class ParseLongNode extends CraterNode {
        public abstract long execute(CraterString string);

        public static ParseLongNode create() {
            return CraterStringFactory.ParseLongNodeGen.create();
        }

        public static ParseLongNode getUncached() {
            return CraterStringFactory.ParseLongNodeGen.getUncached();
        }

        @Specialization
        long doExecute(CraterString string, @Cached ParseNumberNode parseNumberNode) {
            if (parseNumberNode.execute(string) instanceof Long result) {
                return result;
            }
            else {
                transferToInterpreter();
                throw error("");
            }
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class ParseDoubleNode extends CraterNode {
        public abstract double execute(CraterString string);

        public static ParseDoubleNode create() {
            return CraterStringFactory.ParseDoubleNodeGen.create();
        }

        public static ParseDoubleNode getUncached() {
            return CraterStringFactory.ParseDoubleNodeGen.getUncached();
        }

        @Specialization
        double doExecute(
            CraterString string,
            @Cached ParseNumberNode parseNumberNode,
            @Cached CraterPathProfile pathProfile
        ) {
            var result = parseNumberNode.execute(string);
            if (result instanceof Long) {
                pathProfile.enter(0);
                return (long) result;
            }
            else {
                pathProfile.enter(1);
                return (double) result;
            }
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    @ImportStatic(Double.class)
    public static abstract class FromDoubleNode extends CraterNode {
        public abstract CraterString execute(double value);

        public static FromDoubleNode create() {
            return CraterStringFactory.FromDoubleNodeGen.create();
        }

        public static FromDoubleNode getUncached() {
            return CraterStringFactory.FromDoubleNodeGen.getUncached();
        }

        @Specialization(guards = "value == NEGATIVE_INFINITY")
        CraterString doNegativeInfinity(double value) {
            return getLanguage().getNegativeInfString();
        }

        @Specialization(guards = "value == POSITIVE_INFINITY")
        CraterString doPositiveInfinity(double value) {
            return getLanguage().getInfString();
        }

        @Specialization(guards = "isNaN(value)")
        CraterString doNaN(double value) {
            return getLanguage().getNanString();
        }

        @Specialization(guards = "isFinite(value)")
        CraterString doFinite(double value) {
            var string = new CraterString();
            string.objectData = TAG_LAZY_DOUBLE;
            string.primitiveData = doubleToRawLongBits(value);
            return string;
        }
    }

    @TruffleBoundary
    static byte[] longToString(long value) {
        return Long.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    @TruffleBoundary
    static byte[] doubleToString(double value) {
        assert isFinite(value);
        return Double.toString(value).getBytes(StandardCharsets.UTF_8);
    }

    @TruffleBoundary
    private static Object parseNumber(byte[] bytes) {
        if (bytes.length == 0) {
            return null;
        }

        var start = 0;
        var negative = false;

        findStart:
        for (;;) {
            switch (bytes[start]) {
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                case '\f':
                case 0x0b:
                    if (++start == bytes.length) {
                        return null;
                    }
                    break;

                case '-':
                    negative = true;
                case '+':
                    if (++start == bytes.length) {
                        return null;
                    }
                default:
                    break findStart;
            }
        }

        var end = bytes.length;

        findEnd:
        for (;;) {
            switch (bytes[end - 1]) {
                case ' ':
                case '\r':
                case '\n':
                case '\t':
                case '\f':
                case 0x0b:
                    end--;
                    break;

                default:
                    break findEnd;
            }
        }

        if (start == end) {
            return null;
        }

        if (bytes[start] == '0') {
            if (++start == end) {
                return 0L;
            }

            if (bytes[start] == 'x' || bytes[start] == 'X') {
                if (++start == end) {
                    return null;
                }

                return parseHexNumber(bytes, start, end, negative);
            }
        }

        return parseDecNumber(bytes, start, end, negative);
    }

    private static Object parseDecNumber(byte[] bytes, int start, int end, boolean negative) {
        var offset = start;

        tryInteger:
        for (;;) {
            long total;

            if (negative) {
                switch (bytes[offset]) {
                    case '0' -> total = -0;
                    case '1' -> total = -1;
                    case '2' -> total = -2;
                    case '3' -> total = -3;
                    case '4' -> total = -4;
                    case '5' -> total = -5;
                    case '6' -> total = -6;
                    case '7' -> total = -7;
                    case '8' -> total = -8;
                    case '9' -> total = -9;

                    case '.', 'e', 'E' -> {
                        break tryInteger;
                    }

                    default -> {
                        return null;
                    }
                }

                while (++offset < end) {
                    int digitValue;

                    switch (bytes[offset]) {
                        case '0' -> digitValue = 0;
                        case '1' -> digitValue = 1;
                        case '2' -> digitValue = 2;
                        case '3' -> digitValue = 3;
                        case '4' -> digitValue = 4;
                        case '5' -> digitValue = 5;
                        case '6' -> digitValue = 6;
                        case '7' -> digitValue = 7;
                        case '8' -> digitValue = 8;
                        case '9' -> digitValue = 9;

                        case '.', 'e', 'E' -> {
                            break tryInteger;
                        }

                        default -> {
                            return null;
                        }
                    }

                    try {
                        total = subtractExact(multiplyExact(total, 10), digitValue);
                    }
                    catch (ArithmeticException exception) {
                        break tryInteger;
                    }
                }
            }
            else {
                switch (bytes[offset]) {
                    case '0' -> total = 0;
                    case '1' -> total = 1;
                    case '2' -> total = 2;
                    case '3' -> total = 3;
                    case '4' -> total = 4;
                    case '5' -> total = 5;
                    case '6' -> total = 6;
                    case '7' -> total = 7;
                    case '8' -> total = 8;
                    case '9' -> total = 9;

                    case '.', 'e', 'E' -> {
                        break tryInteger;
                    }

                    default -> {
                        return null;
                    }
                }

                while (++offset < end) {
                    int digitValue;

                    switch (bytes[offset]) {
                        case '0' -> digitValue = 0;
                        case '1' -> digitValue = 1;
                        case '2' -> digitValue = 2;
                        case '3' -> digitValue = 3;
                        case '4' -> digitValue = 4;
                        case '5' -> digitValue = 5;
                        case '6' -> digitValue = 6;
                        case '7' -> digitValue = 7;
                        case '8' -> digitValue = 8;
                        case '9' -> digitValue = 9;

                        case '.', 'e', 'E' -> {
                            break tryInteger;
                        }

                        default -> {
                            return null;
                        }
                    }

                    try {
                        total = addExact(multiplyExact(total, 10), digitValue);
                    }
                    catch (ArithmeticException exception) {
                        break tryInteger;
                    }
                }

                return total;
            }
        }

        // Check for a trailing type suffix, which Lua would not accept
        if (isNotDecDigit(bytes[end - 1])) {
            return null;
        }

        double parsed;
        try {
            parsed = JavaDoubleParser.parseDouble(bytes, start, end - start);
        }
        catch (NumberFormatException exception) {
            return null;
        }

        return negative ? -parsed : parsed;
    }

    private static Object parseHexNumber(byte[] bytes, int start, int end, boolean negative) {
        var offset = start;

        tryInteger:
        for (;;) {
            long total;

            if (negative) {
                switch (bytes[offset]) {
                    case '0' -> total = -0;
                    case '1' -> total = -1;
                    case '2' -> total = -2;
                    case '3' -> total = -3;
                    case '4' -> total = -4;
                    case '5' -> total = -5;
                    case '6' -> total = -6;
                    case '7' -> total = -7;
                    case '8' -> total = -8;
                    case '9' -> total = -9;
                    case 'a', 'A' -> total = -10;
                    case 'b', 'B' -> total = -11;
                    case 'c', 'C' -> total = -12;
                    case 'd', 'D' -> total = -13;
                    case 'e', 'E' -> total = -14;
                    case 'f', 'F' -> total = -15;

                    case '.', 'p', 'P' -> {
                        break tryInteger;
                    }

                    default -> {
                        return null;
                    }
                }

                while (++offset < end) {
                    int digitValue;

                    switch (bytes[offset]) {
                        case '0' -> digitValue = 0;
                        case '1' -> digitValue = 1;
                        case '2' -> digitValue = 2;
                        case '3' -> digitValue = 3;
                        case '4' -> digitValue = 4;
                        case '5' -> digitValue = 5;
                        case '6' -> digitValue = 6;
                        case '7' -> digitValue = 7;
                        case '8' -> digitValue = 8;
                        case '9' -> digitValue = 9;
                        case 'a', 'A' -> digitValue = 10;
                        case 'b', 'B' -> digitValue = 11;
                        case 'c', 'C' -> digitValue = 12;
                        case 'd', 'D' -> digitValue = 13;
                        case 'e', 'E' -> digitValue = 14;
                        case 'f', 'F' -> digitValue = 15;

                        case '.', 'p', 'P' -> {
                            break tryInteger;
                        }

                        default -> {
                            return null;
                        }
                    }

                    total *= 16;
                    total -= digitValue;
                }
            }
            else {
                switch (bytes[offset]) {
                    case '0' -> total = 0;
                    case '1' -> total = 1;
                    case '2' -> total = 2;
                    case '3' -> total = 3;
                    case '4' -> total = 4;
                    case '5' -> total = 5;
                    case '6' -> total = 6;
                    case '7' -> total = 7;
                    case '8' -> total = 8;
                    case '9' -> total = 9;
                    case 'a', 'A' -> total = 10;
                    case 'b', 'B' -> total = 11;
                    case 'c', 'C' -> total = 12;
                    case 'd', 'D' -> total = 13;
                    case 'e', 'E' -> total = 14;
                    case 'f', 'F' -> total = 15;

                    case '.', 'p', 'P' -> {
                        break tryInteger;
                    }

                    default -> {
                        return null;
                    }
                }

                while (++offset < end) {
                    int digitValue;

                    switch (bytes[offset]) {
                        case '0' -> digitValue = 0;
                        case '1' -> digitValue = 1;
                        case '2' -> digitValue = 2;
                        case '3' -> digitValue = 3;
                        case '4' -> digitValue = 4;
                        case '5' -> digitValue = 5;
                        case '6' -> digitValue = 6;
                        case '7' -> digitValue = 7;
                        case '8' -> digitValue = 8;
                        case '9' -> digitValue = 9;
                        case 'a', 'A' -> digitValue = 10;
                        case 'b', 'B' -> digitValue = 11;
                        case 'c', 'C' -> digitValue = 12;
                        case 'd', 'D' -> digitValue = 13;
                        case 'e', 'E' -> digitValue = 14;
                        case 'f', 'F' -> digitValue = 15;

                        case '.', 'p', 'P' -> {
                            break tryInteger;
                        }

                        default -> {
                            return null;
                        }
                    }

                    total *= 16;
                    total += digitValue;
                }
            }

            return total;
        }

        if (bytes[offset] == '.') {
            var exponentIndex = indexOf(bytes, offset + 1, end, (byte) 'p', (byte) 'P');
            if (exponentIndex == -1) {
                return parseHexFloatAfterAddingExponent(bytes, start, end, negative);
            }
        }

        // Check for a trailing type suffix, which Lua would not accept
        if (isNotDecDigit(bytes[end - 1])) {
            return null;
        }

        double parsed;
        try {
            parsed = JavaDoubleParser.parseDouble(bytes, start - 2, end - start + 2);
        }
        catch (NumberFormatException exception) {
            return null;
        }

        return negative ? -parsed : parsed;
    }

    private static Object parseHexFloatAfterAddingExponent(byte[] bytes, int start, int end, boolean negative) {
        var withExponent = new byte[end - start + 4];

        arraycopy(bytes, start - 2, withExponent, 0, end - start + 2); // Include "0x" prefix
        withExponent[withExponent.length - 2] = 'p';
        withExponent[withExponent.length - 1] = '1';

        double parsed;
        try {
            parsed = JavaDoubleParser.parseDouble(withExponent);
        }
        catch (NumberFormatException exception) {
            return null;
        }

        return negative ? -parsed : parsed;
    }

    private static boolean isNotDecDigit(byte b) {
        return b < '0' || b > '9';
    }
}
