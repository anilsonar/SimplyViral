package com.simplyviral.queue.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.handler.StepHandler;
import com.simplyviral.orchestration.repository.StepRunRepository;
import com.simplyviral.orchestration.repository.StepRunUsageRepository;
import com.simplyviral.orchestration.service.Orchestrator;
import com.simplyviral.queue.entity.StepQueue;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import com.simplyviral.workflow.entity.StepDefinition;
import com.simplyviral.workflow.service.WorkflowDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Polls the DB queue for eligible tasks and dispatches them to the
 * appropriate StepHandler. Uses a thread pool for concurrent step execution.
 * Handles retry logic based on the step definition's retry policy.
 */
@Slf4j
@Service
public class StepWorker {

    private final QueueProvider queueProvider;
    private final Map<StepKey, StepHandler> handlerRegistry;
    private final StepRunRepository stepRunRepository;
    private final StepRunUsageRepository usageRepository;
    private final Orchestrator orchestrator;
    private final WorkflowDefinitionService workflowService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate txTemplate;
    private final String workerId = UUID.randomUUID().toString().substring(0, 8);
    private final ExecutorService taskPool;

    public StepWorker(QueueProvider queueProvider,
                      List<StepHandler> handlers,
                      StepRunRepository stepRunRepository,
                      StepRunUsageRepository usageRepository,
                      Orchestrator orchestrator,
                      WorkflowDefinitionService workflowService,
                      ObjectMapper objectMapper,
                      PlatformTransactionManager txManager,
                      @Value("${simplyviral.queue.worker-pool-size:4}") int poolSize) {
        this.queueProvider = queueProvider;
        this.handlerRegistry = handlers.stream()
                .collect(Collectors.toMap(StepHandler::getSupportedStep, Function.identity()));
        this.stepRunRepository = stepRunRepository;
        this.usageRepository = usageRepository;
        this.orchestrator = orchestrator;
        this.workflowService = workflowService;
        this.objectMapper = objectMapper;
        this.txTemplate = new TransactionTemplate(txManager);
        this.taskPool = Executors.newFixedThreadPool(poolSize);

        log.info("StepWorker initialized. Worker ID: {}, Pool Size: {}, Registered handlers: {}",
                workerId, poolSize,
                handlerRegistry.keySet().stream().map(Enum::name).collect(Collectors.joining(", ")));
    }

    @Scheduled(fixedDelayString = "${simplyviral.queue.poll-interval-ms:3000}")
    public void pollForWork() {
        Optional<StepQueue> claimed = txTemplate.execute(status ->
                queueProvider.claim(workerId));

        if (claimed != null && claimed.isPresent()) {
            StepQueue queueEntry = claimed.get();
            taskPool.submit(() -> processTask(queueEntry));
        }
    }

    private void processTask(StepQueue queueEntry) {
        StepKey stepKey = queueEntry.getStepKey();
        UUID jobId = queueEntry.getJob().getId();

        log.info("[WORKER-{}] Processing step {} for job {}", workerId, stepKey, jobId);

        // 1. Resolve the StepRun from DB
        StepRun stepRun = txTemplate.execute(status ->
                stepRunRepository.findByJobIdAndStepKey(jobId, stepKey)
                        .orElseThrow(() -> new IllegalStateException(
                                "No StepRun found for job=" + jobId + " step=" + stepKey)));

        if (stepRun == null) return;

        // 2. Resolve the handler
        StepHandler handler = handlerRegistry.get(stepKey);
        if (handler == null) {
            log.error("[WORKER-{}] No handler registered for step {}", workerId, stepKey);
            txTemplate.executeWithoutResult(status -> {
                stepRun.setStatus(StepStatus.FAILED);
                stepRun.setErrorCode("NO_HANDLER");
                stepRun.setErrorMessage("No handler registered for step: " + stepKey);
                stepRun.setFinishedAt(OffsetDateTime.now());
                stepRunRepository.save(stepRun);
                queueProvider.deadLetter(queueEntry.getId(), "No handler for step " + stepKey);
            });
            return;
        }

        // 3. Mark RUNNING
        txTemplate.executeWithoutResult(status -> {
            stepRun.setStatus(StepStatus.RUNNING);
            stepRun.setStartedAt(OffsetDateTime.now());
            stepRunRepository.save(stepRun);
        });

        // 4. Execute handler (outside transaction — may involve long HTTP calls)
        try {
            log.info("[WORKER-{}] Executing handler {} for step_run={} job={}",
                    workerId, handler.getClass().getSimpleName(), stepRun.getId(), jobId);

            handler.handle(stepRun);

            // 5. Mark COMPLETED (inside transaction)
            txTemplate.executeWithoutResult(status -> {
                stepRun.setStatus(StepStatus.COMPLETED);
                stepRun.setFinishedAt(OffsetDateTime.now());
                stepRunRepository.save(stepRun);
                queueProvider.acknowledge(queueEntry.getId());
            });

            log.info("[WORKER-{}] Step {} COMPLETED for job {}. latency={}ms cost={}",
                    workerId, stepKey, jobId, stepRun.getLatencyMs(), stepRun.getActualCost());

            // 6. Chain next steps via orchestrator
            orchestrator.handleStepCompletion(queueEntry.getJob(), stepKey);

        } catch (Exception e) {
            log.error("[WORKER-{}] Step {} FAILED for job {}: {}", workerId, stepKey, jobId, e.getMessage(), e);
            handleFailure(queueEntry, stepRun, e);
        }
    }

    private void handleFailure(StepQueue queueEntry, StepRun stepRun, Exception error) {
        txTemplate.executeWithoutResult(status -> {
            stepRun.setErrorCode(error.getClass().getSimpleName());
            stepRun.setErrorMessage(error.getMessage() != null ? error.getMessage().substring(0, Math.min(error.getMessage().length(), 500)) : "Unknown error");
            stepRun.setFinishedAt(OffsetDateTime.now());

            // Resolve retry policy from step definition
            RetryPolicy retry = resolveRetryPolicy(stepRun);

            if (stepRun.getAttemptNo() < retry.maxAttempts()) {
                // Retry: increment attempt, compute delay with backoff, requeue
                int nextAttempt = stepRun.getAttemptNo() + 1;
                long delay = (long) (retry.initialDelayMs() * Math.pow(retry.backoffMultiplier(), stepRun.getAttemptNo() - 1));
                delay = Math.min(delay, retry.maxDelayMs());

                stepRun.setAttemptNo(nextAttempt);
                stepRun.setStatus(StepStatus.RETRYING);
                stepRunRepository.save(stepRun);

                queueProvider.requeue(queueEntry.getId(), delay);

                log.warn("[RETRY] Step {} attempt {}/{} for job {}. Next retry in {}ms",
                        stepRun.getStepKey(), nextAttempt, retry.maxAttempts(),
                        queueEntry.getJob().getId(), delay);
            } else {
                // Exhausted retries: dead letter
                stepRun.setStatus(StepStatus.DEAD_LETTER);
                stepRunRepository.save(stepRun);

                queueProvider.deadLetter(queueEntry.getId(),
                        "Exhausted " + retry.maxAttempts() + " attempts: " + error.getMessage());

                log.error("[DEAD_LETTER] Step {} permanently failed for job {} after {} attempts",
                        stepRun.getStepKey(), queueEntry.getJob().getId(), retry.maxAttempts());

                orchestrator.handleStepFailure(queueEntry.getJob(), stepRun.getStepKey(),
                        stepRun.getErrorCode(), stepRun.getErrorMessage());
            }
        });
    }

    private RetryPolicy resolveRetryPolicy(StepRun stepRun) {
        try {
            var job = stepRun.getJob();
            List<StepDefinition> defs = workflowService.getStepsForWorkflow(
                    job.getWorkflowId(), job.getWorkflowVersion());

            return defs.stream()
                    .filter(d -> d.getStepKey().equals(stepRun.getStepKey().name()))
                    .findFirst()
                    .map(def -> parseRetryPolicy(def.getRetryPolicyRef()))
                    .orElse(new RetryPolicy(3, 2000, 2.0, 30000));

        } catch (Exception e) {
            log.warn("Failed to resolve retry policy, using defaults", e);
            return new RetryPolicy(3, 2000, 2.0, 30000);
        }
    }

    private RetryPolicy parseRetryPolicy(String retryJson) {
        try {
            if (retryJson == null) return new RetryPolicy(3, 2000, 2.0, 30000);
            JsonNode node = objectMapper.readTree(retryJson);
            return new RetryPolicy(
                    node.path("max_attempts").asInt(3),
                    node.path("initial_delay_ms").asLong(2000),
                    node.path("backoff_multiplier").asDouble(2.0),
                    node.path("max_delay_ms").asLong(30000)
            );
        } catch (Exception e) {
            return new RetryPolicy(3, 2000, 2.0, 30000);
        }
    }

    private record RetryPolicy(int maxAttempts, long initialDelayMs, double backoffMultiplier, long maxDelayMs) {}
}
