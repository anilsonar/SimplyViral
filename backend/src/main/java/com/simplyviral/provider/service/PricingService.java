package com.simplyviral.provider.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.provider.entity.PricingCatalog;
import com.simplyviral.provider.repository.PricingCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes planned and actual costs using rates from the pricing_catalog table.
 * Rates are stored as JSON and parsed per unit type.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final PricingCatalogRepository pricingRepo;
    private final ObjectMapper objectMapper;

    /**
     * Estimate cost before execution based on typical usage for this model.
     */
    public BigDecimal computePlannedCost(String providerKey, String modelConfigKey, int estimatedUnits) {
        log.debug("Computing planned cost for provider={}, model={}, units={}", providerKey, modelConfigKey, estimatedUnits);
        return pricingRepo.findActiveRate(providerKey, modelConfigKey)
                .map(pc -> computeFromRate(pc, estimatedUnits))
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Compute actual cost from real usage metrics returned by the provider.
     * Parses the raw usage JSON and multiplies by the applicable rate.
     */
    public BigDecimal computeActualCost(String providerKey, String modelConfigKey, String usageJson) {
        log.debug("Computing actual cost for provider={}, model={}", providerKey, modelConfigKey);
        try {
            PricingCatalog catalog = pricingRepo.findActiveRate(providerKey, modelConfigKey).orElse(null);
            if (catalog == null || usageJson == null) return BigDecimal.ZERO;

            JsonNode rates = objectMapper.readTree(catalog.getRatesJson());
            JsonNode usage = objectMapper.readTree(usageJson);

            return switch (catalog.getUnitType()) {
                case "tokens" -> computeTokenCost(rates, usage);
                case "characters" -> computeCharacterCost(rates, usage);
                case "images" -> computeImageCost(rates, usage);
                case "renders" -> computeRenderCost(rates);
                default -> {
                    log.warn("Unknown unit type '{}', returning zero cost", catalog.getUnitType());
                    yield BigDecimal.ZERO;
                }
            };
        } catch (Exception e) {
            log.error("Failed to compute actual cost", e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal computeTokenCost(JsonNode rates, JsonNode usage) {
        double inputRate = rates.path("input_per_1k").asDouble(0);
        double outputRate = rates.path("output_per_1k").asDouble(0);
        int promptTokens = usage.path("prompt_tokens").asInt(0);
        int completionTokens = usage.path("completion_tokens").asInt(0);

        BigDecimal inputCost = BigDecimal.valueOf(promptTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(inputRate));
        BigDecimal outputCost = BigDecimal.valueOf(completionTokens)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(outputRate));

        return inputCost.add(outputCost).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeCharacterCost(JsonNode rates, JsonNode usage) {
        double perKChars = rates.path("per_1k_chars").asDouble(0);
        int charCount = usage.path("character_count").asInt(0);
        return BigDecimal.valueOf(charCount)
                .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(perKChars))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeImageCost(JsonNode rates, JsonNode usage) {
        double perImage = rates.path("per_image").asDouble(0);
        int count = usage.path("image_count").asInt(1);
        return BigDecimal.valueOf(count).multiply(BigDecimal.valueOf(perImage))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeRenderCost(JsonNode rates) {
        double perRender = rates.path("per_render").asDouble(0);
        return BigDecimal.valueOf(perRender).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeFromRate(PricingCatalog catalog, int units) {
        try {
            JsonNode rates = objectMapper.readTree(catalog.getRatesJson());
            return switch (catalog.getUnitType()) {
                case "tokens" -> BigDecimal.valueOf(units)
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(rates.path("input_per_1k").asDouble(0)))
                        .setScale(4, RoundingMode.HALF_UP);
                case "characters" -> BigDecimal.valueOf(units)
                        .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(rates.path("per_1k_chars").asDouble(0)))
                        .setScale(4, RoundingMode.HALF_UP);
                case "images" -> BigDecimal.valueOf(units)
                        .multiply(BigDecimal.valueOf(rates.path("per_image").asDouble(0)))
                        .setScale(4, RoundingMode.HALF_UP);
                case "renders" -> BigDecimal.valueOf(rates.path("per_render").asDouble(0))
                        .setScale(4, RoundingMode.HALF_UP);
                default -> BigDecimal.ZERO;
            };
        } catch (Exception e) {
            log.error("Error computing planned cost from rate", e);
            return BigDecimal.ZERO;
        }
    }
}
