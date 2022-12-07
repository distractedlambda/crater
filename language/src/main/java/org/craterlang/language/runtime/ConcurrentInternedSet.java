package org.craterlang.language.runtime;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class ConcurrentInternedSet<E> {
    private final WeakHashMap<E, WeakReference<E>> globalMap = new WeakHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ThreadLocal<WeakHashMap<E, WeakReference<E>>> localMaps = ThreadLocal.withInitial(WeakHashMap::new);

    @TruffleBoundary
    public E intern(E candidate) {
        var localMap = localMaps.get();
        var valueRef = localMap.get(candidate);

        if (valueRef != null) {
            var value = valueRef.get();
            if (value != null) {
                return value;
            }
        }

        lock.readLock().lock();
        try {
            valueRef = globalMap.get(candidate);
        }
        finally {
            lock.readLock().unlock();
        }

        if (valueRef != null) {
            var value = valueRef.get();
            if (value != null) {
                localMap.put(value, valueRef);
                return value;
            }
        }

        E value;

        lock.writeLock().lock();
        insertIntoGlobal: try {
            var candidateRef = new WeakReference<>(candidate);
            valueRef = globalMap.putIfAbsent(candidate, candidateRef);

            if (valueRef != null) {
                value = valueRef.get();
                if (value != null) {
                    break insertIntoGlobal;
                }
            }

            value = candidate;
            valueRef = candidateRef;
        }
        finally {
            lock.writeLock().unlock();
        }

        localMap.put(value, valueRef);
        return value;
    }
}
