package com.simplyviral.provider.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.entity.StepRunUsage;
import com.simplyviral.orchestration.repository.StepRunUsageRepository;
import com.simplyviral.provider.client.ProviderClient;
import com.simplyviral.provider.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wraps every external provider call with structured observability:
 * latency tracking, usage extraction, cost computation, and persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeteredProviderExecutor {

    private final StepRunUsageRepository usageRepository;
    private final PricingService pricingService;
    private final ObjectMapper objectMapper;

    public <T, R> R executeMetered(ProviderClient<T, R> client, T request, StepRun stepContext) {
        log.info("[METERED] provider={} step_key={} step_run_id={} job_id={} attempt={} model_config={}",
                client.getProviderKey(),
                stepContext.getStepKey(),
                stepContext.getId(),
                stepContext.getJob() != null ? stepContext.getJob().getId() : "N/A",
                stepContext.getAttemptNo(),
                stepContext.getModelConfigRef());

        long start = Instant.now().toEpochMilli();
        try {
            R response = client.execute(request, stepContext);
            long latency = Instant.now().toEpochMilli() - start;
            stepContext.setLatencyMs(latency);

            log.info("[METERED] SUCCESS provider={} step_run={} latency_ms={}",
                    client.getProviderKey(), stepContext.getId(), latency);

            // Persist usage if the adapter populated it on the StepRun context
            persistUsageAndCost(stepContext);

            return response;

        } catch (Exception ex) {
            long latency = Instant.now().toEpochMilli() - start;
            stepContext.setLatencyMs(latency);

            log.error("[METERED] FAILURE provider={} step_run={} latency_ms={} error={}",
                    client.getProviderKey(), stepContext.getId(), latency, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Reads transient StepRunUsage from the StepRun (populated by the adapter),
     * persists it, and computes actual cost via PricingService.
     */
    private void persistUsageAndCost(StepRun stepContext) {
        StepRunUsage usage = stepContext.getUsage();
        if (usage == null) return;

        try {
            // Ensure proper identity linkage
            usage.setId(stepContext.getId());
            usage.setStepRun(stepContext);
            usageRepository.save(usage);

            log.info("[USAGE] step_run={} prompt_tokens={} completion_tokens={} total_tokens={} " +
                            "image_count={} audio_seconds={} video_seconds={}",
                    stepContext.getId(),
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens(),
                    usage.getImageCount(),
                    usage.getAudioSeconds(),
                    usage.getVideoSeconds());

            // Compute actual cost from usage
            if (stepContext.getProviderRef() != null && stepContext.getModelConfigRef() != null
                    && usage.getRawUsageJson() != null) {
                BigDecimal actualCost = pricingService.computeActualCost(
                        stepContext.getProviderRef(),
                        stepContext.getModelConfigRef(),
                        usage.getRawUsageJson());
                stepContext.setActualCost(actualCost);

                log.info("[COST] step_run={} provider={} model={} actual_cost={}",
                        stepContext.getId(), stepContext.getProviderRef(),
                        stepContext.getModelConfigRef(), actualCost);
            }
        } catch (Exception e) {
            // Usage persistence failure should not break the step execution
            log.error("[USAGE] Failed to persist usage for step_run={}", stepContext.getId(), e);
        }
    }
}
