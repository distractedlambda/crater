package org.craterlang.language.runtime;

import sun.misc.Unsafe;

import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_OBJECT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_OBJECT_INDEX_SCALE;

@SuppressWarnings("unchecked")
public class UnsafeAccess {
    public static <T> T getUnchecked(T[] array, int index) {
        assert array != null && index > 0 && index < array.length;
        return (T) UNSAFE.getObject(array, ARRAY_OBJECT_BASE_OFFSET + (long) index * ARRAY_OBJECT_INDEX_SCALE);
    }

    private static void assertInBounds(byte[] array, int offset, int size) {
        assert array != null && offset > 0 && offset <= array.length - size;
    }

    public static boolean getBooleanUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 1);
        return UNSAFE.getBoolean(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static byte getByteUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 1);
        return UNSAFE.getByte(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static short getShortUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 2);
        return UNSAFE.getShort(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static int getIntUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 4);
        return UNSAFE.getInt(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static long getLongUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 8);
        return UNSAFE.getLong(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static float getFloatUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 4);
        return UNSAFE.getFloat(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static double getDoubleUnchecked(byte[] array, int offset) {
        assertInBounds(array, offset, 8);
        return UNSAFE.getDouble(array, ARRAY_BYTE_BASE_OFFSET + offset);
    }

    public static void setBooleanUnchecked(byte[] array, int offset, boolean value) {
        assertInBounds(array, offset, 1);
        UNSAFE.putBoolean(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setByteUnchecked(byte[] array, int offset, byte value) {
        assertInBounds(array, offset, 1);
        UNSAFE.putByte(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setShortUnchecked(byte[] array, int offset, short value) {
        assertInBounds(array, offset, 2);
        UNSAFE.putShort(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setIntUnchecked(byte[] array, int offset, int value) {
        assertInBounds(array, offset, 4);
        UNSAFE.putInt(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setLongUnchecked(byte[] array, int offset, long value) {
        assertInBounds(array, offset, 8);
        UNSAFE.putLong(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setFloatUnchecked(byte[] array, int offset, float value) {
        assertInBounds(array, offset, 4);
        UNSAFE.putFloat(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    public static void setDoubleUnchecked(byte[] array, int offset, double value) {
        assertInBounds(array, offset, 8);
        UNSAFE.putDouble(array, ARRAY_BYTE_BASE_OFFSET + offset, value);
    }

    private static final Unsafe UNSAFE;

    static {
        Unsafe instance;

        try {
            instance = Unsafe.getUnsafe();
        } catch (SecurityException securityException) {
            try {
                var field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                instance = (Unsafe) field.get(null);
            }
            catch (NoSuchFieldException | SecurityException | IllegalAccessException exception) {
                exception.addSuppressed(securityException);
                throw new UnsupportedOperationException("This class requires access to sun.misc.Unsafe", exception);
            }
        }

        UNSAFE = instance;
    }
}
