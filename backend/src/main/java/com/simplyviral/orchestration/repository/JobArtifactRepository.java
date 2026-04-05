package com.simplyviral.orchestration.repository;

import com.simplyviral.orchestration.entity.JobArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobArtifactRepository extends JpaRepository<JobArtifact, UUID> {
    Optional<JobArtifact> findByJobIdAndArtifactKey(UUID jobId, String artifactKey);
    List<JobArtifact> findByJobId(UUID jobId);
}
