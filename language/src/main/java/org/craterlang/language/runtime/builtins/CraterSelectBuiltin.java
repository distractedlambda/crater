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
import org.craterlang.language.runtime.CraterMath;
import org.craterlang.language.runtime.CraterMultipleValues;
import org.craterlang.language.runtime.CraterNoValues;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

public final class CraterSelectBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return CraterSelectBuiltinFactory.ImplNodeGen.create();
    }

    @Override public Object callUncached(Object arguments) {
        return CraterSelectBuiltinFactory.ImplNodeGen.getUncached().execute(arguments);
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        Object doMultipleValues(CraterMultipleValues arguments, @Cached IndexDispatchNode indexDispatchNode) {
            return indexDispatchNode.execute(arguments, arguments.get(0));
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
        CraterNoValues doSingleLong(long argument) {
            if (argument <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return CraterNoValues.getInstance();
        }

        @Specialization
        CraterNoValues doSingleDouble(double argument) {
            if (!CraterMath.hasExactLongValue(argument) || (long) argument <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return CraterNoValues.getInstance();
        }

        @Fallback
        Object doInvalid(Object arguments) {
            transferToInterpreter();
            throw error("");
        }
    }

    @GenerateUncached
    static abstract class IndexDispatchNode extends CraterNode {
        abstract Object execute(CraterMultipleValues arguments, Object index);

        @Specialization
        Object doString(
            CraterMultipleValues arguments,
            TruffleString index,
            @Cached TruffleString.EqualNode stringEqualNode
        ) {
            if (!stringEqualNode.execute(index, getLanguage().getPoundSignString(), TruffleString.Encoding.BYTES)) {
                transferToInterpreter();
                throw error("");
            }

            return arguments.getLength() - 1;
        }

        @Fallback
        Object doNumeric(
            CraterMultipleValues arguments,
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
        abstract Object execute(CraterMultipleValues arguments, long index);

        @Specialization(guards = "index >= arguments.getLength()")
        CraterNoValues doOverflowing(CraterMultipleValues arguments, long index) {
            return CraterNoValues.getInstance();
        }

        @Specialization(guards = "index < 0")
        Object doNegative(CraterMultipleValues arguments, long index) {
            index += arguments.getLength();

            if (index <= 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments.get((int) index);
        }

        @Fallback
        Object doOther(CraterMultipleValues arguments, long index) {
            if (index == 0) {
                transferToInterpreter();
                throw error("");
            }

            return arguments.get((int) index);
        }
    }
}
