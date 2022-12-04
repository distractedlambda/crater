package org.craterlang.language;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.object.Shape;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.utilities.AssumedValue;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterTable;

import java.util.concurrent.ConcurrentHashMap;

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

    private final ConcurrentHashMap<TruffleString, TruffleString> literalStrings = new ConcurrentHashMap<>();

    private final TruffleString nilString = getLiteralString("nil");
    private final TruffleString trueString = getLiteralString("true");
    private final TruffleString falseString = getLiteralString("false");

    private final TruffleString addMetamethodKey = getLiteralString("__add");
    private final TruffleString subMetamethodKey = getLiteralString("__sub");
    private final TruffleString mulMetamethodKey = getLiteralString("__mul");
    private final TruffleString divMetamethodKey = getLiteralString("__div");
    private final TruffleString modMetamethodKey = getLiteralString("__mod");
    private final TruffleString powMetamethodKey = getLiteralString("__pow");
    private final TruffleString unmMetamethodKey = getLiteralString("__unm");
    private final TruffleString idivMetamethodKey = getLiteralString("__idiv");
    private final TruffleString bandMetamethodKey = getLiteralString("__band");
    private final TruffleString borMetamethodKey = getLiteralString("__bor");
    private final TruffleString bxorMetamethodKey = getLiteralString("__bxor");
    private final TruffleString bnotMetamethodKey = getLiteralString("__bnot");
    private final TruffleString shlMetamethodKey = getLiteralString("__shl");
    private final TruffleString shrMetamethodKey = getLiteralString("__shr");
    private final TruffleString concatMetamethodKey = getLiteralString("__concat");
    private final TruffleString lenMetamethodKey = getLiteralString("__len");
    private final TruffleString eqMetamethodKey = getLiteralString("__eq");
    private final TruffleString ltMetamethodKey = getLiteralString("__lt");
    private final TruffleString leMetamethodKey = getLiteralString("__le");
    private final TruffleString indexMetamethodKey = getLiteralString("__index");
    private final TruffleString newindexMetamethodKey = getLiteralString("__newindex");
    private final TruffleString callMetamethodKey = getLiteralString("__call");
    private final TruffleString gcMetamethodKey = getLiteralString("__gc");
    private final TruffleString closeMetamethodKey = getLiteralString("__close");
    private final TruffleString modeMetavalueKey = getLiteralString("__mode");
    private final TruffleString tostringMetamethodKey = getLiteralString("__tostring");
    private final TruffleString nameMetavalueKey = getLiteralString("__name");

    private final TruffleString weakKeyModeString = getLiteralString("k");
    private final TruffleString weakValueModeString = getLiteralString("v");
    private final TruffleString weakKeyAndValueModeString = getLiteralString("kv");

    private final TruffleString nString = getLiteralString("n");
    private final TruffleString poundSignString = getLiteralString("#");

    @Override protected Context createContext(Env env) {
        return new Context();
    }

    @Override protected void initializeMultipleContexts() {
        singleContextAssumption.invalidate("initializeMultipleContexts() called");
    }

    @Override protected CallTarget parse(ParsingRequest request) throws Exception {
        return super.parse(request);
    }

    public static CraterLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    public CraterTable createTable() {
        return new CraterTable(rootTableShape);
    }

    public TruffleString getLiteralString(String javaString) {
        var string = TruffleString
            .fromJavaStringUncached(javaString, TruffleString.Encoding.UTF_8)
            .forceEncodingUncached(TruffleString.Encoding.UTF_8, TruffleString.Encoding.BYTES);

        var existingString = literalStrings.putIfAbsent(string, string);

        if (existingString != null) {
            return existingString;
        }
        else {
            return string;
        }
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

    public TruffleString getIndexMetamethodKey() {
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

    public TruffleString getLowercaseLetterNString() {
        return nString;
    }

    public TruffleString getPoundSignString() {
        return poundSignString;
    }
}
