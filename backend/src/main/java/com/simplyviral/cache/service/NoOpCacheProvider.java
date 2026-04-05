package com.simplyviral.cache.service;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory no-op cache provider for local dev and environments
 * where Redis is unavailable. Uses a simple ConcurrentHashMap with TTL.
 * This is NOT suitable for multi-instance production deployments.
 */
@Slf4j
public class NoOpCacheProvider implements CacheProvider {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value, long ttlSeconds) {
        log.debug("[CACHE:NOOP] PUT key={} ttl={}s", key, ttlSeconds);
        long expiresAt = ttlSeconds > 0
                ? Instant.now().plusSeconds(ttlSeconds).toEpochMilli()
                : Long.MAX_VALUE;
        cache.put(key, new CacheEntry(value, expiresAt));
    }

    @Override
    public String get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            log.debug("[CACHE:NOOP] MISS key={}", key);
            return null;
        }
        if (Instant.now().toEpochMilli() > entry.expiresAt()) {
            log.debug("[CACHE:NOOP] EXPIRED key={}", key);
            cache.remove(key);
            return null;
        }
        log.debug("[CACHE:NOOP] HIT key={}", key);
        return entry.value();
    }

    @Override
    public void evict(String key) {
        log.debug("[CACHE:NOOP] EVICT key={}", key);
        cache.remove(key);
    }

    @Override
    public boolean exists(String key) {
        String val = get(key); // side-effect: cleans expired entries
        return val != null;
    }

    private record CacheEntry(String value, long expiresAt) {}
}
