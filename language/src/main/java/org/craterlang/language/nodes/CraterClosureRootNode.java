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

public final class CraterClosureRootNode extends RootNode implements BytecodeOSRNode {
    @Children private final CraterInstructionNode[] instructionNodes;

    private final SourceSection sourceSection;
    private final String qualifiedName;
    private final String name;

    @CompilationFinal private Object osrMetadata;

    private CraterClosureRootNode(
        CraterLanguage language,
        FrameDescriptor frameDescriptor,
        CraterInstructionNode[] instructionNodes,
        SourceSection sourceSection,
        String qualifiedName,
        String name
    ) {
        super(language, frameDescriptor);
        this.instructionNodes = instructionNodes;
        this.sourceSection = sourceSection;
        this.qualifiedName = qualifiedName;
        this.name = name;
    }

    public CraterClosureRootNode(
        CraterLanguage language,
        FrameDescriptor frameDescriptor,
        List<CraterInstructionNode> instructionNodes,
        SourceSection sourceSection,
        String qualifiedName,
        String name
    ) {
        this(
            language,
            frameDescriptor,
            instructionNodes.toArray(CraterInstructionNode[]::new),
            sourceSection,
            qualifiedName,
            name
        );
    }

    @Override public Object execute(VirtualFrame frame) {
        try {
            executeFromIndex(frame, 0);
        }
        catch (CraterReturnException returnException) {
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

    @Override public SourceSection getSourceSection() {
        return sourceSection;
    }

    @Override public String getQualifiedName() {
        return qualifiedName;
    }

    @Override public String getName() {
        return name;
    }

    @Override protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override protected RootNode cloneUninitialized() {
        var clonedInstructionNodes = Arrays
            .stream(instructionNodes)
            .map(CraterInstructionNode::cloneUninitialized)
            .toArray(CraterInstructionNode[]::new);

        return new CraterClosureRootNode(
            getLanguage(CraterLanguage.class),
            getFrameDescriptor(),
            clonedInstructionNodes,
            sourceSection,
            qualifiedName,
            name
        );
    }
}
