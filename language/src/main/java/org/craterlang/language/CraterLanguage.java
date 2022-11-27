package org.craterlang.language;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import org.craterlang.language.runtime.CraterClosure;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterString;
import org.craterlang.language.runtime.CraterTable;

@TruffleLanguage.Registration(id = "crater", name = "Crater", contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
public final class CraterLanguage extends TruffleLanguage<CraterLanguage.Context> {
    public final class Context {
        private static final ContextReference<Context> REFERENCE = ContextReference.create(CraterLanguage.class);

        public static Context get(Node node) {
            return REFERENCE.get(node);
        }

        private Object nilMetatable = CraterNil.getInstance();
        private Object booleanMetatable = CraterNil.getInstance();
        private Object numberMetatable = CraterNil.getInstance();
        private Object stringMetatable = defaultStringMetatable;
    }

    private static final LanguageReference<CraterLanguage> REFERENCE = LanguageReference.create(CraterLanguage.class);

    private final CraterInternedStringTable globalInternedStringTable = new CraterInternedStringTable();

    private final CraterString nilString = getInternedString("nil");
    private final CraterString trueString = getInternedString("true");
    private final CraterString falseString = getInternedString("false");

    private final CraterString addMetamethodKey = getInternedString("__add");
    private final CraterString subMetamethodKey = getInternedString("__sub");
    private final CraterString mulMetamethodKey = getInternedString("__mul");
    private final CraterString divMetamethodKey = getInternedString("__div");
    private final CraterString modMetamethodKey = getInternedString("__mod");
    private final CraterString powMetamethodKey = getInternedString("__pow");
    private final CraterString unmMetamethodKey = getInternedString("__unm");
    private final CraterString idivMetamethodKey = getInternedString("__idiv");
    private final CraterString bandMetamethodKey = getInternedString("__band");
    private final CraterString borMetamethodKey = getInternedString("__bor");
    private final CraterString bxorMetamethodKey = getInternedString("__bxor");
    private final CraterString bnotMetamethodKey = getInternedString("__bnot");
    private final CraterString shlMetamethodKey = getInternedString("__shl");
    private final CraterString shrMetamethodKey = getInternedString("__shr");
    private final CraterString concatMetamethodKey = getInternedString("__concat");
    private final CraterString lenMetamethodKey = getInternedString("__len");
    private final CraterString eqMetamethodKey = getInternedString("__eq");
    private final CraterString ltMetamethodKey = getInternedString("__lt");
    private final CraterString leMetamethodKey = getInternedString("__le");
    private final CraterString indexMetamethodKey = getInternedString("__index");
    private final CraterString newindexMetamethodKey = getInternedString("__newindex");
    private final CraterString callMetamethodKey = getInternedString("__call");
    private final CraterString gcMetamethodKey = getInternedString("__gc");
    private final CraterString closeMetamethodKey = getInternedString("__close");
    private final CraterString modeMetavalueKey = getInternedString("__mode");
    private final CraterString tostringMetamethodKey = getInternedString("__tostring");
    private final CraterString nameMetavalueKey = getInternedString("__name");

    private final CraterString weakKeyModeString = getInternedString("k");
    private final CraterString weakValueModeString = getInternedString("v");
    private final CraterString weakKeyAndValueModeString = getInternedString("kv");

    private final Assumption noContextOverridesNilMetatable = Truffle.getRuntime().createAssumption(
        "no context overrides the nil metatable"
    );

    private final Assumption noContextOverridesBooleanMetatable = Truffle.getRuntime().createAssumption(
        "no context overrides the boolean metatable"
    );

    private final Assumption noContextOverridesNumberMetatable = Truffle.getRuntime().createAssumption(
        "no context overrides the number metatable"
    );

    private final Assumption noContextOverridesStringMetatable = Truffle.getRuntime().createAssumption(
        "no context overrides the string metatable"
    );

    // FIXME: replace this with the actual string metatable
    private final Object defaultStringMetatable = CraterNil.getInstance();

    private final Assumption singleContextAssumption = Truffle.getRuntime().createAssumption();

    private final Shape rootTableShape = Shape.newBuilder()
        .propertyAssumptions(true)
        .singleContextAssumption(singleContextAssumption)
        .layout(CraterTable.class)
        .build();

    private final Shape rootClosureShape = Shape.newBuilder()
        .propertyAssumptions(true)
        .singleContextAssumption(singleContextAssumption)
        .layout(CraterClosure.class)
        .build();

    @CompilationFinal
    private boolean isMultiContext = false;

    @CompilationFinal
    private ThreadLocal<CraterInternedStringTable> threadLocalInternedStringTables;

    @Override protected void initializeMultipleContexts() {
        isMultiContext = true;
        threadLocalInternedStringTables = ThreadLocal.withInitial(CraterInternedStringTable::new);
        singleContextAssumption.invalidate(); // FIXME: invalidate this only when the second context is created?
    }

    @Override protected Context createContext(Env env) {
        return new Context();
    }

    @Override protected CallTarget parse(ParsingRequest request) throws Exception {
        return super.parse(request);
    }

    public static CraterLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    public boolean isSingleContext() {
        return !isMultiContext;
    }

    public boolean isMultiContext() {
        return isMultiContext;
    }

    public Object getNilMetatable(Context context) {
        if (noContextOverridesNilMetatable.isValid()) {
            return CraterNil.getInstance();
        }
        else {
            return context.nilMetatable;
        }
    }

    public void setNilMetatable(Context context, Object value) {
        context.nilMetatable = value;
        if (value != CraterNil.getInstance()) {
            noContextOverridesNilMetatable.invalidate();
        }
    }

    public Object getBooleanMetatable(Context context) {
        if (noContextOverridesBooleanMetatable.isValid()) {
            return CraterNil.getInstance();
        }
        else {
            return context.booleanMetatable;
        }
    }

    public void setBooleanMetatable(Context context, Object value) {
        context.booleanMetatable = value;
        if (value != CraterNil.getInstance()) {
            noContextOverridesBooleanMetatable.invalidate();
        }
    }

    public Object getNumberMetatable(Context context) {
        if (noContextOverridesNumberMetatable.isValid()) {
            return CraterNil.getInstance();
        }
        else {
            return context.numberMetatable;
        }
    }

    public void setNumberMetatable(Context context, Object value) {
        context.numberMetatable = value;
        if (value != CraterNil.getInstance()) {
            noContextOverridesNumberMetatable.invalidate();
        }
    }

    public Object getStringMetatable(Context context) {
        if (noContextOverridesStringMetatable.isValid()) {
            return defaultStringMetatable;
        }
        else {
            return context.stringMetatable;
        }
    }

    public void setStringMetatable(Context context, Object value) {
        context.stringMetatable = value;
        if (value != defaultStringMetatable) {
            noContextOverridesStringMetatable.invalidate();
        }
    }

    public Shape getRootTableShape() {
        return rootTableShape;
    }

    public Shape getRootClosureShape() {
        return rootClosureShape;
    }

    @TruffleBoundary
    public CraterString getInternedString(String string) {
        return getInternedString(CraterString.fromJavaString(string));
    }

    public CraterString getInternedString(CraterString string) {
        if (isMultiContext) {
            assert threadLocalInternedStringTables != null;
            return getMultiContextInternedString(string, globalInternedStringTable, threadLocalInternedStringTables);
        }
        else {
            return getSingleContextInternedString(string, globalInternedStringTable);
        }
    }

    @TruffleBoundary
    private static CraterString getSingleContextInternedString(
        CraterString string,
        CraterInternedStringTable globalTable
    ) {
        var interned = globalTable.findExisting(string);

        if (interned == null) {
            globalTable.insertAssumingNotPresent(string);
            interned = string;
        }

        return interned;
    }

    @TruffleBoundary
    private static CraterString getMultiContextInternedString(
        CraterString string,
        CraterInternedStringTable globalTable,
        ThreadLocal<CraterInternedStringTable> localTables
    ) {
        var localTable = localTables.get();
        var interned = localTable.findExisting(string);

        if (interned != null) {
            return interned;
        }

        synchronized (globalTable) {
            interned = globalTable.findExisting(string);
            if (interned == null) {
                globalTable.insertAssumingNotPresent(string);
                interned = string;
            }
        }

        localTable.insertAssumingNotPresent(interned);
        return interned;
    }

    public CraterString getNilString() {
        return nilString;
    }

    public CraterString getTrueString() {
        return trueString;
    }

    public CraterString getFalseString() {
        return falseString;
    }

    public CraterString getAddMetamethodKey() {
        return addMetamethodKey;
    }

    public CraterString getSubMetamethodKey() {
        return subMetamethodKey;
    }

    public CraterString getMulMetamethodKey() {
        return mulMetamethodKey;
    }

    public CraterString getDivMetamethodKey() {
        return divMetamethodKey;
    }

    public CraterString getModMetamethodKey() {
        return modMetamethodKey;
    }

    public CraterString getPowMetamethodKey() {
        return powMetamethodKey;
    }

    public CraterString getUnmMetamethodKey() {
        return unmMetamethodKey;
    }

    public CraterString getIdivMetamethodKey() {
        return idivMetamethodKey;
    }

    public CraterString getBandMetamethodKey() {
        return bandMetamethodKey;
    }

    public CraterString getBorMetamethodKey() {
        return borMetamethodKey;
    }

    public CraterString getBxorMetamethodKey() {
        return bxorMetamethodKey;
    }

    public CraterString getBnotMetamethodKey() {
        return bnotMetamethodKey;
    }

    public CraterString getShlMetamethodKey() {
        return shlMetamethodKey;
    }

    public CraterString getShrMetamethodKey() {
        return shrMetamethodKey;
    }

    public CraterString getConcatMetamethodKey() {
        return concatMetamethodKey;
    }

    public CraterString getLenMetamethodKey() {
        return lenMetamethodKey;
    }

    public CraterString getEqMetamethodKey() {
        return eqMetamethodKey;
    }

    public CraterString getLtMetamethodKey() {
        return ltMetamethodKey;
    }

    public CraterString getLeMetamethodKey() {
        return leMetamethodKey;
    }

    public CraterString getIndexMetamethodKey() {
        return indexMetamethodKey;
    }

    public CraterString getNewindexMetamethodKey() {
        return newindexMetamethodKey;
    }

    public CraterString getCallMetamethodKey() {
        return callMetamethodKey;
    }

    public CraterString getGcMetamethodKey() {
        return gcMetamethodKey;
    }

    public CraterString getCloseMetamethodKey() {
        return closeMetamethodKey;
    }

    public CraterString getModeMetavalueKey() {
        return modeMetavalueKey;
    }

    public CraterString getTostringMetamethodKey() {
        return tostringMetamethodKey;
    }

    public CraterString getNameMetavalueKey() {
        return nameMetavalueKey;
    }

    public CraterString getWeakKeyModeString() {
        return weakKeyModeString;
    }

    public CraterString getWeakValueModeString() {
        return weakValueModeString;
    }

    public CraterString getWeakKeyAndValueModeString() {
        return weakKeyAndValueModeString;
    }
}
