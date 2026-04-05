package com.simplyviral.orchestration.repository;

import com.simplyviral.orchestration.entity.StepRunUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StepRunUsageRepository extends JpaRepository<StepRunUsage, UUID> {
}
