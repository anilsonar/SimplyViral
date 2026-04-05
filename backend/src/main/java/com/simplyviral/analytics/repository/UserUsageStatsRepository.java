package com.simplyviral.analytics.repository;

import com.simplyviral.analytics.entity.UserUsageStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserUsageStatsRepository extends JpaRepository<UserUsageStats, UUID> {
    Optional<UserUsageStats> findByUserIdAndPeriod(UUID userId, String period);
}
