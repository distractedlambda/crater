package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import org.craterlang.language.CraterNode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.lang.System.arraycopy;

public final class CraterStrings implements TruffleObject {
    @CompilationFinal(dimensions = 1) private final byte[] bytes;
    int cachedHashCode;

    private CraterStrings(byte[] bytes) {
        assert bytes != null;
        this.bytes = bytes;
    }

    public static CraterStrings wrapping(byte[] bytes) {
        return new CraterStrings(bytes);
    }

    @TruffleBoundary
    public static CraterStrings fromJavaString(String string) {
        return new CraterStrings(string.getBytes(StandardCharsets.UTF_8));
    }

    public int getLength() {
        return bytes.length;
    }

    public byte getByte(int index) {
        return bytes[index];
    }

    public byte[] getInternalByteArray() {
        return bytes;
    }

    @TruffleBoundary(allowInlining = true)
    @Override public boolean equals(Object obj) {
        if (!(obj instanceof CraterStrings other)) {
            return false;
        }

        return CraterStringsFactory.EqualsNodeGen.create().execute(this, other);
    }

    @TruffleBoundary(allowInlining = true)
    @Override public int hashCode() {
        return CraterStringsFactory.HashCodeNodeGen.getUncached().execute(this);
    }

    @GenerateUncached
    public static abstract class HashCodeNode extends CraterNode {
        public abstract int execute(CraterStrings string);

        @Specialization(guards = "string == cachedString")
        protected int doCachedIdentity(
            CraterStrings string,
            @Cached("string") CraterStrings cachedString,
            @Cached("cachedString.hashCode()") int cachedHashCode
        ) {
            return cachedHashCode;
        }

        @Specialization(guards = "hashCode != 0", replaces = "doCachedIdentity")
        protected int doReadyHashCode(CraterStrings string, @Bind("string.cachedHashCode") int hashCode) {
            return hashCode;
        }

        @Specialization(replaces = "doReadyHashCode")
        protected int doAnyString(CraterStrings string) {
            var hashCode = string.cachedHashCode;
            if (hashCode != 0) {
                return hashCode;
            }
            else {
                return (string.cachedHashCode = computeHashCode(string.bytes));
            }
        }

        @TruffleBoundary
        private static int computeHashCode(byte[] bytes) {
            var code = CraterHashing.hash(bytes);
            return code != 0 ? code : -1;
        }
    }

    @GenerateUncached
    public static abstract class EqualsNode extends CraterNode {
        public abstract boolean execute(CraterStrings lhs, CraterStrings rhs);

        @Specialization(guards = "lhs == rhs")
        protected boolean doIdenticalStrings(CraterStrings lhs, CraterStrings rhs) {
            return true;
        }

        @Fallback
        protected boolean doDeepComparison(
            CraterStrings lhs,
            CraterStrings rhs,
            @Cached ConditionProfile hashCodeMismatch
        ) {
            if (hashCodeMismatch.profile(lhs.cachedHashCode != 0 && lhs.cachedHashCode != rhs.cachedHashCode)) {
                return false;
            }
            else {
                return Arrays.equals(lhs.bytes, rhs.bytes);
            }
        }
    }

    @GenerateUncached
    public static abstract class ConcatNode extends CraterNode {
        public abstract CraterStrings execute(CraterStrings lhs, CraterStrings rhs);

        @Specialization(guards = {"lhs == cachedLhs", "rhs == cachedRhs"})
        protected CraterStrings doConstant(
            CraterStrings lhs,
            CraterStrings rhs,
            @Cached("lhs") CraterStrings cachedLhs,
            @Cached("rhs") CraterStrings cachedRhs,
            @Cached("concatInterned(cachedLhs, cachedRhs)") CraterStrings cachedResult
        ) {
            return cachedResult;
        }

        @Specialization(replaces = "doConstant")
        protected CraterStrings doDynamic(
            CraterStrings lhs,
            CraterStrings rhs,
            @Cached IntValueProfile lhsLengthProfile,
            @Cached IntValueProfile rhsLengthProfile
        ) {
            var lhsLength = lhsLengthProfile.profile(lhs.bytes.length);
            if (lhsLength == 0) {
                return rhs;
            }

            var rhsLength = rhsLengthProfile.profile(rhs.bytes.length);
            if (rhsLength == 0) {
                return lhs;
            }

            return new CraterStrings(concatBytes(lhs.bytes, rhs.bytes));
        }

        @TruffleBoundary
        protected CraterStrings concatInterned(CraterStrings lhs, CraterStrings rhs) {
            return getLanguage().getInternedString(new CraterStrings(concatBytes(lhs.bytes, rhs.bytes)));
        }

        @TruffleBoundary(allowInlining = true)
        private static byte[] concatBytes(byte[] lhs, byte[] rhs) {
            var combined = Arrays.copyOf(lhs, lhs.length + rhs.length);
            arraycopy(rhs, 0, combined, lhs.length, rhs.length);
            return combined;
        }
    }
}
