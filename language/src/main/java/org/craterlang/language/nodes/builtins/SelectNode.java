package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.LongValueProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.ForceIntoLongNode;
import org.craterlang.language.runtime.CraterString;

import java.util.function.Supplier;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@GenerateUncached
@GeneratePackagePrivate
public abstract class SelectNode extends BuiltinFunctionBodyNode {
    public static Supplier<SelectNode> getFactory() {
        return SelectNodeGen::create;
    }

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

    @GenerateUncached
    static abstract class IndexDispatchNode extends CraterNode {
        abstract Object execute(Object[] arguments, int argumentsStart, int argumentsLength, Object index);

        @Specialization
        Object doString(
            Object[] arguments,
            int argumentsStart,
            int argumentsLength,
            CraterString index,
            @Cached CraterString.EqualsNode stringEqualNode
        ) {
            if (!stringEqualNode.execute(index, getLanguage().getPoundSignString())) {
                // FIXME: handle integer-valued strings
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
            @Cached ForceIntoLongNode forceIntoIntegerNode,
            @Cached LongValueProfile indexValueProfile,
            @Cached NumericIndexDispatchNode numericIndexDispatchNode
        ) {
            var longIndex = indexValueProfile.profile(forceIntoIntegerNode.execute(index));
            return numericIndexDispatchNode.execute(arguments, argumentsStart, argumentsLength, longIndex);
        }
    }

    @GenerateUncached
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
