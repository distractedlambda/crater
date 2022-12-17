package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GeneratePackagePrivate;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.craterlang.language.CraterNode;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class Upvalue {
    public static Upvalue createUncached(Object initialValue) {
        return UpvalueFactory.CreateNodeGen.getUncached().execute(initialValue);
    }

    public Object readUncached() {
        return UpvalueFactory.ReadNodeGen.getUncached().executeGeneric(this);
    }

    public void writeUncached(Object value) {
        UpvalueFactory.WriteNodeGen.getUncached().execute(this, value);
    }

    private static final Object BOOLEAN_TAG = new Object();
    private static final Object LONG_TAG = new Object();
    private static final Object DOUBLE_TAG = new Object();

    private Object objectValue;
    private long primitiveValue;

    private Upvalue(boolean initialValue) {
        objectValue = BOOLEAN_TAG;
        primitiveValue = initialValue ? 1 : 0;
    }

    private Upvalue(long initialValue) {
        objectValue = LONG_TAG;
        primitiveValue = initialValue;
    }

    private Upvalue(double initialValue) {
        objectValue = DOUBLE_TAG;
        primitiveValue = doubleToRawLongBits(initialValue);
    }

    private Upvalue(Object initialValue) {
        objectValue = initialValue;
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class CreateNode extends CraterNode {
        public abstract Upvalue execute(Object initialValue);
        public abstract Upvalue execute(boolean initialValue);
        public abstract Upvalue execute(long initialValue);
        public abstract Upvalue execute(double initialValue);

        public static CreateNode create() {
            return UpvalueFactory.CreateNodeGen.create();
        }

        public static CreateNode getUncached() {
            return UpvalueFactory.CreateNodeGen.getUncached();
        }

        @Specialization
        protected Upvalue doBoolean(boolean initialValue) {
            return new Upvalue(initialValue);
        }

        @Specialization
        protected Upvalue doLong(long initialValue) {
            return new Upvalue(initialValue);
        }

        @Specialization
        protected Upvalue doDouble(double initialValue) {
            return new Upvalue(initialValue);
        }

        @Fallback
        protected Upvalue doObject(Object initialValue) {
            return new Upvalue(initialValue);
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class ReadNode extends CraterNode {
        public abstract boolean executeBoolean(Upvalue upvalue) throws UnexpectedResultException;
        public abstract long executeLong(Upvalue upvalue) throws UnexpectedResultException;
        public abstract double executeDouble(Upvalue upvalue) throws UnexpectedResultException;
        public abstract Object executeGeneric(Upvalue upvalue);

        public static ReadNode create() {
            return UpvalueFactory.ReadNodeGen.create();
        }

        public static ReadNode getUncached() {
            return UpvalueFactory.ReadNodeGen.getUncached();
        }

        @Specialization(guards = "isBoolean(upvalue)")
        protected boolean doBoolean(Upvalue upvalue) {
            return upvalue.primitiveValue != 0;
        }

        @Specialization(guards = "isLong(upvalue)")
        protected long doLong(Upvalue upvalue) {
            return upvalue.primitiveValue;
        }

        @Specialization(guards = "isDouble(upvalue)")
        protected double doDouble(Upvalue upvalue) {
            return longBitsToDouble(upvalue.primitiveValue);
        }

        @Fallback
        protected Object doObject(Upvalue upvalue) {
            return upvalue.objectValue;
        }

        protected static boolean isBoolean(Upvalue upvalue) {
            return upvalue.objectValue == BOOLEAN_TAG;
        }

        protected static boolean isLong(Upvalue upvalue) {
            return upvalue.objectValue == LONG_TAG;
        }

        protected static boolean isDouble(Upvalue upvalue) {
            return upvalue.objectValue == DOUBLE_TAG;
        }
    }

    @GenerateUncached
    @GeneratePackagePrivate
    public static abstract class WriteNode extends CraterNode {
        public abstract void execute(Upvalue upvalue, boolean value);
        public abstract void execute(Upvalue upvalue, long value);
        public abstract void execute(Upvalue upvalue, double value);
        public abstract void execute(Upvalue upvalue, Object value);

        public static WriteNode create() {
            return UpvalueFactory.WriteNodeGen.create();
        }

        public static WriteNode getUncached() {
            return UpvalueFactory.WriteNodeGen.getUncached();
        }

        @Specialization
        protected void doBoolean(Upvalue upvalue, boolean value) {
            upvalue.objectValue = BOOLEAN_TAG;
            upvalue.primitiveValue = value ? 1 : 0;
        }

        @Specialization
        protected void doLong(Upvalue upvalue, long value) {
            upvalue.objectValue = LONG_TAG;
            upvalue.primitiveValue = value;
        }

        @Specialization
        protected void doDouble(Upvalue upvalue, double value) {
            upvalue.objectValue = DOUBLE_TAG;
            upvalue.primitiveValue = doubleToRawLongBits(value);
        }

        @Fallback
        protected void doObject(Upvalue upvalue, Object value) {
            upvalue.objectValue = value;
        }
    }
}
