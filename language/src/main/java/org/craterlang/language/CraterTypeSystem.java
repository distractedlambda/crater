package org.craterlang.language;

import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import org.craterlang.language.runtime.CraterClosure;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterString;
import org.craterlang.language.runtime.CraterTable;

@TypeSystem({
    boolean.class,
    long.class,
    double.class,
    CraterClosure.class,
    CraterNil.class,
    CraterString.class,
    CraterTable.class,
})
public abstract class CraterTypeSystem {
    @TypeCheck(CraterNil.class)
    public static boolean isNil(Object value) {
        return value == CraterNil.getInstance();
    }

    @TypeCast(CraterNil.class)
    public static CraterNil asNil(Object value) {
        assert isNil(value);
        return CraterNil.getInstance();
    }
}
