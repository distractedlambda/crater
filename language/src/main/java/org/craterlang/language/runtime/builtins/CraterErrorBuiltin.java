package org.craterlang.language.runtime.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.IntValueProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterForceIntoIntegerNode;
import org.craterlang.language.runtime.CraterBuiltin;
import org.craterlang.language.runtime.CraterNil;

import static org.craterlang.language.CraterTypeSystem.isNil;

public final class CraterErrorBuiltin extends CraterBuiltin {
    @Override public BodyNode createBodyNode() {
        return null;
    }

    @Override public Object callUncached(Object[] arguments) {
        return null;
    }

    @GenerateUncached
    static abstract class ImplNode extends BodyNode {
        @Specialization
        Object doExecute(
            Object[] arguments,
            @Cached IntValueProfile argumentsLengthProfile,
            @Cached ConditionProfile levelIsNilProfile,
            @Cached CraterForceIntoIntegerNode forceLevelIntoIntegerNode,
            @Cached AddLocationToMessageNode addLocationToMessageNode
        ) {
            Object message = CraterNil.getInstance();
            Object level = CraterNil.getInstance();

            var argumentsLength = argumentsLengthProfile.profile(arguments.length);

            if (argumentsLength > 0) {
                message = arguments[0];
                if (argumentsLength > 1) {
                    level = arguments[1];
                }
            }

            long levelInteger;

            if (levelIsNilProfile.profile(isNil(level))) {
                levelInteger = 1;
            }
            else {
                levelInteger = forceLevelIntoIntegerNode.execute(level);
            }

            message = addLocationToMessageNode.execute(message, levelInteger);

            throw error(message);
        }
    }

    @GenerateUncached
    static abstract class AddLocationToMessageNode extends CraterNode {
        abstract Object execute(Object message, long level);

        @TruffleBoundary
        @Specialization(guards = "level > 0")
        byte[] doAddLocation(byte[] message, long level) {
            var skipFrames = level - 1;

            if (skipFrames > Integer.MAX_VALUE) {
                return message;
            }

            throw new UnsupportedOperationException("TODO");

            // var location = Truffle.getRuntime().iterateFrames(
            //     frame -> {
            //         frame.

            //     },
            //     (int) (skipFrames)
            // );
        }

        @Fallback
        Object doPassThrough(Object message, long level) {
            return message;
        }
    }
}
