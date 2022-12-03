package org.craterlang.language;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.craterlang.language.runtime.CraterErrorException;

import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;

@ReportPolymorphism
@NodeInfo(language = "Crater")
@TypeSystemReference(CraterTypeSystem.class)
public abstract class CraterNode extends Node {
    protected final CraterLanguage getLanguage() {
        return CraterLanguage.get(this);
    }

    protected final CraterLanguage.Context getContext() {
        return CraterLanguage.Context.get(this);
    }

    protected final CraterErrorException error(Object payload) {
        return CraterErrorException.create(payload, this);
    }

    protected final CraterErrorException error(String message) {
        neverPartOfCompilation();
        return CraterErrorException.create(getLanguage().getLiteralString(message), this);
    }
}
