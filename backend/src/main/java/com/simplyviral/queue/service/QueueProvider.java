package com.simplyviral.queue.service;

import com.simplyviral.queue.entity.StepQueue;

import java.util.Optional;
import java.util.UUID;

public interface QueueProvider {
    void enqueue(UUID jobId, String stepKey);
    Optional<StepQueue> claim(String workerId);
    void acknowledge(UUID queueId);
    void requeue(UUID queueId, long delayMs);
    void deadLetter(UUID queueId, String reason);
}
