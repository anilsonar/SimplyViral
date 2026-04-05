package com.simplyviral.lock.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL advisory lock-based distributed lock provider.
 * Uses pg_try_advisory_lock/pg_advisory_unlock for lightweight, non-blocking locks.
 * Lock names are hashed to int values for PostgreSQL advisory lock IDs.
 */
@Slf4j
@RequiredArgsConstructor
public class DbLockProvider implements LockProvider {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryLock(String lockName, long ttlSeconds) {
        try {
            int lockId = lockName.hashCode();
            Boolean acquired = jdbcTemplate.queryForObject(
                    "SELECT pg_try_advisory_lock(?)", Boolean.class, lockId);

            boolean result = Boolean.TRUE.equals(acquired);
            if (result) {
                log.debug("[LOCK:DB] Acquired lock: {} (id={})", lockName, lockId);
            } else {
                log.debug("[LOCK:DB] Failed to acquire lock: {} (id={})", lockName, lockId);
            }
            return result;
        } catch (Exception e) {
            log.error("[LOCK:DB] Error acquiring lock: {}", lockName, e);
            return false;
        }
    }

    @Override
    public void unlock(String lockName) {
        try {
            int lockId = lockName.hashCode();
            jdbcTemplate.queryForObject(
                    "SELECT pg_advisory_unlock(?)", Boolean.class, lockId);
            log.debug("[LOCK:DB] Released lock: {} (id={})", lockName, lockId);
        } catch (Exception e) {
            log.error("[LOCK:DB] Error releasing lock: {}", lockName, e);
        }
    }

    @Override
    public boolean isLocked(String lockName) {
        // PostgreSQL doesn't have a direct "is this advisory lock held" check
        // We try to lock and immediately unlock if successful
        try {
            int lockId = lockName.hashCode();
            Boolean acquired = jdbcTemplate.queryForObject(
                    "SELECT pg_try_advisory_lock(?)", Boolean.class, lockId);
            if (Boolean.TRUE.equals(acquired)) {
                // We got the lock, meaning it wasn't held — release immediately
                jdbcTemplate.queryForObject(
                        "SELECT pg_advisory_unlock(?)", Boolean.class, lockId);
                return false;
            }
            return true; // Lock is held by someone else
        } catch (Exception e) {
            log.error("[LOCK:DB] Error checking lock status: {}", lockName, e);
            return false;
        }
    }
}
