package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import org.craterlang.language.CraterLanguage;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.ForceIntoLongNode;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterString;

import java.util.function.Supplier;

import static org.craterlang.language.CraterTypeSystem.isNil;

@GenerateUncached
@GeneratePackagePrivate
public abstract class ErrorNode extends BuiltinFunctionBodyNode {
    public static Supplier<ErrorNode> getFactory() {
        return ErrorNodeGen::create;
    }

    @Specialization
    Object doExecute(
        Object[] arguments,
        int argumentsStart,
        int argumentsLength,
        @Cached ConditionProfile levelIsNilProfile,
        @Cached ForceIntoLongNode forceLevelIntoIntegerNode,
        @Cached AddLocationToMessageNode addLocationToMessageNode
    ) {
        var message = argumentsLength > 0 ? arguments[argumentsStart] : CraterNil.getInstance();
        var level = arguments.length > 1 ? arguments[argumentsStart + 1] : CraterNil.getInstance();

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

    @GenerateUncached
    static abstract class AddLocationToMessageNode extends CraterNode {
        abstract Object execute(Object message, long level);

        @Specialization(guards = {"level > 0", "level < 2147483647"})
        CraterString doAddLocation(
            CraterString message,
            long level,
            @Cached GetCallSiteSourceLocationNode getCallSiteSourceLocationNode,
            @Cached ConditionProfile sourceLocationIsNullProfile,
            @Cached BuildMessageWithLocationNode buildMessageWithLocationNode
        ) {
            var location = getCallSiteSourceLocationNode.execute(getCallSite((int) (level - 1)));
            if (sourceLocationIsNullProfile.profile(location == null)) {
                return message;
            }
            else {
                return buildMessageWithLocationNode.execute(message, location);
            }
        }

        @TruffleBoundary
        private static Node getCallSite(int skipFrames) {
            var callSite = Truffle.getRuntime().iterateFrames(
                frame -> {
                    var callNode = frame.getCallNode();
                    if (callNode == null) {
                        return UNKNOWN_CALL_SITE;
                    }
                    else {
                        return callNode;
                    }
                },
                skipFrames
            );

            if (callSite == UNKNOWN_CALL_SITE) {
                return null;
            }
            else {
                return (Node) callSite;
            }
        }

        private static final Object UNKNOWN_CALL_SITE = new Object();

        @Fallback
        Object doPassThrough(Object message, long level) {
            return message;
        }
    }

    @GenerateUncached
    static abstract class BuildMessageWithLocationNode extends CraterNode {
        abstract CraterString execute(CraterString message, SourceLocation sourceLocation);

        @Specialization
        CraterString doExecute(
            CraterString message,
            SourceLocation sourceLocation
        ) {
            // TODO
            return null;
        }
    }

    @GenerateUncached
    @ImportStatic(SourceLocation.class)
    static abstract class GetCallSiteSourceLocationNode extends CraterNode {
        abstract SourceLocation execute(Node callSite);

        @Specialization(guards = "callSite == null")
        SourceLocation doNull(Node callSite) {
            return null;
        }

        @Specialization(guards = {"callSite != null", "callSite == cachedCallSite"})
        SourceLocation doConstant(
            Node callSite,
            @Cached(value = "callSite", adopt = false) Node cachedCallSite,
            @Cached("ofNode(cachedCallSite)") SourceLocation sourceLocation
        ) {
            return sourceLocation;
        }

        @TruffleBoundary
        @Specialization(replaces = {"doNull", "doConstant"})
        SourceLocation doDynamic(Node node) {
            return SourceLocation.ofNode(node);
        }
    }

    record SourceLocation(CraterString file, int line, int column) {
        static SourceLocation ofSourceSection(SourceSection sourceSection) {
            return new SourceLocation(
                CraterLanguage.get(null).getInternedString(sourceSection.getSource().getPath()),
                sourceSection.getStartLine(),
                sourceSection.getStartColumn()
            );
        }

        static SourceLocation ofNode(Node node) {
            var sourceSection = node.getEncapsulatingSourceSection();
            if (sourceSection == null) {
                return null;
            }
            else {
                return ofSourceSection(sourceSection);
            }
        }
    }
}
