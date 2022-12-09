package org.craterlang.language.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.NodeCost;
import org.craterlang.language.CraterNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public abstract class CraterPathProfileNode extends CraterNode {
    private static final class Impl extends CraterPathProfileNode {
        @CompilationFinal private long valueMask;

        @Override public void execute(int value) {
            var bit = 1L << value;
            if ((valueMask & bit) == 0) {
                transferToInterpreterAndInvalidate();
                var oldMask = (long) VH_VALUE_MASK.getAndBitwiseOr(this, bit);
                if ((oldMask & bit) == 0) {
                    reportPolymorphicSpecialize();
                }
            }
        }

        private static final VarHandle VH_VALUE_MASK;
        static {
            try {
                VH_VALUE_MASK = MethodHandles.lookup().findVarHandle(Impl.class, "valueMask", long.class);
            }
            catch (NoSuchFieldException | IllegalAccessException exception) {
                throw new AssertionError(exception);
            }
        }
    }

    @DenyReplace
    private static final class Uncached extends CraterPathProfileNode {
        @Override public void execute(int value) {}

        @Override public NodeCost getCost() {
            return NodeCost.MEGAMORPHIC;
        }

        @Override public boolean isAdoptable() {
            return false;
        }

        private static final Uncached INSTANCE = new Uncached();
    }

    public abstract void execute(int value);

    public static CraterPathProfileNode create() {
        return new Impl();
    }

    public static CraterPathProfileNode getUncached() {
        return Uncached.INSTANCE;
    }
}
