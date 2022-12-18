package org.craterlang.language.util;

import com.oracle.truffle.api.CompilerDirectives.ValueType;
import org.graalvm.collections.EconomicMap;

import java.lang.invoke.VarHandle;
import java.lang.ref.WeakReference;
import java.util.function.Function;

import static java.lang.System.identityHashCode;
import static java.util.Objects.requireNonNull;

@ValueType
@SuppressWarnings("unchecked")
public final class SmallCache<K, V> {
    private final Object owner;
    private final VarHandle storageHandle;

    public SmallCache(Object owner, VarHandle storageHandle) {
        this.owner = requireNonNull(owner);
        this.storageHandle = requireNonNull(storageHandle);
    }

    public V getOrCompute(K key, Function<K, V> computeValue) {
        var storage = storageHandle.get(owner);
        if (storage == null) {
            var value = computeValue.apply(key);
            storageHandle.set(owner, new SingleEntry(key, value));
            return value;
        }
        else if (storage instanceof SingleEntry entry) {
            var existingValue = entry.get();
            if (existingValue == null) {
                var value = computeValue.apply(key);
                storageHandle.set(owner, new SingleEntry(key, value));
                return value;
            }
            else if (key.equals(entry.key)) {
                return (V) existingValue;
            }
            else {
                var value = computeValue.apply(key);
                EconomicMap<K, WeakReference<V>> map = EconomicMap.create();
                map.put((K) entry.key, new WeakReference<>((V) existingValue));
                map.put(key, new WeakReference<>(value));
                storageHandle.set(owner, map);
                return value;
            }
        }
        else {
            var map = (EconomicMap<K, WeakReference<V>>) storage;

            var existingReference = map.get(key);
            if (existingReference != null) {
                var existingValue = existingReference.get();
                if (existingValue != null) {
                    return existingValue;
                }
            }

            var value = computeValue.apply(key);
            map.put(key, new WeakReference<>(value));
            return value;
        }
    }

    @Override public boolean equals(Object obj) {
        if (!(obj instanceof SmallCache<?,?> other)) {
            return false;
        }

        return owner == other.owner && storageHandle.equals(other.storageHandle);
    }

    @Override public int hashCode() {
        return 31 * identityHashCode(owner) + storageHandle.hashCode();
    }

    private static final class SingleEntry extends WeakReference<Object> {
        private final Object key;

        private SingleEntry(Object key, Object value) {
            super(value);
            this.key = key;
        }
    }
}
