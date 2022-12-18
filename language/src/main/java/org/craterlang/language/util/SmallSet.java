package org.craterlang.language.util;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import org.graalvm.collections.EconomicSet;

import java.lang.invoke.VarHandle;
import java.util.function.Consumer;

import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;

@ValueType
@SuppressWarnings("unchecked")
public final class SmallSet<E> {
    private final Object owner;
    private final VarHandle storageHandle;

    public SmallSet(Object owner, VarHandle storageHandle) {
        this.owner = requireNonNull(owner);
        this.storageHandle = requireNonNull(storageHandle);
    }

    public boolean contains(E element) {
        assert !(element instanceof EconomicSet<?>);
        var storage = storageHandle.get(owner);
        if (storage == null) {
            return false;
        }
        else if (storage instanceof EconomicSet<?> set) {
            return ((EconomicSet<E>) set).contains(element);
        }
        else {
            return element.equals(storage);
        }
    }

    public boolean add(E element) {
        assert !(element instanceof EconomicSet<?>);
        var storage = storageHandle.get(owner);
        if (storage == null) {
            storageHandle.set(owner, element);
            return true;
        }
        else if (storage instanceof EconomicSet<?> set) {
            return ((EconomicSet<E>) set).add(element);
        }
        else if (element.equals(storage)) {
            return false;
        }
        else {
            EconomicSet<E> set = EconomicSet.create();
            set.add((E) storage);
            set.add(element);
            storageHandle.set(owner, set);
            return true;
        }
    }

    public void forEach(Consumer<? super E> action) {
        var storage = storageHandle.get(owner);
        if (storage == null) {
            return;
        }
        else if (storage instanceof EconomicSet<?> set) {
            ((EconomicSet<E>) set).forEach(action);
        }
        else {
            action.accept((E) storage);
        }
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof SmallSet<?> other)) {
            return false;
        }

        return owner == other.owner && storageHandle.equals(other.storageHandle);
    }

    @Override public int hashCode() {
        return 31 * identityHashCode(owner) + storageHandle.hashCode();
    }
}
