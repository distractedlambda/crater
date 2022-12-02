package org.craterlang.language;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.Node;
import org.craterlang.language.runtime.CraterNil;
import org.craterlang.language.runtime.CraterStrings;

import java.nio.charset.StandardCharsets;

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

    private final CraterStrings.InternedSet globalInternedStrings = new CraterStrings.InternedSet();

    private final byte[] nilString = getInternedString("nil");
    private final byte[] trueString = getInternedString("true");
    private final byte[] falseString = getInternedString("false");

    private final byte[] addMetamethodKey = getInternedString("__add");
    private final byte[] subMetamethodKey = getInternedString("__sub");
    private final byte[] mulMetamethodKey = getInternedString("__mul");
    private final byte[] divMetamethodKey = getInternedString("__div");
    private final byte[] modMetamethodKey = getInternedString("__mod");
    private final byte[] powMetamethodKey = getInternedString("__pow");
    private final byte[] unmMetamethodKey = getInternedString("__unm");
    private final byte[] idivMetamethodKey = getInternedString("__idiv");
    private final byte[] bandMetamethodKey = getInternedString("__band");
    private final byte[] borMetamethodKey = getInternedString("__bor");
    private final byte[] bxorMetamethodKey = getInternedString("__bxor");
    private final byte[] bnotMetamethodKey = getInternedString("__bnot");
    private final byte[] shlMetamethodKey = getInternedString("__shl");
    private final byte[] shrMetamethodKey = getInternedString("__shr");
    private final byte[] concatMetamethodKey = getInternedString("__concat");
    private final byte[] lenMetamethodKey = getInternedString("__len");
    private final byte[] eqMetamethodKey = getInternedString("__eq");
    private final byte[] ltMetamethodKey = getInternedString("__lt");
    private final byte[] leMetamethodKey = getInternedString("__le");
    private final byte[] indexMetamethodKey = getInternedString("__index");
    private final byte[] newindexMetamethodKey = getInternedString("__newindex");
    private final byte[] callMetamethodKey = getInternedString("__call");
    private final byte[] gcMetamethodKey = getInternedString("__gc");
    private final byte[] closeMetamethodKey = getInternedString("__close");
    private final byte[] modeMetavalueKey = getInternedString("__mode");
    private final byte[] tostringMetamethodKey = getInternedString("__tostring");
    private final byte[] nameMetavalueKey = getInternedString("__name");

    private final byte[] weakKeyModeString = getInternedString("k");
    private final byte[] weakValueModeString = getInternedString("v");
    private final byte[] weakKeyAndValueModeString = getInternedString("kv");

    private final byte[] poundSignString = getInternedString("#");

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

    @CompilationFinal
    private boolean isMultiContext = false;

    @CompilationFinal
    private ThreadLocal<CraterStrings.InternedSet> threadLocalInternedStrings;

    @Override protected void initializeMultipleContexts() {
        isMultiContext = true;
        threadLocalInternedStrings = ThreadLocal.withInitial(CraterStrings.InternedSet::new);
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

    @TruffleBoundary
    public byte[] getInternedString(String javaString) {
        return getInternedStringFromUtf8(javaString.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getInternedStringFromUtf8(byte[] utf8) {
        if (isMultiContext) {
            assert threadLocalInternedStrings != null;
            return CraterStrings.internedFromUtf8(utf8, threadLocalInternedStrings, globalInternedStrings);
        }
        else {
            return CraterStrings.internedFromUtf8(utf8, globalInternedStrings);
        }
    }

    public byte[] getNilString() {
        return nilString;
    }

    public byte[] getTrueString() {
        return trueString;
    }

    public byte[] getFalseString() {
        return falseString;
    }

    public byte[] getAddMetamethodKey() {
        return addMetamethodKey;
    }

    public byte[] getSubMetamethodKey() {
        return subMetamethodKey;
    }

    public byte[] getMulMetamethodKey() {
        return mulMetamethodKey;
    }

    public byte[] getDivMetamethodKey() {
        return divMetamethodKey;
    }

    public byte[] getModMetamethodKey() {
        return modMetamethodKey;
    }

    public byte[] getPowMetamethodKey() {
        return powMetamethodKey;
    }

    public byte[] getUnmMetamethodKey() {
        return unmMetamethodKey;
    }

    public byte[] getIdivMetamethodKey() {
        return idivMetamethodKey;
    }

    public byte[] getBandMetamethodKey() {
        return bandMetamethodKey;
    }

    public byte[] getBorMetamethodKey() {
        return borMetamethodKey;
    }

    public byte[] getBxorMetamethodKey() {
        return bxorMetamethodKey;
    }

    public byte[] getBnotMetamethodKey() {
        return bnotMetamethodKey;
    }

    public byte[] getShlMetamethodKey() {
        return shlMetamethodKey;
    }

    public byte[] getShrMetamethodKey() {
        return shrMetamethodKey;
    }

    public byte[] getConcatMetamethodKey() {
        return concatMetamethodKey;
    }

    public byte[] getLenMetamethodKey() {
        return lenMetamethodKey;
    }

    public byte[] getEqMetamethodKey() {
        return eqMetamethodKey;
    }

    public byte[] getLtMetamethodKey() {
        return ltMetamethodKey;
    }

    public byte[] getLeMetamethodKey() {
        return leMetamethodKey;
    }

    public byte[] getIndexMetamethodKey() {
        return indexMetamethodKey;
    }

    public byte[] getNewindexMetamethodKey() {
        return newindexMetamethodKey;
    }

    public byte[] getCallMetamethodKey() {
        return callMetamethodKey;
    }

    public byte[] getGcMetamethodKey() {
        return gcMetamethodKey;
    }

    public byte[] getCloseMetamethodKey() {
        return closeMetamethodKey;
    }

    public byte[] getModeMetavalueKey() {
        return modeMetavalueKey;
    }

    public byte[] getTostringMetamethodKey() {
        return tostringMetamethodKey;
    }

    public byte[] getNameMetavalueKey() {
        return nameMetavalueKey;
    }

    public byte[] getWeakKeyModeString() {
        return weakKeyModeString;
    }

    public byte[] getWeakValueModeString() {
        return weakValueModeString;
    }

    public byte[] getWeakKeyAndValueModeString() {
        return weakKeyAndValueModeString;
    }

    public byte[] getPoundSignString() {
        return poundSignString;
    }
}
