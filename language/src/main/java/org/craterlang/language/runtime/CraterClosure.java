package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.CraterTypeSystem;

import static com.oracle.truffle.api.CompilerDirectives.castExact;
import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;
import static org.craterlang.language.CraterTypeSystem.asNil;
import static org.craterlang.language.CraterTypeSystem.isNil;

public final class CraterClosure extends DynamicObject implements TruffleObject {
    public CraterClosure(Shape shape) {
        super(shape);
    }

    public CallTarget getCallTargetUncached() {
        return CraterClosureFactory.GetCallTargetNodeGen.getUncached().execute(this);
    }

    public Object getMetatableUncached() {
        return CraterClosureFactory.GetMetatableNodeGen.getUncached().execute(this);
    }

    public void setMetatableUncached(Object metatable) {
        CraterClosureFactory.SetMetatableNodeGen.getUncached().execute(this, metatable);
    }

    private static final HiddenKey METATABLE_KEY = new HiddenKey("metatable");

    @GenerateUncached
    public static abstract class GetCallTargetNode extends CraterNode {
        public abstract CallTarget execute(CraterClosure closure);

        @Specialization(guards = "closure == cachedClosure")
        protected CallTarget doCachedIdentity(
            CraterClosure closure,
            @Cached("closure") CraterClosure cachedClosure,
            @Cached("getCallTarget(cachedClosure.getShape())") CallTarget cachedCallTarget
        ) {
            return cachedCallTarget;
        }

        @Specialization(guards = "closure.getShape() == cachedShape", replaces = "doCachedIdentity")
        protected CallTarget doCachedShape(
            CraterClosure closure,
            @Cached("closure.getShape()") Shape cachedShape,
            @Cached("getCallTarget(cachedShape)") CallTarget cachedCallTarget
        ) {
            return cachedCallTarget;
        }

        @Megamorphic
        @Specialization(replaces = "doCachedShape")
        protected CallTarget doUncached(CraterClosure closure) {
            return (CallTarget) closure.getShape().getDynamicType();
        }

        protected static CallTarget getCallTarget(Shape shape) {
            return (CallTarget) shape.getDynamicType();
        }
    }

    @GenerateUncached
    public static abstract class GetMetatableNode extends CraterNode {
        public abstract Object execute(CraterClosure closure);

        @Specialization(limit = "3")
        protected Object doExecute(
            CraterClosure closure,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects
        ) {
            return dynamicObjects.getOrDefault(closure, METATABLE_KEY, CraterNil.getInstance());
        }
    }

    @GenerateUncached
    public static abstract class SetMetatableNode extends CraterNode {
        public abstract void execute(CraterClosure closure, Object metatable);

        @Specialization(limit = "3")
        protected void doCached(
            CraterClosure closure,
            Object metatable,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects,
            @Cached("createCountingProfile()") ConditionProfile nilProfile
        ) {
            impl(closure, metatable, dynamicObjects, nilProfile);
        }

        @Megamorphic
        @TruffleBoundary
        @Specialization(replaces = "doCached")
        protected void doUncached(CraterClosure closure, Object metatable) {
            impl(closure, metatable, DynamicObjectLibrary.getUncached(), ConditionProfile.getUncached());
        }

        private void impl(
            CraterClosure closure,
            Object metatable,
            DynamicObjectLibrary dynamicObjects,
            ConditionProfile nilProfile
        ) {
            if (nilProfile.profile(isNil(metatable))) {
                dynamicObjects.putIfPresent(closure, METATABLE_KEY, asNil(metatable));
            }
            else if (metatable instanceof CraterTable) {
                dynamicObjects.put(closure, METATABLE_KEY, metatable);
            }
            else {
                transferToInterpreter();
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    public static abstract class GetCaptureNode extends CraterNode {
        protected final int index;

        protected GetCaptureNode(int index) {
            assert index >= 0;
            this.index = index;
        }

        public abstract Object execute(CraterClosure closure);
    }

    public static abstract class GetImmediateCaptureNode extends GetCaptureNode {
        protected GetImmediateCaptureNode(int index) {
            super(index);
        }

        @Specialization(limit = "3")
        protected Object doExecute(
            CraterClosure closure,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects
        ) {
            return dynamicObjects.getOrDefault(closure, index, CraterNil.getInstance());
        }
    }

    public static abstract class GetBoxedCaptureNode extends GetCaptureNode {
        protected GetBoxedCaptureNode(int index) {
            super(index);
        }

        @Specialization(limit = "3")
        protected Object doCached(
            CraterClosure closure,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects,
            @Cached CraterUpvalueBox.ReadNode upvalueReadNode
        ) {
            return impl(closure, dynamicObjects, upvalueReadNode);
        }

        @Megamorphic
        @TruffleBoundary
        @Specialization(replaces = "doCached")
        protected Object doUncached(CraterClosure closure) {
            return impl(closure, DynamicObjectLibrary.getUncached(), CraterUpvalueBoxFactory.ReadNodeGen.getUncached());
        }

        private Object impl(
            CraterClosure closure,
            DynamicObjectLibrary dynamicObjects,
            CraterUpvalueBox.ReadNode upvalueReadNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            return upvalueReadNode.executeGeneric(box);
        }
    }

    public static abstract class SetCaptureNode extends CraterNode {
        protected final int index;

        protected SetCaptureNode(int index) {
            assert index >= 0;
            this.index = index;
        }

        public abstract void execute(CraterClosure closure, Object value);
    }

    @ImportStatic(CraterTypeSystem.class)
    public static abstract class SetImmediateCaptureNode extends SetCaptureNode {
        protected SetImmediateCaptureNode(int index) {
            super(index);
        }

        @Specialization(limit = "3")
        protected void doCached(
            CraterClosure closure,
            Object value,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects,
            @Cached("createCountingProfile()") ConditionProfile nilProfile
        ) {
            impl(closure, value, dynamicObjects, nilProfile);
        }

        @Megamorphic
        @TruffleBoundary
        @Specialization(replaces = "doCached")
        protected void doUncached(CraterClosure closure, Object value) {
            impl(closure, value, DynamicObjectLibrary.getUncached(), ConditionProfile.getUncached());
        }

        private void impl(
            CraterClosure closure,
            Object value,
            DynamicObjectLibrary dynamicObjects,
            ConditionProfile nilProfile
        ) {
            if (nilProfile.profile(isNil(value))) {
                dynamicObjects.putIfPresent(closure, index, asNil(value));
            }
            else {
                dynamicObjects.put(closure, index, value);
            }
        }
    }

    public static abstract class SetBoxedCaptureNode extends SetCaptureNode {
        protected SetBoxedCaptureNode(int index) {
            super(index);
        }

        @Specialization(limit = "3")
        protected void doCached(
            CraterClosure closure,
            Object value,
            @CachedLibrary("closure") DynamicObjectLibrary dynamicObjects,
            @Cached CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            impl(closure, value, dynamicObjects, upvalueWriteNode);
        }

        @Megamorphic
        @TruffleBoundary
        @Specialization(replaces = "doCached")
        protected void doUncached(CraterClosure closure, Object value) {
            impl(
                closure,
                value,
                DynamicObjectLibrary.getUncached(),
                CraterUpvalueBoxFactory.WriteNodeGen.getUncached()
            );
        }

        private void impl(
            CraterClosure closure,
            Object value,
            DynamicObjectLibrary dynamicObjects,
            CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            upvalueWriteNode.execute(box, value);
        }
    }
}
