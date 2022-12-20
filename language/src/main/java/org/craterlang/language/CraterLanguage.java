package org.craterlang.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.utilities.AssumedValue;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterString;
import org.craterlang.language.runtime.CraterTable;
import org.craterlang.language.util.InternedSet;

@TruffleLanguage.Registration(id = "crater", name = "Crater", contextPolicy = TruffleLanguage.ContextPolicy.EXCLUSIVE)
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

    private final InternedSet<CraterString> internedStrings = new InternedSet<>(64);

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

    private final CraterString lowercaseLetterNString = getInternedString("n");
    private final CraterString poundSignString = getInternedString("#");

    private final CraterString nanString = getInternedString("nan");
    private final CraterString infString = getInternedString("inf");
    private final CraterString negativeInfString = getInternedString("-inf");

    private final InternedSet<CraterTable.Shape> internedTableShapes = new InternedSet<>(64);

    @Override protected Context createContext(Env env) {
        return new Context();
    }

    @Override protected CallTarget parse(ParsingRequest request) throws Exception {
        // TODO
        return null;
    }

    public static CraterLanguage get(Node node) {
        return REFERENCE.get(node);
    }

    @TruffleBoundary
    public CraterString getInternedString(String javaString) {
        // TODO
        return null;
    }

    public CraterString getInternedString(CraterString string) {
        return internedStrings.intern(string);
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

    public CraterString getIndexMetavalueKey() {
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

    public CraterString getNanString() {
        return nanString;
    }

    public CraterString getInfString() {
        return infString;
    }

    public CraterString getNegativeInfString() {
        return negativeInfString;
    }

    public CraterString getLowercaseLetterNString() {
        return lowercaseLetterNString;
    }

    public CraterString getPoundSignString() {
        return poundSignString;
    }

    public CraterTable.Shape getInternedTableShape(CraterTable.Shape candidate) {
        return internedTableShapes.intern(candidate);
    }

    public CraterTable createTable() {
        // TODO
        return null;
    }
}
