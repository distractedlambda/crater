package org.craterlang.language.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.BytecodeOSRNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import org.craterlang.language.CraterLanguage;

import java.util.Arrays;
import java.util.List;

import static com.oracle.truffle.api.CompilerDirectives.shouldNotReachHere;
import static com.oracle.truffle.api.nodes.BytecodeOSRNode.pollOSRBackEdge;
import static com.oracle.truffle.api.nodes.BytecodeOSRNode.tryOSR;

public final class UserFunctionRootNode extends FunctionRootNode implements BytecodeOSRNode {
    @Children private final InstructionNode[] instructionNodes;
    @CompilationFinal private Object osrMetadata;

    private UserFunctionRootNode(
        CraterLanguage language,
        FrameDescriptor frameDescriptor,
        SourceSection sourceSection,
        String qualifiedName,
        String name,
        InstructionNode[] instructionNodes
    ) {
        super(language, frameDescriptor, sourceSection, qualifiedName, name);
        this.instructionNodes = instructionNodes;
    }

    public UserFunctionRootNode(
        CraterLanguage language,
        FrameDescriptor frameDescriptor,
        SourceSection sourceSection,
        String qualifiedName,
        String name,
        List<InstructionNode> instructionNodes
    ) {
        this(
            language,
            frameDescriptor,
            sourceSection,
            qualifiedName,
            name,
            instructionNodes.toArray(InstructionNode[]::new)
        );
    }

    @Override public Object execute(VirtualFrame frame) {
        try {
            executeFromIndex(frame, 0);
        }
        catch (ReturnException returnException) {
            return returnException.getResult();
        }

        throw shouldNotReachHere();
    }

    @Override public Object executeOSR(VirtualFrame osrFrame, int target, Object interpreterState) {
        executeFromIndex(osrFrame, target);
        throw shouldNotReachHere();
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    private void executeFromIndex(VirtualFrame frame, int instructionIndex) {
        for (;;) {
            var nextIndex = instructionNodes[instructionIndex].execute(frame);

            if (nextIndex <= instructionIndex && pollOSRBackEdge(this)) {
                tryOSR(this, nextIndex, null, null, frame);
            }

            instructionIndex = nextIndex;
        }
    }

    @Override public Object getOSRMetadata() {
        return osrMetadata;
    }

    @Override public void setOSRMetadata(Object osrMetadata) {
        this.osrMetadata = osrMetadata;
    }

    @Override protected RootNode cloneUninitialized() {
        var clonedInstructionNodes = Arrays
            .stream(instructionNodes)
            .map(InstructionNode::cloneUninitialized)
            .toArray(InstructionNode[]::new);

        return new UserFunctionRootNode(
            getLanguage(CraterLanguage.class),
            getFrameDescriptor(),
            getSourceSection(),
            getQualifiedName(),
            getName(),
            clonedInstructionNodes
        );
    }
}
