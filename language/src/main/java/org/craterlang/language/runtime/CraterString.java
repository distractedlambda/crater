package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterPathProfileNode;

import java.util.Arrays;

import static com.oracle.truffle.api.CompilerDirectives.castExact;
import static com.oracle.truffle.api.CompilerDirectives.isExact;
import static java.lang.Double.longBitsToDouble;
import static java.lang.System.arraycopy;

public final class CraterString implements TruffleObject {
    private Object objectData;
    private long primitiveData;

    private static final Object TAG_LAZY_LONG = new Object();
    private static final Object TAG_LAZY_DOUBLE = new Object();

    private static final byte TAG_INTERNED = 0;
    private static final byte TAG_DYNAMIC = 1;
    private static final byte TAG_LAZY_CONCAT = 2;
    private static final byte TAG_LAZY_REPEAT = 3;
    private static final byte TAG_LAZY_REVERSE = 4;

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

    boolean isInterned() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_INTERNED;
    }

    byte[] getInternedBytes() {
        assert isInterned();
        return (byte[]) objectData;
    }

    int getInternedLength() {
        assert isInterned();
        return getInternedBytes().length;
    }

    int getInternedHashCode() {
        assert isInterned();
        return (int) (primitiveData >>> 8);
    }

    boolean isDynamic() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_DYNAMIC;
    }

    byte[] getDynamicBytes() {
        assert isDynamic();
        return (byte[]) objectData;
    }

    int getDynamicLength() {
        assert isDynamic();
        return getDynamicBytes().length;
    }

    boolean hasDynamicHashCode() {
        assert isDynamic();
        return primitiveData >= 0;
    }

    int getDynamicHashCode() {
        assert hasDynamicHashCode();
        return (int) (primitiveData >>> 8);
    }

    void setDynamicHashCode(int value) {
        assert isDynamic();
        primitiveData = (Integer.toUnsignedLong(value) << 8) | TAG_DYNAMIC;
    }

    void setDynamic(byte[] bytes) {
        objectData = bytes;
        primitiveData = Long.MAX_VALUE | TAG_DYNAMIC;
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

    boolean isLazyReverse() {
        return objectData instanceof byte[] && (byte) primitiveData == TAG_LAZY_REVERSE;
    }

    int getLazyReverseLength() {
        assert isLazyReverse();
        return (int) (primitiveData >>> 8);
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

        @Specialization(guards = "string.isInterned()")
        int doInterned(CraterString string) {
            return string.getInternedLength();
        }

        @Specialization(guards = "string.isDynamic()")
        int doDynamic(CraterString string) {
            return string.getDynamicLength();
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
        int doExecute(CraterString string, @Cached ForceNode forceNode, @Cached CraterPathProfileNode pathProfileNode) {
            forceNode.execute(string);

            if (string.isInterned()) {
                pathProfileNode.execute(0);
                return string.getInternedHashCode();
            }

            pathProfileNode.execute(1);

            if (!string.hasDynamicHashCode()) {
                pathProfileNode.execute(2);
                var hashCode = hashCodeBoundary(string.getDynamicBytes());
                string.setDynamicHashCode(hashCode);
            }

            return string.getDynamicHashCode();
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

        @Specialization(guards = "string.isInterned()")
        void doInterned(CraterString string) {}

        @Specialization(guards = "string.isDynamic()")
        void doDynamic(CraterString string) {}

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
            string.setDynamic(bytes);
        }
    }

    @GenerateUncached
    static abstract class AppendSubstringNode extends CraterNode {
        public abstract int execute(CraterString substring, byte[] destination, int offset);

        static AppendSubstringNode create() {
            return CraterStringFactory.AppendSubstringNodeGen.create();
        }

        @Specialization(guards = "string.isInterned()")
        int doInterned(CraterString string, byte[] destination, int offset) {
            var bytes = string.getInternedBytes();
            arraycopy(bytes, 0, destination, offset, bytes.length);
            return offset + bytes.length;
        }

        @Specialization(guards = "string.isDynamic()")
        int doDynamic(CraterString string, byte[] destination, int offset) {
            var bytes = string.getDynamicBytes();
            arraycopy(bytes, 0, destination, offset, bytes.length);
            return offset + bytes.length;
        }

        @ExplodeLoop
        @Specialization(guards = {"string.isLazyConcat()", "substrings.length == substringsLength"}, limit = "1")
        int doExplodedLazyConcat(
            CraterString string,
            byte[] destination,
            int offset,
            @Bind("string.getLazyConcatSubstrings()") CraterString[] substrings,
            @Cached("substrings.length") int substringsLength,
            @Cached("createAppendSubstringNodes(substringsLength)") AppendSubstringNode[] appendSubstringNodes
        ) {
            for (var i = 0; i < substringsLength; i++) {
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
}
