package org.craterlang.language;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;

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
}
