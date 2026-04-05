package com.simplyviral.orchestration.repository;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepRunRepository extends JpaRepository<StepRun, UUID> {
    List<StepRun> findByJobId(UUID jobId);
    Optional<StepRun> findByJobIdAndStepKey(UUID jobId, StepKey stepKey);
    List<StepRun> findByJobIdAndStatus(UUID jobId, StepStatus status);
    long countByJobIdAndStatus(UUID jobId, StepStatus status);
}
