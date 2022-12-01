package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.craterlang.language.CraterNode;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class CraterUpvalue {
    public static CraterUpvalue createUncached(Object initialValue) {
        return CraterUpvalueFactory.CreateNodeGen.getUncached().execute(initialValue);
    }

    public Object readUncached() {
        return CraterUpvalueFactory.ReadNodeGen.getUncached().executeGeneric(this);
    }

    public void writeUncached(Object value) {
        CraterUpvalueFactory.WriteNodeGen.getUncached().execute(this, value);
    }

    private static final Object BOOLEAN_TAG = new Object();
    private static final Object LONG_TAG = new Object();
    private static final Object DOUBLE_TAG = new Object();

    private Object objectValue;
    private long primitiveValue;

    private CraterUpvalue(boolean initialValue) {
        objectValue = BOOLEAN_TAG;
        primitiveValue = initialValue ? 1 : 0;
    }

    private CraterUpvalue(long initialValue) {
        objectValue = LONG_TAG;
        primitiveValue = initialValue;
    }

    private CraterUpvalue(double initialValue) {
        objectValue = DOUBLE_TAG;
        primitiveValue = doubleToRawLongBits(initialValue);
    }

    private CraterUpvalue(Object initialValue) {
        objectValue = initialValue;
    }

    @GenerateUncached
    public static abstract class CreateNode extends CraterNode {
        public abstract CraterUpvalue execute(Object initialValue);
        public abstract CraterUpvalue execute(boolean initialValue);
        public abstract CraterUpvalue execute(long initialValue);
        public abstract CraterUpvalue execute(double initialValue);

        @Specialization
        protected CraterUpvalue doBoolean(boolean initialValue) {
            return new CraterUpvalue(initialValue);
        }

        @Specialization
        protected CraterUpvalue doLong(long initialValue) {
            return new CraterUpvalue(initialValue);
        }

        @Specialization
        protected CraterUpvalue doDouble(double initialValue) {
            return new CraterUpvalue(initialValue);
        }

        @Fallback
        protected CraterUpvalue doObject(Object initialValue) {
            return new CraterUpvalue(initialValue);
        }
    }

    @GenerateUncached
    public static abstract class ReadNode extends CraterNode {
        public abstract boolean executeBoolean(CraterUpvalue upvalue) throws UnexpectedResultException;
        public abstract long executeLong(CraterUpvalue upvalue) throws UnexpectedResultException;
        public abstract double executeDouble(CraterUpvalue upvalue) throws UnexpectedResultException;
        public abstract Object executeGeneric(CraterUpvalue upvalue);

        @Specialization(guards = "isBoolean(upvalue)")
        protected boolean doBoolean(CraterUpvalue upvalue) {
            return upvalue.primitiveValue != 0;
        }

        @Specialization(guards = "isLong(upvalue)")
        protected long doLong(CraterUpvalue upvalue) {
            return upvalue.primitiveValue;
        }

        @Specialization(guards = "isDouble(upvalue)")
        protected double doDouble(CraterUpvalue upvalue) {
            return longBitsToDouble(upvalue.primitiveValue);
        }

        @Fallback
        protected Object doObject(CraterUpvalue upvalue) {
            return upvalue.objectValue;
        }

        protected static boolean isBoolean(CraterUpvalue upvalue) {
            return upvalue.objectValue == BOOLEAN_TAG;
        }

        protected static boolean isLong(CraterUpvalue upvalue) {
            return upvalue.objectValue == LONG_TAG;
        }

        protected static boolean isDouble(CraterUpvalue upvalue) {
            return upvalue.objectValue == DOUBLE_TAG;
        }
    }

    @GenerateUncached
    public static abstract class WriteNode extends CraterNode {
        public abstract void execute(CraterUpvalue upvalue, boolean value);
        public abstract void execute(CraterUpvalue upvalue, long value);
        public abstract void execute(CraterUpvalue upvalue, double value);
        public abstract void execute(CraterUpvalue upvalue, Object value);

        @Specialization
        protected void doBoolean(CraterUpvalue upvalue, boolean value) {
            upvalue.objectValue = BOOLEAN_TAG;
            upvalue.primitiveValue = value ? 1 : 0;
        }

        @Specialization
        protected void doLong(CraterUpvalue upvalue, long value) {
            upvalue.objectValue = LONG_TAG;
            upvalue.primitiveValue = value;
        }

        @Specialization
        protected void doDouble(CraterUpvalue upvalue, double value) {
            upvalue.objectValue = DOUBLE_TAG;
            upvalue.primitiveValue = doubleToRawLongBits(value);
        }

        @Fallback
        protected void doObject(CraterUpvalue upvalue, Object value) {
            upvalue.objectValue = value;
        }
    }
}
