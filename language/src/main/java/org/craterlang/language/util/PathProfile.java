package org.craterlang.language.util;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public sealed abstract class PathProfile {
    private static final class Impl extends PathProfile {
        @SuppressWarnings("unused")
        @CompilationFinal private long pathMask;

        @Override public void enter(int path) {
            var bit = 1L << path;
            if ((pathMask & bit) == 0) {
                transferToInterpreterAndInvalidate();
                VH_PATH_MASK.getAndBitwiseOr(this, bit);
            }
        }

        private static final VarHandle VH_PATH_MASK;
        static {
            try {
                VH_PATH_MASK = MethodHandles.lookup().findVarHandle(Impl.class, "pathMask", long.class);
            }
            catch (NoSuchFieldException | IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    private static final class Uncached extends PathProfile {
        @Override public void enter(int path) {}

        private static final Uncached INSTANCE = new Uncached();
    }

    public abstract void enter(int path);

    public static PathProfile create() {
        return new Impl();
    }

    public static PathProfile getUncached() {
        return Uncached.INSTANCE;
    }
}
