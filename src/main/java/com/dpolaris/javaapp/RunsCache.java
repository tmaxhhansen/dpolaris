package com.dpolaris.javaapp;

import java.util.HashMap;
import java.util.Map;

final class RunsCache {
    private final long ttlMillis;
    private final Map<String, Entry> entries = new HashMap<>();

    RunsCache(long ttlMillis) {
        this.ttlMillis = Math.max(1_000L, ttlMillis);
    }

    synchronized <T> T get(String key, boolean forceRefresh, Loader<T> loader) throws Exception {
        long now = System.currentTimeMillis();
        if (!forceRefresh) {
            Entry existing = entries.get(key);
            if (existing != null && existing.expiresAt > now) {
                @SuppressWarnings("unchecked")
                T value = (T) existing.value;
                return value;
            }
        }

        T loaded = loader.load();
        entries.put(key, new Entry(loaded, now + ttlMillis));
        return loaded;
    }

    synchronized void invalidateAll() {
        entries.clear();
    }

    private record Entry(Object value, long expiresAt) {
    }

    @FunctionalInterface
    interface Loader<T> {
        T load() throws Exception;
    }
}
