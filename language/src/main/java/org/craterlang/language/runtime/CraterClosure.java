package org.craterlang.language.runtime;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.staticobject.StaticProperty;
import com.oracle.truffle.api.staticobject.StaticShape;
import org.craterlang.language.CraterLanguage;
import org.craterlang.language.CraterNode;
import org.craterlang.language.nodes.CraterDispatchedCallNode;
import org.craterlang.language.nodes.values.CraterPrependValueNode;

import java.util.ArrayList;

import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;
import static com.oracle.truffle.api.CompilerDirectives.castExact;

public abstract class CraterClosure implements TruffleObject {
    private final Type type;
    private Object metatable = CraterNil.getInstance();

    public CraterClosure(Type type) {
        assert type != null;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Object getMetatable() {
        return metatable;
    }

    public void setMetatable(Object metatable) {
        assert metatable != null;
        this.metatable = metatable;
    }

    @TruffleBoundary(allowInlining = true)
    public CraterUpvalue getUpvalueUncached(int index) {
        return castExact(type.shape.captureProperties[index].getObject(this), CraterUpvalue.class);
    }

    private interface Factory {
        CraterClosure createInstance(Type type);
    }

    public static final class Shape {
        @CompilationFinal(dimensions = 1) private final CaptureProperty[] captureProperties;
        private final Factory instanceFactory;

        private Shape(CraterLanguage language, int captureCount) {
            neverPartOfCompilation();

            captureProperties = new CaptureProperty[captureCount];
            var shapeBuilder = StaticShape.newBuilder(language);

            for (var i = 0; i < captureCount; i++) {
                captureProperties[i] = new CaptureProperty(i);
                shapeBuilder.property(captureProperties[i], CraterUpvalue.class, true);
            }

            instanceFactory = shapeBuilder.build(CraterClosure.class, Factory.class).getFactory();
        }

        int getCaptureCount() {
            return captureProperties.length;
        }

        CaptureProperty getCaptureProperty(int index) {
            return captureProperties[index];
        }

        CraterClosure newClosure(Type type) {
            assert type.getShape() == this;
            return instanceFactory.createInstance(type);
        }

        static final class CaptureProperty extends StaticProperty {
            private final String id;

            private CaptureProperty(int index) {
                neverPartOfCompilation();
                this.id = Integer.toString(index);
            }

            @Override public String getId() {
                return id;
            }
        }
    }

    public static final class Type {
        private final CallTarget callTarget;
        private final Shape shape;

        public Type(CallTarget callTarget, Shape shape) {
            neverPartOfCompilation();

            assert callTarget != null;
            assert shape != null;

            this.callTarget = callTarget;
            this.shape = shape;
        }

        CallTarget getCallTarget() {
            return callTarget;
        }

        Shape getShape() {
            return shape;
        }
    }

    public static final class ShapeCache {
        private final CraterLanguage language;
        private final ArrayList<Shape> shapes = new ArrayList<>();

        public ShapeCache(CraterLanguage language) {
            assert language != null;
            this.language = language;
        }

        @TruffleBoundary
        public Shape getShape(int captureCount) {
            assert captureCount >= 0;
            synchronized (shapes) {
                Shape shape;

                if (captureCount < shapes.size()) {
                    shape = shapes.get(captureCount);
                }
                else {
                    shape = null;
                    shapes.ensureCapacity(captureCount);
                    while (shapes.size() <= captureCount) {
                        shapes.add(null);
                    }
                }

                if (shape == null) {
                    shape = new Shape(language, captureCount);
                    shapes.set(captureCount, shape);
                }

                return shape;
            }
        }
    }

    @GenerateUncached
    public static abstract class GetCallTargetNode extends CraterNode {
        public abstract CallTarget execute(CraterClosure closure);

        @Specialization(guards = "closure == cachedClosure")
        CallTarget doConstantClosure(
            CraterClosure closure,
            @Cached("closure") CraterClosure cachedClosure
        ) {
            return cachedClosure.type.callTarget;
        }

        @Specialization(guards = "closure.getType() == cachedType", replaces = "doConstantClosure")
        CallTarget doConstantType(CraterClosure closure, @Cached("closure.getType()") Type cachedType) {
            return cachedType.callTarget;
        }

        @Specialization(replaces = "doConstantType")
        CallTarget doDynamic(CraterClosure closure) {
            return closure.type.callTarget;
        }
    }

    public static abstract class GetUpvalueNode extends CraterNode {
        final int index;

        GetUpvalueNode(int index) {
            assert index >= 0;
            this.index = index;
        }

        public abstract CraterUpvalue execute(CraterClosure closure);

        @Specialization(guards = "closure == cachedClosure")
        CraterUpvalue doConstantClosure(
            CraterClosure closure,
            @Cached("closure") CraterClosure cachedClosure
        ) {
            return castExact(cachedClosure.type.shape.captureProperties[index].getObject(this), CraterUpvalue.class);
        }

        @Specialization(guards = "closure.getType() == cachedType", replaces = "doConstantClosure")
        CraterUpvalue doConstantType(
            CraterClosure closure,
            @Cached("closure.getType()") Type cachedType
        ) {
            return castExact(cachedType.shape.captureProperties[index].getObject(closure), CraterUpvalue.class);
        }

        @Specialization(guards = "closure.getType().getShape() == cachedShape", replaces = "doConstantType")
        CraterUpvalue doConstantShape(
            CraterClosure closure,
            @Cached("closure.getType().getShape()") Shape cachedShape
        ) {
            return castExact(cachedShape.captureProperties[index].getObject(closure), CraterUpvalue.class);
        }

        @Specialization(replaces = "doConstantShape")
        CraterUpvalue doDynamic(CraterClosure closure) {
            return closure.getUpvalueUncached(index);
        }
    }

    @GenerateUncached
    public static abstract class InvokeNode extends CraterNode {
        public abstract Object execute(CraterClosure closure, Object[] arguments);

        @Specialization
        Object doExecute(
            CraterClosure callee,
            Object[] arguments,
            @Cached GetCallTargetNode getCallTargetNode,
            @Cached CraterPrependValueNode prependCalleeNode,
            @Cached CraterDispatchedCallNode dispatchedCallNode
        ) {
            var callTarget = getCallTargetNode.execute(callee);
            var callArguments = prependCalleeNode.execute(callee, arguments);
            return dispatchedCallNode.execute(callTarget, callArguments);
        }
    }
}
