package org.craterlang.language.runtime;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.craterlang.language.CraterNode;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Double.longBitsToDouble;

public final class CraterUpvalueBox {
    public static CraterUpvalueBox createUncached(Object initialValue) {
        return CraterUpvalueBoxFactory.CreateNodeGen.getUncached().execute(initialValue);
    }

    public Object readUncached() {
        return CraterUpvalueBoxFactory.ReadNodeGen.getUncached().executeGeneric(this);
    }

    public void writeUncached(Object value) {
        CraterUpvalueBoxFactory.WriteNodeGen.getUncached().execute(this, value);
    }

    private static final Object BOOLEAN_TAG = new Object();
    private static final Object LONG_TAG = new Object();
    private static final Object DOUBLE_TAG = new Object();

    private Object objectValue;
    private long primitiveValue;

    private CraterUpvalueBox(boolean initialValue) {
        objectValue = BOOLEAN_TAG;
        primitiveValue = initialValue ? 1 : 0;
    }

    private CraterUpvalueBox(long initialValue) {
        objectValue = LONG_TAG;
        primitiveValue = initialValue;
    }

    private CraterUpvalueBox(double initialValue) {
        objectValue = DOUBLE_TAG;
        primitiveValue = doubleToRawLongBits(initialValue);
    }

    private CraterUpvalueBox(Object initialValue) {
        objectValue = initialValue;
    }

    @GenerateUncached
    public static abstract class CreateNode extends CraterNode {
        public abstract CraterUpvalueBox execute(Object initialValue);
        public abstract CraterUpvalueBox execute(boolean initialValue);
        public abstract CraterUpvalueBox execute(long initialValue);
        public abstract CraterUpvalueBox execute(double initialValue);

        @Specialization
        protected CraterUpvalueBox doBoolean(boolean initialValue) {
            return new CraterUpvalueBox(initialValue);
        }

        @Specialization
        protected CraterUpvalueBox doLong(long initialValue) {
            return new CraterUpvalueBox(initialValue);
        }

        @Specialization
        protected CraterUpvalueBox doDouble(double initialValue) {
            return new CraterUpvalueBox(initialValue);
        }

        @Fallback
        protected CraterUpvalueBox doObject(Object initialValue) {
            return new CraterUpvalueBox(initialValue);
        }
    }

    @GenerateUncached
    public static abstract class ReadNode extends CraterNode {
        public abstract boolean executeBoolean(CraterUpvalueBox upvalueBox) throws UnexpectedResultException;
        public abstract long executeLong(CraterUpvalueBox upvalueBox) throws UnexpectedResultException;
        public abstract double executeDouble(CraterUpvalueBox upvalueBox) throws UnexpectedResultException;
        public abstract Object executeGeneric(CraterUpvalueBox upvalueBox);

        @Specialization(guards = "isBoolean(upvalueBox)")
        protected boolean doBoolean(CraterUpvalueBox upvalueBox) {
            return upvalueBox.primitiveValue != 0;
        }

        @Specialization(guards = "isLong(upvalueBox)")
        protected long doLong(CraterUpvalueBox upvalueBox) {
            return upvalueBox.primitiveValue;
        }

        @Specialization(guards = "isDouble(upvalueBox)")
        protected double doDouble(CraterUpvalueBox upvalueBox) {
            return longBitsToDouble(upvalueBox.primitiveValue);
        }

        @Fallback
        protected Object doObject(CraterUpvalueBox upvalueBox) {
            return upvalueBox.objectValue;
        }

        protected static boolean isBoolean(CraterUpvalueBox upvalueBox) {
            return upvalueBox.objectValue == BOOLEAN_TAG;
        }

        protected static boolean isLong(CraterUpvalueBox upvalueBox) {
            return upvalueBox.objectValue == LONG_TAG;
        }

        protected static boolean isDouble(CraterUpvalueBox upvalueBox) {
            return upvalueBox.objectValue == DOUBLE_TAG;
        }
    }

    @GenerateUncached
    public static abstract class WriteNode extends CraterNode {
        public abstract void execute(CraterUpvalueBox upvalueBox, boolean value);
        public abstract void execute(CraterUpvalueBox upvalueBox, long value);
        public abstract void execute(CraterUpvalueBox upvalueBox, double value);
        public abstract void execute(CraterUpvalueBox upvalueBox, Object value);

        @Specialization
        protected void doBoolean(CraterUpvalueBox upvalueBox, boolean value) {
            upvalueBox.objectValue = BOOLEAN_TAG;
            upvalueBox.primitiveValue = value ? 1 : 0;
        }

        @Specialization
        protected void doLong(CraterUpvalueBox upvalueBox, long value) {
            upvalueBox.objectValue = LONG_TAG;
            upvalueBox.primitiveValue = value;
        }

        @Specialization
        protected void doDouble(CraterUpvalueBox upvalueBox, double value) {
            upvalueBox.objectValue = DOUBLE_TAG;
            upvalueBox.primitiveValue = doubleToRawLongBits(value);
        }

        @Fallback
        protected void doObject(CraterUpvalueBox upvalueBox, Object value) {
            upvalueBox.objectValue = value;
        }
    }
}
