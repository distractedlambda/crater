package org.craterlang.language.runtime.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterForceIntoIntegerNode;
import org.craterlang.language.runtime.CraterBuiltin;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.runtime.CraterMath.hasExactLongValue;

public final class CraterSelectBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterSelectBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object arguments) {
        return CraterSelectBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization(guards = "arguments.length > 1")
        Object doMultipleValues(Object[] arguments, @Cached IndexDispatchNode indexDispatchNode) {
            return indexDispatchNode.execute(arguments, arguments[0]);
        }

        @Specialization
        Object doSingleString(TruffleString argument, @Cached TruffleString.EqualNode stringEqualNode) {
            if (!stringEqualNode.execute(argument, getLanguage().getPoundSignString(), TruffleString.Encoding.BYTES)) {
                transferToInterpreter();
                throw error("");
            }

            return 0;
        }

        @Specialization
        Object[] doSingleLong(long argument) {
            if (argument <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return EMPTY_RESULTS;
        }

        @Specialization
        Object[] doSingleDouble(double argument) {
            if (!hasExactLongValue(argument) || (long) argument <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return EMPTY_RESULTS;
        }

        @Fallback
        Object doInvalid(Object arguments) {
            transferToInterpreter();
            throw error("");
        }
    }

    @GenerateUncached
    static abstract class IndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, Object index);

        @Specialization
        Object doString(
            Object[] arguments,
            TruffleString index,
            @Cached TruffleString.EqualNode stringEqualNode
        ) {
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
    static abstract class NumericIndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, long index);

        @Specialization(guards = "index >= arguments.length")
        Object[] doOverflowing(Object[] arguments, long index) {
            return EMPTY_RESULTS;
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

    private static final Object[] EMPTY_RESULTS = new Object[0];
}
