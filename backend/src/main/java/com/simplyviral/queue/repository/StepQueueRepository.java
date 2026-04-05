package com.simplyviral.queue.repository;

import com.simplyviral.queue.entity.StepQueue;
import com.simplyviral.shared.constant.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StepQueueRepository extends JpaRepository<StepQueue, UUID> {

    @Query("SELECT q FROM StepQueue q WHERE q.status = :status AND q.nextAttemptAt <= :now ORDER BY q.createdAt ASC")
    List<StepQueue> findEligibleTasks(@Param("status") StepStatus status, @Param("now") OffsetDateTime now);

    /**
     * Atomic claim: find the oldest eligible task and lock it.
     * PESSIMISTIC_WRITE + SKIP LOCKED ensures no two workers claim the same task.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM StepQueue q WHERE q.status = :status AND q.nextAttemptAt <= :now ORDER BY q.createdAt ASC LIMIT 1")
    Optional<StepQueue> findAndClaimNext(@Param("status") StepStatus status, @Param("now") OffsetDateTime now);
}
