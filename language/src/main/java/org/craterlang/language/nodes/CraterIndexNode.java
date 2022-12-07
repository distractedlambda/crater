package org.craterlang.language.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.CraterTypeSystem;
import org.craterlang.language.nodes.values.CraterAdjustToOneValueNode;
import org.craterlang.language.runtime.CraterFunction;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;

import static org.craterlang.language.CraterTypeSystem.isNil;

@ImportStatic(CraterTypeSystem.class)
public abstract class CraterIndexNode extends CraterNode {
    public abstract Object execute(Object receiver, Object key);

    @Specialization
    Object doExecute(
        CraterTable receiver,
        Object key,
        @Cached GetInTableNode getInTableNode,
        @Cached("createCountingProfile()") ConditionProfile inTableProfile,
        @Cached ThroughMetavalueNode throughMetavalueNode
    ) {
        var existing = getInTableNode.execute(receiver, key);
        if (inTableProfile.profile(!isNil(existing))) {
            return existing;
        }
        else {
            return throughMetavalueNode.execute(receiver, key);
        }
    }

    @GenerateUncached
    static abstract class GetInTableNode extends CraterNode {
        abstract Object execute(Object receiver, Object key);

        @Specialization
        Object doTable(CraterTable receiver, Object key, @Cached CraterTable.RawGetNode rawGetNode) {
            return rawGetNode.execute(receiver, key);
        }

        @Fallback
        CraterNil doOther(Object receiver, Object key) {
            return CraterNil.getInstance();
        }
    }

    static abstract class ThroughMetavalueNode extends CraterNode {
        abstract Object execute(Object receiver, Object key);

        @Specialization
        Object doExecute(
            Object receiver,
            Object key,
            @Cached CraterGetMetatableNode getMetatableNode,
            @Cached ConditionProfile noMetatableProfile,
            @Cached CraterTable.RawGetNode getMetavalueNode,
            @Cached MetavalueDispatchNode metavalueDispatchNode
        ) {
            var metatable = getMetatableNode.execute(receiver);

            if (noMetatableProfile.profile(!(metatable instanceof CraterTable))) {
                return CraterNil.getInstance();
            }

            var metavalue = getMetavalueNode.execute(
                (CraterTable) metatable,
                getLanguage().getIndexMetavalueKey()
            );

            return metavalueDispatchNode.execute(receiver, key, metavalue);
        }
    }

    static abstract class MetavalueDispatchNode extends CraterNode {
        abstract Object execute(Object receiver, Object key, Object indexMetavalue);

        @Specialization
        CraterNil doNil(Object receiver, Object key, CraterNil metavalue) {
            return CraterNil.getInstance();
        }

        @Specialization
        Object doFunction(
            Object receiver,
            Object key,
            CraterFunction function,
            @Cached CraterFunction.InvokeNode invokeNode,
            @Cached CraterAdjustToOneValueNode adjustToOneValueNode
        ) {
            return adjustToOneValueNode.execute(invokeNode.execute(function, new Object[]{receiver, key}));
        }

        @Fallback
        Object doNestedIndex(Object receiver, Object key, Object metavalue, @Cached CraterIndexNode nestedIndexNode) {
            return nestedIndexNode.execute(metavalue, key);
        }
    }
}
