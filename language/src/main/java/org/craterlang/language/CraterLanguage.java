package org.craterlang.language;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.AssumedValue;
import org.craterlang.language.runtime.ConcurrentInternedSet;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;

import static com.oracle.truffle.api.CompilerAsserts.neverPartOfCompilation;

@TruffleLanguage.Registration(id = "crater", name = "Crater", contextPolicy = TruffleLanguage.ContextPolicy.SHARED)
public final class CraterLanguage extends TruffleLanguage<CraterLanguage.Context> {
    public static final class Context {
        private static final ContextReference<Context> REFERENCE = ContextReference.create(CraterLanguage.class);

        public static Context get(Node node) {
            return REFERENCE.get(node);
        }

        private final AssumedValue<Object> nilMetatable = new AssumedValue<>(CraterNil.getInstance());
        private final AssumedValue<Object> booleanMetatable = new AssumedValue<>(CraterNil.getInstance());
        private final AssumedValue<Object> numberMetatable = new AssumedValue<>(CraterNil.getInstance());
        private final AssumedValue<Object> stringMetatable = new AssumedValue<>(CraterNil.getInstance());
        private final AssumedValue<Object> functionMetatable = new AssumedValue<>(CraterNil.getInstance());

        public Object getNilMetatable() {
            return nilMetatable.get();
        }

        public void setNilMetatable(Object table) {
            assert table != null;
            nilMetatable.set(table);
        }

        public Object getBooleanMetatable() {
            return booleanMetatable.get();
        }

        public void setBooleanMetatable(Object table) {
            assert table != null;
            booleanMetatable.set(table);
        }

        public Object getNumberMetatable() {
            return numberMetatable.get();
        }

        public void setNumberMetatable(Object table) {
            assert table != null;
            numberMetatable.set(table);
        }

        public Object getStringMetatable() {
            return stringMetatable.get();
        }

        public void setStringMetatable(Object table) {
            assert table != null;
            stringMetatable.set(table);
        }

        public Object getFunctionMetatable() {
            return functionMetatable.get();
        }

        public void setFunctionMetatable(Object table) {
            assert table != null;
            functionMetatable.set(table);
        }
    }

    private static final LanguageReference<CraterLanguage> REFERENCE = LanguageReference.create(CraterLanguage.class);

    private final Assumption singleContextAssumption = Truffle.getRuntime().createAssumption("singleContext");

    private final Shape rootTableShape = Shape.newBuilder()
        .propertyAssumptions(true)
        .singleContextAssumption(singleContextAssumption)
        .layout(CraterTable.class)
        .build();

    private final ConcurrentInternedSet<TruffleString> internedStrings = new ConcurrentInternedSet<>();

    private final TruffleString nilString = getInternedString("nil");
    private final TruffleString trueString = getInternedString("true");
    private final TruffleString falseString = getInternedString("false");

    private final TruffleString addMetamethodKey = getInternedString("__add");
    private final TruffleString subMetamethodKey = getInternedString("__sub");
    private final TruffleString mulMetamethodKey = getInternedString("__mul");
    private final TruffleString divMetamethodKey = getInternedString("__div");
    private final TruffleString modMetamethodKey = getInternedString("__mod");
    private final TruffleString powMetamethodKey = getInternedString("__pow");
    private final TruffleString unmMetamethodKey = getInternedString("__unm");
    private final TruffleString idivMetamethodKey = getInternedString("__idiv");
    private final TruffleString bandMetamethodKey = getInternedString("__band");
    private final TruffleString borMetamethodKey = getInternedString("__bor");
    private final TruffleString bxorMetamethodKey = getInternedString("__bxor");
    private final TruffleString bnotMetamethodKey = getInternedString("__bnot");
    private final TruffleString shlMetamethodKey = getInternedString("__shl");
    private final TruffleString shrMetamethodKey = getInternedString("__shr");
    private final TruffleString concatMetamethodKey = getInternedString("__concat");
    private final TruffleString lenMetamethodKey = getInternedString("__len");
    private final TruffleString eqMetamethodKey = getInternedString("__eq");
    private final TruffleString ltMetamethodKey = getInternedString("__lt");
    private final TruffleString leMetamethodKey = getInternedString("__le");
    private final TruffleString indexMetamethodKey = getInternedString("__index");
    private final TruffleString newindexMetamethodKey = getInternedString("__newindex");
    private final TruffleString callMetamethodKey = getInternedString("__call");
    private final TruffleString gcMetamethodKey = getInternedString("__gc");
    private final TruffleString closeMetamethodKey = getInternedString("__close");
    private final TruffleString modeMetavalueKey = getInternedString("__mode");
    private final TruffleString tostringMetamethodKey = getInternedString("__tostring");
    private final TruffleString nameMetavalueKey = getInternedString("__name");

    private final TruffleString weakKeyModeString = getInternedString("k");
    private final TruffleString weakValueModeString = getInternedString("v");
    private final TruffleString weakKeyAndValueModeString = getInternedString("kv");

    private final TruffleString lowercaseLetterNString = getInternedString("n");
    private final TruffleString poundSignString = getInternedString("#");

    private final TruffleString nanString = getInternedString("nan");
    private final TruffleString infString = getInternedString("inf");
    private final TruffleString negativeInfString = getInternedString("-inf");

    @Override protected Context createContext(Env env) {
        return new Context();
    }

    @Override protected void initializeMultipleContexts() {
        singleContextAssumption.invalidate("initializeMultipleContexts() called");
    }

    @Override protected CallTarget parse(ParsingRequest request) throws Exception {
        // TODO
        return null;
    }

    public static CraterLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    public CraterTable createTable() {
        return new CraterTable(rootTableShape);
    }

    public Shape getFunctionRootShape(CallTarget callTarget) {
        neverPartOfCompilation();

        return Shape.newBuilder()
            .propertyAssumptions(true)
            .singleContextAssumption(singleContextAssumption)
            .dynamicType(callTarget)
            .build();
    }

    @TruffleBoundary
    public TruffleString getInternedString(String javaString) {
        var string = TruffleString
            .fromJavaStringUncached(javaString, TruffleString.Encoding.UTF_8)
            .forceEncodingUncached(TruffleString.Encoding.UTF_8, TruffleString.Encoding.BYTES);

        return getInternedString(string);
    }

    @TruffleBoundary
    public TruffleString getInternedString(TruffleString string) {
        return internedStrings.intern(string);
    }

    public TruffleString getNilString() {
        return nilString;
    }

    public TruffleString getTrueString() {
        return trueString;
    }

    public TruffleString getFalseString() {
        return falseString;
    }

    public TruffleString getAddMetamethodKey() {
        return addMetamethodKey;
    }

    public TruffleString getSubMetamethodKey() {
        return subMetamethodKey;
    }

    public TruffleString getMulMetamethodKey() {
        return mulMetamethodKey;
    }

    public TruffleString getDivMetamethodKey() {
        return divMetamethodKey;
    }

    public TruffleString getModMetamethodKey() {
        return modMetamethodKey;
    }

    public TruffleString getPowMetamethodKey() {
        return powMetamethodKey;
    }

    public TruffleString getUnmMetamethodKey() {
        return unmMetamethodKey;
    }

    public TruffleString getIdivMetamethodKey() {
        return idivMetamethodKey;
    }

    public TruffleString getBandMetamethodKey() {
        return bandMetamethodKey;
    }

    public TruffleString getBorMetamethodKey() {
        return borMetamethodKey;
    }

    public TruffleString getBxorMetamethodKey() {
        return bxorMetamethodKey;
    }

    public TruffleString getBnotMetamethodKey() {
        return bnotMetamethodKey;
    }

    public TruffleString getShlMetamethodKey() {
        return shlMetamethodKey;
    }

    public TruffleString getShrMetamethodKey() {
        return shrMetamethodKey;
    }

    public TruffleString getConcatMetamethodKey() {
        return concatMetamethodKey;
    }

    public TruffleString getLenMetamethodKey() {
        return lenMetamethodKey;
    }

    public TruffleString getEqMetamethodKey() {
        return eqMetamethodKey;
    }

    public TruffleString getLtMetamethodKey() {
        return ltMetamethodKey;
    }

    public TruffleString getLeMetamethodKey() {
        return leMetamethodKey;
    }

    public TruffleString getIndexMetavalueKey() {
        return indexMetamethodKey;
    }

    public TruffleString getNewindexMetamethodKey() {
        return newindexMetamethodKey;
    }

    public TruffleString getCallMetamethodKey() {
        return callMetamethodKey;
    }

    public TruffleString getGcMetamethodKey() {
        return gcMetamethodKey;
    }

    public TruffleString getCloseMetamethodKey() {
        return closeMetamethodKey;
    }

    public TruffleString getModeMetavalueKey() {
        return modeMetavalueKey;
    }

    public TruffleString getTostringMetamethodKey() {
        return tostringMetamethodKey;
    }

    public TruffleString getNameMetavalueKey() {
        return nameMetavalueKey;
    }

    public TruffleString getWeakKeyModeString() {
        return weakKeyModeString;
    }

    public TruffleString getWeakValueModeString() {
        return weakValueModeString;
    }

    public TruffleString getWeakKeyAndValueModeString() {
        return weakKeyAndValueModeString;
    }

    public TruffleString getNanString() {
        return nanString;
    }

    public TruffleString getInfString() {
        return infString;
    }

    public TruffleString getNegativeInfString() {
        return negativeInfString;
    }

    public TruffleString getLowercaseLetterNString() {
        return lowercaseLetterNString;
    }

    public TruffleString getPoundSignString() {
        return poundSignString;
    }
}
