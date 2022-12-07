package org.craterlang.language.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;

@ImportStatic(Double.class)
public abstract class CraterDoubleToStringNode extends CraterNode {
    public abstract TruffleString execute(double value);

    @Specialization(guards = "isNaN(value)")
    TruffleString doNaN(double value) {
        return getLanguage().getNanString();
    }

    @Specialization(guards = "value == POSITIVE_INFINITY")
    TruffleString doPositiveInfinity(double value) {
        return getLanguage().getInfString();
    }

    @Specialization(guards = "value == NEGATIVE_INFINITY")
    TruffleString doNegativeInfinity(double value) {
        return getLanguage().getNegativeInfString();
    }

    @Specialization(guards = {"isFinite(value)", "value == cachedValue"})
    TruffleString doFiniteConstant(
        double value,
        @Cached("value") double cachedValue,
        @Cached("doubleToLiteralString(cachedValue)") TruffleString result
    ) {
        return result;
    }

    TruffleString doubleToLiteralString(double value) {
        neverPartOfCompilation();
        return getLanguage().getInternedString(Double.toString(value));
    }

    @Specialization(guards = "isFinite(value)", replaces = "doFiniteConstant")
    TruffleString doFinite(double value) {
        return doubleToString(value);
    }

    @TruffleBoundary
    private static TruffleString doubleToString(double value) {
        return TruffleString
            .fromJavaStringUncached(Double.toString(value), TruffleString.Encoding.UTF_8)
            .forceEncodingUncached(TruffleString.Encoding.UTF_8, TruffleString.Encoding.BYTES);
    }
}
