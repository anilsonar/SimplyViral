package com.simplyviral.orchestration.service;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.repository.StepRunRepository;
import com.simplyviral.queue.service.QueueProvider;
import com.simplyviral.shared.constant.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Transitions a StepRun from PENDING to READY and
 * enqueues it for processing by the StepWorker.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StepScheduler {

    private final QueueProvider queueProvider;
    private final StepRunRepository stepRunRepository;

    public void scheduleStep(StepRun stepRun) {
        log.info("Scheduling step {} for Job {} (StepRun {})",
                stepRun.getStepKey(), stepRun.getJob().getId(), stepRun.getId());

        stepRun.setStatus(StepStatus.READY);
        stepRunRepository.save(stepRun);

        queueProvider.enqueue(stepRun.getJob().getId(), stepRun.getStepKey().name());
    }
}
