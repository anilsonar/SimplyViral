package com.simplyviral.orchestration.service;

import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.entity.JobArtifact;
import com.simplyviral.orchestration.repository.JobArtifactRepository;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.exception.SimplyViralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Helper service for reading and writing inter-step artifacts.
 * Keeps handlers free from direct repository coupling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobArtifactService {

    private final JobArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public String readArtifact(UUID jobId, String artifactKey) {
        return artifactRepository.findByJobIdAndArtifactKey(jobId, artifactKey)
                .map(JobArtifact::getArtifactValue)
                .orElseThrow(() -> new SimplyViralException(
                        "Missing artifact '" + artifactKey + "' for job " + jobId));
    }

    @Transactional(readOnly = true)
    public String readArtifactOrNull(UUID jobId, String artifactKey) {
        return artifactRepository.findByJobIdAndArtifactKey(jobId, artifactKey)
                .map(JobArtifact::getArtifactValue)
                .orElse(null);
    }

    @Transactional
    public void writeArtifact(Job job, StepKey stepKey, String artifactKey, String value) {
        log.info("Persisting artifact '{}' for job {} from step {}", artifactKey, job.getId(), stepKey);

        // Upsert: delete existing artifact with same key for this job, then insert
        artifactRepository.findByJobIdAndArtifactKey(job.getId(), artifactKey)
                .ifPresent(artifactRepository::delete);

        JobArtifact artifact = JobArtifact.builder()
                .job(job)
                .stepKey(stepKey)
                .artifactKey(artifactKey)
                .artifactValue(value)
                .build();
        artifactRepository.save(artifact);
    }
}
