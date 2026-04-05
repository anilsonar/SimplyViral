package com.simplyviral.lock.configuration;

import com.simplyviral.lock.service.DbLockProvider;
import com.simplyviral.lock.service.LockProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
public class LockConfig {

    @Bean
    @ConditionalOnMissingBean(LockProvider.class)
    @ConditionalOnProperty(name = "simplyviral.lock.mode", havingValue = "DB", matchIfMissing = true)
    public LockProvider dbLockProvider(JdbcTemplate jdbcTemplate) {
        log.info("Initializing LockProvider with DB mode (PostgreSQL advisory locks).");
        return new DbLockProvider(jdbcTemplate);
    }

    // Future: Redis-backed distributed lock provider
    // @Bean
    // @ConditionalOnProperty(name = "simplyviral.lock.mode", havingValue = "REDIS")
    // public LockProvider redisLockProvider(RedissonClient redisson) { ... }
}
