package com.simplyviral.cache.configuration;

import com.simplyviral.cache.service.CacheProvider;
import com.simplyviral.cache.service.NoOpCacheProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnMissingBean(CacheProvider.class)
    @ConditionalOnProperty(name = "simplyviral.cache.mode", havingValue = "NONE", matchIfMissing = true)
    public CacheProvider noOpCacheProvider() {
        log.info("Initializing CacheProvider with NoOp mode (in-memory, single-instance only).");
        return new NoOpCacheProvider();
    }

    // Future: Redis-backed cache provider
    // @Bean
    // @ConditionalOnProperty(name = "simplyviral.cache.mode", havingValue = "REDIS")
    // public CacheProvider redisCacheProvider(RedisTemplate<String, String> redisTemplate) { ... }
}
