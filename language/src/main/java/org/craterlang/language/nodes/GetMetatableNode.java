package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import org.craterlang.language.CraterNode;
import org.craterlang.language.runtime.CraterFunction;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;

@GenerateUncached
@GeneratePackagePrivate
public abstract class GetMetatableNode extends CraterNode {
    public abstract Object execute(Object subject);

    public static GetMetatableNode create() {
        return GetMetatableNodeGen.create();
    }

    public static GetMetatableNode getUncached() {
        return GetMetatableNodeGen.getUncached();
    }

    @Specialization
    Object doNil(CraterNil subject) {
        return getContext().getNilMetatable();
    }

    @Specialization
    Object doBoolean(boolean subject) {
        return getContext().getBooleanMetatable();
    }

    @Specialization
    Object doLong(long subject) {
        return doNumber();
    }

    @Specialization
    Object doDouble(double subject) {
        return doNumber();
    }

    private Object doNumber() {
        return getContext().getNumberMetatable();
    }

    @Specialization
    Object doString(byte[] subject) {
        return getContext().getStringMetatable();
    }

    @Specialization
    Object doFunction(CraterFunction subject) {
        return getContext().getFunctionMetatable();
    }

    @Specialization
    Object doTable(CraterTable subject, @Cached CraterTable.GetMetatableNode getMetatableNode) {
        return getMetatableNode.execute(subject);
    }
}
