package com.simplyviral.orchestration.service;

import com.simplyviral.identity.entity.User;
import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.repository.JobRepository;
import com.simplyviral.orchestration.repository.StepRunRepository;
import com.simplyviral.shared.constant.JobStatus;
import com.simplyviral.shared.exception.SimplyViralException;
import com.simplyviral.workflow.entity.WorkflowDefinition;
import com.simplyviral.workflow.service.WorkflowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final StepRunRepository stepRunRepository;
    private final WorkflowDefinitionService workflowService;
    private final Orchestrator orchestrator;

    @Transactional
    public Job createGenerationJob(User user, String planType) {
        log.info("Creating generation job for user {} under plan {}", user.getId(), planType);

        WorkflowDefinition activeWorkflow = workflowService.getActiveWorkflow(planType);

        Job job = Job.builder()
                .user(user)
                .workflowId(activeWorkflow.getWorkflowId())
                .workflowVersion(activeWorkflow.getVersion())
                .planType(planType)
                .status(JobStatus.QUEUED)
                .priority(activeWorkflow.getPriority())
                .build();

        Job savedJob = jobRepository.save(job);

        // Initialize the orchestration pipeline — creates StepRuns + schedules root steps
        orchestrator.initializeJobOrchestration(savedJob);

        return savedJob;
    }

    /**
     * Creates a job without a User (for anonymous/test usage).
     */
    @Transactional
    public Job createGenerationJobAnonymous(String planType) {
        log.info("Creating anonymous generation job under plan {}", planType);

        WorkflowDefinition activeWorkflow = workflowService.getActiveWorkflow(planType);

        Job job = Job.builder()
                .workflowId(activeWorkflow.getWorkflowId())
                .workflowVersion(activeWorkflow.getVersion())
                .planType(planType)
                .status(JobStatus.QUEUED)
                .priority(activeWorkflow.getPriority())
                .build();

        Job savedJob = jobRepository.save(job);
        orchestrator.initializeJobOrchestration(savedJob);
        return savedJob;
    }

    @Transactional(readOnly = true)
    public Job getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new SimplyViralException("Job not found: " + jobId));
    }

    @Transactional(readOnly = true)
    public List<StepRun> getJobStepRuns(UUID jobId) {
        return stepRunRepository.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public JobStatus getJobStatus(UUID jobId) {
        return getJob(jobId).getStatus();
    }
}
