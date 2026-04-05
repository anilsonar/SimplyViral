package com.simplyviral.orchestration.repository;

import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.shared.constant.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByUserIdAndStatus(UUID userId, JobStatus status);
}
