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
import org.craterlang.language.runtime.CraterStrings;

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

    private final CraterStrings nilString = getInternedString("nil");
    private final CraterStrings trueString = getInternedString("true");
    private final CraterStrings falseString = getInternedString("false");

    private final CraterStrings addMetamethodKey = getInternedString("__add");
    private final CraterStrings subMetamethodKey = getInternedString("__sub");
    private final CraterStrings mulMetamethodKey = getInternedString("__mul");
    private final CraterStrings divMetamethodKey = getInternedString("__div");
    private final CraterStrings modMetamethodKey = getInternedString("__mod");
    private final CraterStrings powMetamethodKey = getInternedString("__pow");
    private final CraterStrings unmMetamethodKey = getInternedString("__unm");
    private final CraterStrings idivMetamethodKey = getInternedString("__idiv");
    private final CraterStrings bandMetamethodKey = getInternedString("__band");
    private final CraterStrings borMetamethodKey = getInternedString("__bor");
    private final CraterStrings bxorMetamethodKey = getInternedString("__bxor");
    private final CraterStrings bnotMetamethodKey = getInternedString("__bnot");
    private final CraterStrings shlMetamethodKey = getInternedString("__shl");
    private final CraterStrings shrMetamethodKey = getInternedString("__shr");
    private final CraterStrings concatMetamethodKey = getInternedString("__concat");
    private final CraterStrings lenMetamethodKey = getInternedString("__len");
    private final CraterStrings eqMetamethodKey = getInternedString("__eq");
    private final CraterStrings ltMetamethodKey = getInternedString("__lt");
    private final CraterStrings leMetamethodKey = getInternedString("__le");
    private final CraterStrings indexMetamethodKey = getInternedString("__index");
    private final CraterStrings newindexMetamethodKey = getInternedString("__newindex");
    private final CraterStrings callMetamethodKey = getInternedString("__call");
    private final CraterStrings gcMetamethodKey = getInternedString("__gc");
    private final CraterStrings closeMetamethodKey = getInternedString("__close");
    private final CraterStrings modeMetavalueKey = getInternedString("__mode");
    private final CraterStrings tostringMetamethodKey = getInternedString("__tostring");
    private final CraterStrings nameMetavalueKey = getInternedString("__name");

    private final CraterStrings weakKeyModeString = getInternedString("k");
    private final CraterStrings weakValueModeString = getInternedString("v");
    private final CraterStrings weakKeyAndValueModeString = getInternedString("kv");

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

    public Shape getRootClosureShape() {
        return rootClosureShape;
    }

    @TruffleBoundary
    public CraterStrings getInternedString(String string) {
        return getInternedString(CraterStrings.fromJavaString(string));
    }

    public CraterStrings getInternedString(CraterStrings string) {
        if (isMultiContext) {
            assert threadLocalInternedStringTables != null;
            return getMultiContextInternedString(string, globalInternedStringTable, threadLocalInternedStringTables);
        }
        else {
            return getSingleContextInternedString(string, globalInternedStringTable);
        }
    }

    @TruffleBoundary
    private static CraterStrings getSingleContextInternedString(
        CraterStrings string,
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
    private static CraterStrings getMultiContextInternedString(
        CraterStrings string,
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

    public CraterStrings getNilString() {
        return nilString;
    }

    public CraterStrings getTrueString() {
        return trueString;
    }

    public CraterStrings getFalseString() {
        return falseString;
    }

    public CraterStrings getAddMetamethodKey() {
        return addMetamethodKey;
    }

    public CraterStrings getSubMetamethodKey() {
        return subMetamethodKey;
    }

    public CraterStrings getMulMetamethodKey() {
        return mulMetamethodKey;
    }

    public CraterStrings getDivMetamethodKey() {
        return divMetamethodKey;
    }

    public CraterStrings getModMetamethodKey() {
        return modMetamethodKey;
    }

    public CraterStrings getPowMetamethodKey() {
        return powMetamethodKey;
    }

    public CraterStrings getUnmMetamethodKey() {
        return unmMetamethodKey;
    }

    public CraterStrings getIdivMetamethodKey() {
        return idivMetamethodKey;
    }

    public CraterStrings getBandMetamethodKey() {
        return bandMetamethodKey;
    }

    public CraterStrings getBorMetamethodKey() {
        return borMetamethodKey;
    }

    public CraterStrings getBxorMetamethodKey() {
        return bxorMetamethodKey;
    }

    public CraterStrings getBnotMetamethodKey() {
        return bnotMetamethodKey;
    }

    public CraterStrings getShlMetamethodKey() {
        return shlMetamethodKey;
    }

    public CraterStrings getShrMetamethodKey() {
        return shrMetamethodKey;
    }

    public CraterStrings getConcatMetamethodKey() {
        return concatMetamethodKey;
    }

    public CraterStrings getLenMetamethodKey() {
        return lenMetamethodKey;
    }

    public CraterStrings getEqMetamethodKey() {
        return eqMetamethodKey;
    }

    public CraterStrings getLtMetamethodKey() {
        return ltMetamethodKey;
    }

    public CraterStrings getLeMetamethodKey() {
        return leMetamethodKey;
    }

    public CraterStrings getIndexMetamethodKey() {
        return indexMetamethodKey;
    }

    public CraterStrings getNewindexMetamethodKey() {
        return newindexMetamethodKey;
    }

    public CraterStrings getCallMetamethodKey() {
        return callMetamethodKey;
    }

    public CraterStrings getGcMetamethodKey() {
        return gcMetamethodKey;
    }

    public CraterStrings getCloseMetamethodKey() {
        return closeMetamethodKey;
    }

    public CraterStrings getModeMetavalueKey() {
        return modeMetavalueKey;
    }

    public CraterStrings getTostringMetamethodKey() {
        return tostringMetamethodKey;
    }

    public CraterStrings getNameMetavalueKey() {
        return nameMetavalueKey;
    }

    public CraterStrings getWeakKeyModeString() {
        return weakKeyModeString;
    }

    public CraterStrings getWeakValueModeString() {
        return weakValueModeString;
    }

    public CraterStrings getWeakKeyAndValueModeString() {
        return weakKeyAndValueModeString;
    }
}
