package com.simplyviral.queue.service;

import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.repository.JobRepository;
import com.simplyviral.queue.entity.StepQueue;
import com.simplyviral.queue.repository.StepQueueRepository;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * DB-backed queue implementation using step_queue table.
 * Uses SELECT ... FOR UPDATE SKIP LOCKED style atomicity via StepQueueRepository.
 * Managed as a bean by QueueConfig, not component-scanned.
 */
@Slf4j
@RequiredArgsConstructor
public class DbQueueProvider implements QueueProvider {

    private final StepQueueRepository queueRepository;
    private final JobRepository jobRepository;

    @Override
    public void enqueue(UUID jobId, String stepKey) {
        log.info("Enqueueing step {} for job {}", stepKey, jobId);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));

        StepQueue entry = StepQueue.builder()
                .job(job)
                .stepKey(StepKey.valueOf(stepKey))
                .status(StepStatus.READY)
                .nextAttemptAt(OffsetDateTime.now())
                .build();
        queueRepository.save(entry);
    }

    @Override
    public Optional<StepQueue> claim(String workerId) {
        log.debug("Worker {} attempting to claim a task from step_queue", workerId);

        return queueRepository.findAndClaimNext(StepStatus.READY, OffsetDateTime.now())
                .map(entry -> {
                    entry.setClaimedBy(workerId);
                    entry.setClaimedAt(OffsetDateTime.now());
                    entry.setStatus(StepStatus.RUNNING);
                    queueRepository.save(entry);
                    log.info("Worker {} claimed queue entry {} for step {} / job {}",
                            workerId, entry.getId(), entry.getStepKey(), entry.getJob().getId());
                    return entry;
                });
    }

    @Override
    public void acknowledge(UUID queueId) {
        log.info("Acknowledging completion of queue entry {}", queueId);
        queueRepository.findById(queueId).ifPresent(entry -> {
            entry.setStatus(StepStatus.COMPLETED);
            queueRepository.save(entry);
        });
    }

    @Override
    public void requeue(UUID queueId, long delayMs) {
        log.info("Requeueing task {} with delay {} ms", queueId, delayMs);
        queueRepository.findById(queueId).ifPresent(entry -> {
            entry.setStatus(StepStatus.READY);
            entry.setClaimedBy(null);
            entry.setClaimedAt(null);
            entry.setNextAttemptAt(OffsetDateTime.now().plusNanos(delayMs * 1_000_000L));
            queueRepository.save(entry);
        });
    }

    @Override
    public void deadLetter(UUID queueId, String reason) {
        log.warn("Moving queue entry {} to dead letter, reason: {}", queueId, reason);
        queueRepository.findById(queueId).ifPresent(entry -> {
            entry.setStatus(StepStatus.DEAD_LETTER);
            queueRepository.save(entry);
        });
    }
}
