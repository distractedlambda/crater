package org.craterlang.language.nodes.values;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import org.craterlang.language.CraterNode;

import static com.oracle.truffle.api.CompilerDirectives.ensureVirtualizedHere;
import static java.lang.System.arraycopy;

@GenerateUncached
public abstract class CraterPrependValueNode extends CraterNode {
    public abstract Object[] execute(Object head, Object[] tail);

    @ExplodeLoop
    @Specialization(guards = "tail.length == cachedLength", limit = "1")
    protected Object[] doCachedLength(Object head, Object[] tail, @Cached("tail.length") int cachedLength) {
        var combined = new Object[cachedLength + 1];
        combined[0] = head;
        for (var i = 0; i < cachedLength; i++) combined[i + 1] = tail[i];
        ensureVirtualizedHere(combined);
        return combined;
    }

    @TruffleBoundary(allowInlining = true)
    @Specialization(replaces = "doCachedLength")
    protected Object[] doDynamicLength(Object head, Object[] tail) {
        var combined = new Object[tail.length + 1];
        combined[0] = head;
        arraycopy(tail, 0, combined, 1, tail.length);
        return combined;
    }
}
