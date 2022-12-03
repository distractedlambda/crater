package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterClosure;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;

@GenerateUncached
public abstract class CraterGetMetatableNode extends CraterNode {
    public abstract Object execute(Object subject);

    @Specialization
    protected Object doNil(CraterNil subject) {
        return getContext().getNilMetatable();
    }

    @Specialization
    protected Object doBoolean(boolean subject) {
        return getContext().getBooleanMetatable();
    }

    @Specialization
    protected Object doLong(long subject) {
        return doNumber();
    }

    @Specialization
    protected Object doDouble(double subject) {
        return doNumber();
    }

    private Object doNumber() {
        return getContext().getNumberMetatable();
    }

    @Specialization
    protected Object doString(byte[] subject) {
        return getContext().getStringMetatable();
    }

    @Specialization
    protected Object doClosure(CraterClosure subject) {
        return subject.getMetatable();
    }

    @Specialization
    protected Object doTable(CraterTable subject, @Cached CraterTable.GetMetatableNode getMetatableNode) {
        return getMetatableNode.execute(subject);
    }
}
