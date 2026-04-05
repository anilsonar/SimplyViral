package com.simplyviral.lock.service;

/**
 * Distributed lock abstraction for coordinating concurrent access
 * across workers. Implementations: DbLockProvider (default), RedisLockProvider (future).
 */
public interface LockProvider {

    /**
     * Attempts to acquire a named lock. Returns true if the lock was acquired.
     * @param lockName Unique name identifying the resource to lock
     * @param ttlSeconds How long the lock should be held (auto-release safety net)
     * @return true if lock was successfully acquired
     */
    boolean tryLock(String lockName, long ttlSeconds);

    /**
     * Releases a previously acquired lock.
     * @param lockName The lock name to release
     */
    void unlock(String lockName);

    /**
     * Checks if a lock is currently held (informational, not for concurrency decisions).
     */
    boolean isLocked(String lockName);
}
