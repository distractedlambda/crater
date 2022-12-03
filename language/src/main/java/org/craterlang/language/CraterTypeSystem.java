package org.craterlang.language;

import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;
import org.craterlang.language.runtime.CraterClosure;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;
import org.craterlang.language.runtime.CraterMultipleValues;

@TypeSystem({
    boolean.class,
    long.class,
    double.class,
    CraterClosure.class,
    CraterMultipleValues.class,
    CraterNil.class,
    CraterTable.class,
    TruffleString.class,
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
