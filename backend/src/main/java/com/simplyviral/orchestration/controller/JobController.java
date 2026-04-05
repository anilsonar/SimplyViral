package com.simplyviral.orchestration.controller;

import com.simplyviral.identity.entity.User;
import com.simplyviral.identity.service.AuthService;
import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.service.JobService;
import com.simplyviral.shared.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final AuthService authService;

    /**
     * Dispatches a new content generation job for the authenticated user.
     * POST /api/v1/jobs/generate?planType=FREE
     */
    @PostMapping("/generate")
    public ApiResult<Map<String, Object>> createGenerationJob(
            @RequestParam String planType,
            Authentication authentication) {
        log.info("Received request to create generation job for plan: {}", planType);

        User user = null;
        if (authentication != null && authentication.getPrincipal() != null) {
            String subject = authentication.getPrincipal().toString();
            try {
                UUID userId = UUID.fromString(subject);
                user = authService.getUserById(userId);
            } catch (IllegalArgumentException e) {
                try {
                    user = authService.getUserByEmail(subject);
                } catch (Exception ignored) {}
            }
        }

        Job job;
        if (user != null) {
            job = jobService.createGenerationJob(user, planType);
        } else {
            job = jobService.createGenerationJobAnonymous(planType);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("job_id", job.getId().toString());
        response.put("status", job.getStatus().name());
        response.put("workflow_id", job.getWorkflowId());
        response.put("workflow_version", job.getWorkflowVersion());
        response.put("plan_type", job.getPlanType());

        log.info("Job {} created and orchestration initialized", job.getId());
        return ApiResult.success(response);
    }

    /**
     * Returns the current status of a job.
     * GET /api/v1/jobs/{jobId}/status
     */
    @GetMapping("/{jobId}/status")
    public ApiResult<Map<String, Object>> getJobStatus(@PathVariable UUID jobId) {
        Job job = jobService.getJob(jobId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("job_id", job.getId().toString());
        response.put("status", job.getStatus().name());
        response.put("started_at", job.getStartedAt());
        response.put("completed_at", job.getCompletedAt());

        return ApiResult.success(response);
    }

    /**
     * Returns all step runs for a job with full observability data.
     * GET /api/v1/jobs/{jobId}/steps
     */
    @GetMapping("/{jobId}/steps")
    public ApiResult<List<Map<String, Object>>> getJobStepRuns(@PathVariable UUID jobId) {
        List<StepRun> steps = jobService.getJobStepRuns(jobId);

        List<Map<String, Object>> stepData = steps.stream().map(step -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("step_run_id", step.getId().toString());
            map.put("step_key", step.getStepKey().name());
            map.put("status", step.getStatus().name());
            map.put("attempt_no", step.getAttemptNo());
            map.put("provider_ref", step.getProviderRef());
            map.put("model_config_ref", step.getModelConfigRef());
            map.put("latency_ms", step.getLatencyMs());
            map.put("planned_cost", step.getPlannedCost());
            map.put("actual_cost", step.getActualCost());
            map.put("started_at", step.getStartedAt());
            map.put("finished_at", step.getFinishedAt());
            map.put("error_code", step.getErrorCode());
            map.put("error_message", step.getErrorMessage());
            return map;
        }).collect(Collectors.toList());

        return ApiResult.success(stepData);
    }
}
