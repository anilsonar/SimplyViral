package com.simplyviral.provider.service;

import com.simplyviral.provider.entity.ModelConfig;
import com.simplyviral.provider.entity.ProviderConfig;
import com.simplyviral.provider.repository.ModelConfigRepository;
import com.simplyviral.provider.repository.ProviderConfigRepository;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves provider endpoints and model names from DB-backed configuration.
 * Eliminates hardcoded model names and API paths in business code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderConfigService {

    private final ProviderConfigRepository providerConfigRepository;
    private final ModelConfigRepository modelConfigRepository;

    @Transactional(readOnly = true)
    public String resolveModelName(String modelConfigKey) {
        if (modelConfigKey == null) return null;
        return modelConfigRepository.findById(modelConfigKey)
                .filter(ModelConfig::getActiveFlag)
                .map(ModelConfig::getExternalModelName)
                .orElseThrow(() -> new SimplyViralException(
                        "No active model config for key: " + modelConfigKey));
    }

    @Transactional(readOnly = true)
    public ModelConfig getModelConfig(String modelConfigKey) {
        return modelConfigRepository.findById(modelConfigKey)
                .orElseThrow(() -> new SimplyViralException(
                        "Model config not found: " + modelConfigKey));
    }

    @Transactional(readOnly = true)
    public ProviderConfig getProviderConfig(String providerKey) {
        return providerConfigRepository.findById(providerKey)
                .filter(ProviderConfig::getEnabledFlag)
                .orElseThrow(() -> new SimplyViralException(
                        "No enabled provider for key: " + providerKey));
    }

    @Transactional(readOnly = true)
    public String resolveEndpoint(String providerKey) {
        return getProviderConfig(providerKey).getEndpointRef();
    }
}
