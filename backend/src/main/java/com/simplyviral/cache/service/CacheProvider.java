package com.simplyviral.cache.service;

/**
 * Abstraction for caching infrastructure.
 * Implementations: NoOpCacheProvider (default), RedisCacheProvider (future).
 */
public interface CacheProvider {

    /**
     * Stores a value in cache with optional TTL in seconds.
     */
    void put(String key, String value, long ttlSeconds);

    /**
     * Retrieves a value from cache. Returns null if not found or expired.
     */
    String get(String key);

    /**
     * Removes a specific key from cache.
     */
    void evict(String key);

    /**
     * Checks presence of a key in cache.
     */
    boolean exists(String key);
}
