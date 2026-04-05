package com.simplyviral.asset.configuration;

import com.simplyviral.asset.service.AssetStorageService;
import com.simplyviral.asset.service.LocalAssetStorageImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "simplyviral.storage.mode", havingValue = "LOCAL", matchIfMissing = true)
    public AssetStorageService localAssetStorage() {
        log.info("Injecting LocalAssetStorage service based on configuration.");
        return new LocalAssetStorageImpl();
    }
    
    // Future:
    // @Bean
    // @ConditionalOnProperty(name = "simplyviral.storage.mode", havingValue = "S3")
    // public AssetStorageService s3AssetStorage() { ... }
}
