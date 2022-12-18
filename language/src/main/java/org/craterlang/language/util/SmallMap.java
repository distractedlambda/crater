package org.craterlang.language.util;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import org.graalvm.collections.EconomicMap;

import java.lang.invoke.VarHandle;
import java.util.function.BiConsumer;

import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;

@ValueType
@SuppressWarnings("unchecked")
public final class SmallMap<K, V> {
    private final Object owner;
    private final VarHandle storageHandle;

    public SmallMap(Object owner, VarHandle storageHandle) {
        this.owner = requireNonNull(owner);
        this.storageHandle = requireNonNull(storageHandle);
    }

    public V get(K key) {
        var storage = storageHandle.get(owner);
        if (storage == null) {
            return null;
        }
        else if (storage instanceof SingleEntry entry) {
            if (key.equals(entry.key)) {
                return (V) entry.value;
            }
            else {
                return null;
            }
        }
        else {
            return ((EconomicMap<K, V>) storage).get(key);
        }
    }

    public V put(K key, V value) {
        var storage = storageHandle.get(owner);
        if (storage == null) {
            storageHandle.set(owner, new SingleEntry(key, value));
            return null;
        }
        else if (storage instanceof SingleEntry entry) {
            if (key.equals(entry.key)) {
                var oldValue = entry.value;
                entry.value = value;
                return (V) oldValue;
            }
            else {
                EconomicMap<K, V> map = EconomicMap.create();
                map.put((K) entry.key, (V) entry.value);
                map.put(key, value);
                storageHandle.set(owner, map);
                return null;
            }
        }
        else {
            return ((EconomicMap<K, V>) storage).put(key, value);
        }
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        var storage = storageHandle.get(owner);
        if (storage == null) {
            return;
        }
        else if (storage instanceof SingleEntry entry) {
            action.accept((K) entry.key, (V) entry.value);
        }
        else {
            for (var cursor = ((EconomicMap<K, V>) storage).getEntries(); cursor.advance();) {
                action.accept(cursor.getKey(), cursor.getValue());
            }
        }
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof SmallMap<?,?> other)) {
            return false;
        }

        return owner == other.owner && storageHandle.equals(other.storageHandle);
    }

    @Override public int hashCode() {
        return 31 * identityHashCode(owner) + storageHandle.hashCode();
    }

    private static final class SingleEntry {
        private final Object key;
        private Object value;

        private SingleEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
