package com.simplyviral.analytics.service;

import com.simplyviral.analytics.entity.UserUsageStats;
import com.simplyviral.analytics.repository.UserUsageStatsRepository;
import com.simplyviral.identity.entity.User;
import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.orchestration.repository.StepRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Aggregates per-user usage metrics after job completion.
 * Called by the Orchestrator when a job finishes successfully.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserUsageStatsRepository statsRepository;
    private final StepRunRepository stepRunRepository;

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Updates the user's usage stats for the current month after a job completes.
     */
    @Transactional
    public void recordJobCompletion(Job job) {
        User user = job.getUser();
        if (user == null) {
            log.warn("Job {} has no user associated; skipping analytics", job.getId());
            return;
        }

        String period = OffsetDateTime.now().format(PERIOD_FORMAT);

        UserUsageStats stats = statsRepository.findByUserIdAndPeriod(user.getId(), period)
                .orElseGet(() -> UserUsageStats.builder()
                        .user(user)
                        .period(period)
                        .build());

        // Aggregate costs from all step runs in this job
        List<StepRun> stepRuns = stepRunRepository.findByJobId(job.getId());
        BigDecimal jobCost = stepRuns.stream()
                .map(sr -> sr.getActualCost() != null ? sr.getActualCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long tokenCount = stepRuns.stream()
                .filter(sr -> sr.getUsage() != null && sr.getUsage().getTotalTokens() != null)
                .mapToLong(sr -> sr.getUsage().getTotalTokens())
                .sum();

        stats.setTotalJobs(stats.getTotalJobs() + 1);
        stats.setTotalCost(stats.getTotalCost().add(jobCost));
        stats.setTotalTokensUsed(stats.getTotalTokensUsed() + tokenCount);
        stats.setTotalVideosRendered(stats.getTotalVideosRendered() + 1);
        stats.setUpdatedAt(OffsetDateTime.now());

        statsRepository.save(stats);
        log.info("Analytics updated for user={} period={}: jobs={}, total_cost={}",
                user.getId(), period, stats.getTotalJobs(), stats.getTotalCost());
    }

    /**
     * Retrieves usage stats for a user for the current billing period.
     */
    @Transactional(readOnly = true)
    public UserUsageStats getCurrentPeriodStats(UUID userId) {
        String period = OffsetDateTime.now().format(PERIOD_FORMAT);
        return statsRepository.findByUserIdAndPeriod(userId, period).orElse(null);
    }
}
