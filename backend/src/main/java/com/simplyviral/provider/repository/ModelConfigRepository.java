package com.simplyviral.provider.repository;

import com.simplyviral.provider.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, String> {
    List<ModelConfig> findByProviderKeyAndActiveFlagTrue(String providerKey);
}
