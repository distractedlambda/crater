package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LongValueProfile;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterForceIntoIntegerNode;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public abstract class CraterSelectNode extends CraterBuiltinBodyNode {
    @Specialization
    Object doExecute(
        Object[] arguments,
        int argumentsStart,
        int argumentsLength,
        @Cached IndexDispatchNode indexDispatchNode
    ) {
        if (argumentsLength == 0) {
            transferToInterpreter();
            throw error("");
        }

        return indexDispatchNode.execute(arguments, argumentsStart, argumentsLength, arguments[argumentsStart]);
    }

    static abstract class IndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, int argumentsStart, int argumentsLength, Object index);

        @Specialization
        Object doString(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            TruffleString index,
            @Cached TruffleString.EqualNode stringEqualNode
        ) {
            if (!stringEqualNode.execute(index, getLanguage().getPoundSignString(), TruffleString.Encoding.BYTES)) {
                transferToInterpreter();
                throw error("");
            }

            return argumentsLength - 1;
        }

        @Fallback
        Object doNumeric(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            Object index,
            @Cached CraterForceIntoIntegerNode forceIntoIntegerNode,
            @Cached LongValueProfile indexValueProfile,
            @Cached NumericIndexDispatchNode numericIndexDispatchNode
        ) {
            var longIndex = indexValueProfile.profile(forceIntoIntegerNode.execute(index));
            return numericIndexDispatchNode.execute(arguments, argumentsStart, argumentsLength, longIndex);
        }
    }

    static abstract class NumericIndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, int argumentsStart, int argumentsLength, long index);

        @Specialization(guards = "index >= argumentsLength")
        Object[] doOverflowing(Object[] arguments, int argumentsStart, int argumentsLength, long index) {
            return EMPTY_RESULTS;
        }

        @Specialization(guards = "index < 0")
        Object doNegative(Object[] arguments, int argumentsStart, int argumentsLength, long index) {
            index += argumentsLength;

            if (index <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments[(int) index + argumentsStart];
        }

        @Fallback
        Object doOther(Object[] arguments, int argumentsStart, int argumentsLength, long index) {
            if (index == 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments[(int) index + argumentsStart];
        }
    }

    private static final Object[] EMPTY_RESULTS = new Object[0];
}
