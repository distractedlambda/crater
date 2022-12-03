package org.craterlang.language.runtime.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.IntValueProfile;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterForceIntoIntegerNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterMath;
import org.craterlang.language.runtime.CraterMultipleValues;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public final class CraterSelectBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterSelectBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object[] arguments) {
        return CraterSelectBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        Object doExecute(
            Object[] arguments,
            @Cached IntValueProfile argumentsLengthProfile,
            @Cached IndexDispatchNode indexDispatchNode
        ) {
            if (arguments.length == 0) {
                transferToInterpreter();
                throw error("");
            }

            argumentsLengthProfile.profile(arguments.length);

            return indexDispatchNode.execute(arguments, arguments[0]);
        }
    }

    @GenerateUncached
    static abstract class IndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, Object index);

        @Specialization
        Object doString(Object[] arguments, TruffleString index, @Cached TruffleString.EqualNode stringEqualNode) {
            if (!stringEqualNode.execute(index, getLanguage().getPoundSignString(), TruffleString.Encoding.BYTES)) {
                transferToInterpreter();
                throw error("");
            }

            return arguments.length - 1;
        }

        @Fallback
        Object doNumeric(
            Object[] arguments,
            Object index,
            @Cached CraterForceIntoIntegerNode forceIntoIntegerNode,
            @Cached LongValueProfile indexValueProfile,
            @Cached NumericIndexDispatchNode numericIndexDispatchNode
        ) {
            var longIndex = indexValueProfile.profile(forceIntoIntegerNode.execute(index));
            return numericIndexDispatchNode.execute(arguments, longIndex);
        }
    }

    @GenerateUncached
    static abstract class ConvertNumericIndexNode extends CraterNode {
        abstract long execute(Object index);

        @Specialization
        long doLong(long index) {
            return index;
        }

        @Specialization
        long doDouble(double index) {
            if (!CraterMath.hasExactLongValue(index)) {
                transferToInterpreter();
                throw error("");
            }

            return (long) index;
        }

        @Fallback
        long doOther(Object index) {
            transferToInterpreter();
            throw error("");
        }
    }

    @GenerateUncached
    static abstract class NumericIndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, long index);

        @Specialization(guards = "index >= arguments.length")
        CraterMultipleValues doOverflowing(Object[] arguments, long index) {
            return CraterMultipleValues.getEmpty();
        }

        @Specialization(guards = "index < 0")
        Object doNegative(Object[] arguments, long index) {
            index += arguments.length;

            if (index <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments[(int) index];
        }

        @Fallback
        Object doOther(Object[] arguments, long index) {
            if (index == 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments[(int) index];
        }
    }
}
