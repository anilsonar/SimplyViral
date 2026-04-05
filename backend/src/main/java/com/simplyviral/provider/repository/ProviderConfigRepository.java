package com.simplyviral.provider.repository;

import com.simplyviral.provider.entity.ProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderConfigRepository extends JpaRepository<ProviderConfig, String> {
    List<ProviderConfig> findByEnabledFlagTrue();
}
