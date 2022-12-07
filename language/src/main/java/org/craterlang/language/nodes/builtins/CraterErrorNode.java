package org.craterlang.language.nodes.builtins;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import org.craterlang.language.CraterLanguage;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterForceIntoIntegerNode;
import org.craterlang.language.runtime.CraterNil;

import static org.craterlang.language.CraterTypeSystem.isNil;

public abstract class CraterErrorNode extends CraterBuiltinBodyNode {
    @Specialization
    Object doExecute(
        Object[] arguments,
        int argumentsStart,
        int argumentsLength,
        @Cached ConditionProfile levelIsNilProfile,
        @Cached CraterForceIntoIntegerNode forceLevelIntoIntegerNode,
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

    static abstract class AddLocationToMessageNode extends CraterNode {
        abstract Object execute(Object message, long level);

        @Specialization(guards = {"level > 0", "level < 2147483647"})
        TruffleString doAddLocation(
            TruffleString message,
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

    static abstract class BuildMessageWithLocationNode extends CraterNode {
        abstract TruffleString execute(TruffleString message, SourceLocation sourceLocation);

        @Specialization
        TruffleString doExecute(
            TruffleString message,
            SourceLocation sourceLocation,
            @Cached TruffleStringBuilder.AppendStringNode appendFileNode,
            @Cached TruffleStringBuilder.AppendByteNode appendFileLineSeparatorNode,
            @Cached TruffleStringBuilder.AppendIntNumberNode appendLineNumberNode,
            @Cached TruffleStringBuilder.AppendByteNode appendLineColumnSeparatorNode,
            @Cached TruffleStringBuilder.AppendIntNumberNode appendColumnNumberNode,
            @Cached TruffleStringBuilder.AppendByteNode appendLocationMessageSeparatorNode,
            @Cached TruffleStringBuilder.AppendStringNode appendMessageNode,
            @Cached TruffleStringBuilder.ToStringNode builderToStringNode
        ) {
            var builder = TruffleStringBuilder.create(TruffleString.Encoding.BYTES);
            appendFileNode.execute(builder, sourceLocation.file);
            appendFileLineSeparatorNode.execute(builder, (byte) ':');
            appendLineNumberNode.execute(builder, sourceLocation.line);
            appendLineColumnSeparatorNode.execute(builder, (byte) ':');
            appendColumnNumberNode.execute(builder, sourceLocation.column);
            appendLocationMessageSeparatorNode.execute(builder, (byte) ' ');
            appendMessageNode.execute(builder, message);
            return builderToStringNode.execute(builder);
        }
    }

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

    record SourceLocation(TruffleString file, int line, int column) {
        static SourceLocation ofSourceSection(SourceSection sourceSection) {
            return new SourceLocation(
                CraterLanguage.get(null).getLiteralString(sourceSection.getSource().getPath()),
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