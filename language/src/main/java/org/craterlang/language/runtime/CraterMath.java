package org.craterlang.language.runtime;

import com.oracle.truffle.api.nodes.UnexpectedResultException;

import static java.lang.Double.doubleToRawLongBits;
import static java.lang.Long.numberOfTrailingZeros;
import static java.lang.Math.getExponent;

public final class CraterMath {
    private CraterMath() {}

    public static boolean hasExactLongValue(double number) {
        return number >= -0x1.0p63
            && number < 0x1.0p63
            && getExponent(number) + numberOfTrailingZeros(doubleToRawLongBits(number) << 12) >= 64;
    }

    public static long expectExactLongValue(double number) throws UnexpectedResultException {
        if (hasExactLongValue(number)) {
            return (long) number;
        }
        else {
            throw new UnexpectedResultException(number);
        }
    }

    public static Object coerceExactLongValue(double number) {
        if (hasExactLongValue(number)) {
            return (long) number;
        }
        else {
            return number;
        }
    }
}
