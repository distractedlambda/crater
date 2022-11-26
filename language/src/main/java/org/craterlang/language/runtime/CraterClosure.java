package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.profiles.ConditionProfile;
import org.craterlang.language.CraterNode;
import org.craterlang.language.CraterTypeSystem;
import org.craterlang.language.CraterTypeSystemGen;

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

        @Specialization
        protected Object doExecute(
            CraterClosure closure,
            @CachedLibrary(limit = "3") DynamicObjectLibrary dynamicObjects
        ) {
            return dynamicObjects.getOrDefault(closure, METATABLE_KEY, CraterNil.getInstance());
        }
    }

    @GenerateUncached
    public static abstract class SetMetatableNode extends CraterNode {
        public abstract void execute(CraterClosure closure, Object metatable);

        @Specialization
        protected void doExecute(
            CraterClosure closure,
            Object metatable,
            @CachedLibrary(limit = "3") DynamicObjectLibrary dynamicObjects,
            @Cached("createCountingProfile()") ConditionProfile nilProfile
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

    @ReportPolymorphism.Exclude
    public static abstract class GetCaptureNode extends CraterNode {
        protected final int index;

        protected GetCaptureNode(int index) {
            assert index >= 0;
            this.index = index;
        }

        public boolean executeBoolean(CraterClosure closure) throws UnexpectedResultException {
            return CraterTypeSystemGen.expectBoolean(executeGeneric(closure));
        }

        public abstract long executeLong(CraterClosure closure) throws UnexpectedResultException;

        public abstract double executeDouble(CraterClosure closure) throws UnexpectedResultException;

        public abstract Object executeGeneric(CraterClosure closure);
    }

    public static abstract class GetImmediateCaptureNode extends GetCaptureNode {
        protected GetImmediateCaptureNode(int index) {
            super(index);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected long doLong(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects
        ) throws UnexpectedResultException {
            return dynamicObjects.getLongOrDefault(closure, index, CraterNil.getInstance());
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doDouble(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects
        ) throws UnexpectedResultException {
            return dynamicObjects.getDoubleOrDefault(closure, index, CraterNil.getInstance());
        }

        @Specialization(replaces = {"doLong", "doDouble"})
        protected Object doGeneric(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects
        ) {
            return dynamicObjects.getOrDefault(closure, index, CraterNil.getInstance());
        }
    }

    public static abstract class GetBoxedCaptureNode extends GetCaptureNode {
        protected GetBoxedCaptureNode(int index) {
            super(index);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected boolean doBoolean(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueReadNode") CraterUpvalueBox.ReadNode upvalueReadNode
        ) throws UnexpectedResultException {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            return upvalueReadNode.executeBoolean(box);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected long doLong(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueReadNode") CraterUpvalueBox.ReadNode upvalueReadNode
        ) throws UnexpectedResultException {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            return upvalueReadNode.executeLong(box);
        }

        @Specialization(rewriteOn = UnexpectedResultException.class)
        protected double doDouble(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueReadNode") CraterUpvalueBox.ReadNode upvalueReadNode
        ) throws UnexpectedResultException {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            return upvalueReadNode.executeDouble(box);
        }

        @Specialization(replaces = {"doBoolean", "doLong", "doDouble"})
        protected Object doGeneric(
            CraterClosure closure,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueReadNode") CraterUpvalueBox.ReadNode upvalueReadNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            return upvalueReadNode.executeGeneric(box);
        }
    }

    @ReportPolymorphism.Exclude
    public static abstract class SetCaptureNode extends CraterNode {
        protected final int index;

        protected SetCaptureNode(int index) {
            assert index >= 0;
            this.index = index;
        }

        public void execute(CraterClosure closure, boolean value) {
            execute(closure, (Object) value);
        }

        public abstract void execute(CraterClosure closure, long value);

        public abstract void execute(CraterClosure closure, double value);

        public abstract void execute(CraterClosure closure, Object value);
    }

    @ImportStatic(CraterTypeSystem.class)
    public static abstract class SetImmediateCaptureNode extends SetCaptureNode {
        protected SetImmediateCaptureNode(int index) {
            super(index);
        }

        @Specialization
        protected void doLong(
            CraterClosure closure,
            long value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects
        ) {
            dynamicObjects.putLong(closure, index, value);
        }

        @Specialization
        protected void doDouble(
            CraterClosure closure,
            double value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects
        ) {
            dynamicObjects.putDouble(closure, index, value);
        }

        @Specialization(replaces = {"doLong", "doDouble"})
        protected void doGeneric(
            CraterClosure closure,
            Object value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached("createCountingProfile()") ConditionProfile nilProfile
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

        @Specialization
        protected void doBoolean(
            CraterClosure closure,
            boolean value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueWriteNode") CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            upvalueWriteNode.execute(box, value);
        }

        @Specialization
        protected void doLong(
            CraterClosure closure,
            long value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueWriteNode") CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            upvalueWriteNode.execute(box, value);
        }

        @Specialization
        protected void doDouble(
            CraterClosure closure,
            double value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueWriteNode") CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            upvalueWriteNode.execute(box, value);
        }

        @Specialization(replaces = {"doBoolean", "doLong", "doDouble"})
        protected void doGeneric(
            CraterClosure closure,
            Object value,
            @CachedLibrary(limit = "3") @Shared("dynamicObjects") DynamicObjectLibrary dynamicObjects,
            @Cached @Shared("upvalueWriteNode") CraterUpvalueBox.WriteNode upvalueWriteNode
        ) {
            var box = castExact(dynamicObjects.getOrDefault(closure, index, null), CraterUpvalueBox.class);
            upvalueWriteNode.execute(box, value);
        }
    }
}
