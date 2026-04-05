package com.simplyviral.admin.controller;

import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.repository.JobRepository;
import com.simplyviral.shared.dto.ApiResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin-only controller for system monitoring and management.
 * In production, this should be protected by an ADMIN role check.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JobRepository jobRepository;

    @GetMapping("/health")
    public ApiResult<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("service", "simply-viral-backend");
        health.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ApiResult.success(health);
    }

    @GetMapping("/jobs/recent")
    public ApiResult<List<Map<String, Object>>> recentJobs(@RequestParam(defaultValue = "20") int limit) {
        List<Job> jobs = jobRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 100),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startedAt")))
                .getContent();

        List<Map<String, Object>> result = jobs.stream().map(job -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("job_id", job.getId());
            m.put("workflow_id", job.getWorkflowId());
            m.put("status", job.getStatus());
            m.put("plan_type", job.getPlanType());
            m.put("started_at", job.getStartedAt());
            m.put("completed_at", job.getCompletedAt());
            return m;
        }).collect(Collectors.toList());

        return ApiResult.success(result);
    }
}
