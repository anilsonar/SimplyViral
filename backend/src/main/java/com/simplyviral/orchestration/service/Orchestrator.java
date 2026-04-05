package com.simplyviral.orchestration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.repository.JobRepository;
import com.simplyviral.orchestration.repository.StepRunRepository;
import com.simplyviral.shared.constant.JobStatus;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import com.simplyviral.workflow.entity.StepDefinition;
import com.simplyviral.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Central orchestration engine: initializes the step DAG for a job,
 * resolves dependency completions, and schedules unblocked steps.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Orchestrator {

    private final StepScheduler stepScheduler;
    private final WorkflowDefinitionService workflowService;
    private final StepRunRepository stepRunRepository;
    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    /**
     * Creates StepRun records for every step in the workflow, then
     * schedules root steps (those with no dependencies).
     */
    @Transactional
    public void initializeJobOrchestration(Job job) {
        log.info("Initializing orchestration for Job {} (workflow={}, v={})",
                job.getId(), job.getWorkflowId(), job.getWorkflowVersion());

        job.setStatus(JobStatus.RUNNING);
        jobRepository.save(job);

        List<StepDefinition> stepDefs = workflowService.getStepsForWorkflow(
                job.getWorkflowId(), job.getWorkflowVersion());

        if (stepDefs.isEmpty()) {
            log.error("No step definitions found for workflow {} v{}",
                    job.getWorkflowId(), job.getWorkflowVersion());
            job.setStatus(JobStatus.FAILED);
            jobRepository.save(job);
            return;
        }

        // Create a StepRun for each step definition in PENDING state
        List<StepRun> rootSteps = new ArrayList<>();
        for (StepDefinition def : stepDefs) {
            String providerKey = extractJsonField(def.getProviderPolicyRef(), "provider_key");
            String modelConfigKey = extractJsonField(def.getProviderPolicyRef(), "model_config_key");

            StepRun run = StepRun.builder()
                    .job(job)
                    .stepKey(StepKey.valueOf(def.getStepKey()))
                    .status(StepStatus.PENDING)
                    .attemptNo(1)
                    .providerRef(providerKey)
                    .modelConfigRef(modelConfigKey)
                    .build();
            stepRunRepository.save(run);

            List<String> dependencies = extractDependencies(def.getDependencyRule());
            if (dependencies.isEmpty()) {
                rootSteps.add(run);
            }
        }

        log.info("Created {} step runs for Job {}. Root steps: {}",
                stepDefs.size(), job.getId(),
                rootSteps.stream().map(r -> r.getStepKey().name()).toList());

        // Schedule root steps (no dependencies)
        for (StepRun root : rootSteps) {
            stepScheduler.scheduleStep(root);
        }
    }

    /**
     * Called when a step completes. Evaluates which downstream steps are now unblocked.
     * If all steps are done, marks the job as RENDERED.
     */
    @Transactional
    public void handleStepCompletion(Job job, StepKey completedStep) {
        log.info("Evaluating next steps for Job {} after completion of {}", job.getId(), completedStep);

        List<StepDefinition> allDefs = workflowService.getStepsForWorkflow(
                job.getWorkflowId(), job.getWorkflowVersion());
        List<StepRun> allRuns = stepRunRepository.findByJobId(job.getId());

        // Find steps that depend on the completed step
        for (StepDefinition def : allDefs) {
            List<String> deps = extractDependencies(def.getDependencyRule());
            if (!deps.contains(completedStep.name())) continue;

            StepKey candidateKey = StepKey.valueOf(def.getStepKey());

            // Check if ALL dependencies of this candidate are COMPLETED
            boolean allDepsComplete = deps.stream().allMatch(depName -> {
                StepKey depKey = StepKey.valueOf(depName);
                return allRuns.stream()
                        .filter(r -> r.getStepKey() == depKey)
                        .anyMatch(r -> r.getStatus() == StepStatus.COMPLETED);
            });

            if (!allDepsComplete) {
                log.debug("Step {} still has pending dependencies", candidateKey);
                continue;
            }

            // Find the PENDING StepRun for this candidate and schedule it
            allRuns.stream()
                    .filter(r -> r.getStepKey() == candidateKey && r.getStatus() == StepStatus.PENDING)
                    .findFirst()
                    .ifPresent(run -> {
                        log.info("All dependencies met for step {}. Scheduling.", candidateKey);
                        stepScheduler.scheduleStep(run);
                    });
        }

        // Check if entire job is done
        long totalSteps = allRuns.size();
        long completedSteps = allRuns.stream()
                .filter(r -> r.getStatus() == StepStatus.COMPLETED || r.getStatus() == StepStatus.SKIPPED)
                .count();

        if (completedSteps == totalSteps) {
            log.info("All {} steps completed for Job {}. Marking RENDERED.", totalSteps, job.getId());
            job.setStatus(JobStatus.RENDERED);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }

        // Check for fatal failures
        long failedSteps = allRuns.stream()
                .filter(r -> r.getStatus() == StepStatus.DEAD_LETTER)
                .count();
        if (failedSteps > 0) {
            log.error("Job {} has {} dead-lettered steps. Marking FAILED.", job.getId(), failedSteps);
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }
    }

    /**
     * Called when a step fails permanently (exhausted retries).
     */
    @Transactional
    public void handleStepFailure(Job job, StepKey failedStep, String errorCode, String errorMessage) {
        log.error("Step {} permanently failed for Job {}: {} - {}",
                failedStep, job.getId(), errorCode, errorMessage);
        job.setStatus(JobStatus.FAILED);
        job.setCompletedAt(OffsetDateTime.now());
        jobRepository.save(job);
    }

    private List<String> extractDependencies(String dependencyRuleJson) {
        try {
            if (dependencyRuleJson == null) return List.of();
            JsonNode root = objectMapper.readTree(dependencyRuleJson);
            JsonNode depsNode = root.path("depends_on");
            if (depsNode.isMissingNode() || !depsNode.isArray()) return List.of();

            List<String> deps = new ArrayList<>();
            depsNode.forEach(n -> deps.add(n.asText()));
            return deps;
        } catch (Exception e) {
            log.error("Failed to parse dependency_rule: {}", dependencyRuleJson, e);
            return List.of();
        }
    }

    private String extractJsonField(String json, String field) {
        try {
            if (json == null) return null;
            JsonNode node = objectMapper.readTree(json).path(field);
            return node.isMissingNode() || node.isNull() ? null : node.asText();
        } catch (Exception e) {
            log.error("Failed to parse JSON field '{}' from: {}", field, json, e);
            return null;
        }
    }
}
