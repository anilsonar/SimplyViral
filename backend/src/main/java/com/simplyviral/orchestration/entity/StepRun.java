package com.simplyviral.orchestration.entity;

import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "step_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "step_run_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_key", nullable = false, length = 100)
    private StepKey stepKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StepStatus status;

    @Column(name = "attempt_no")
    @Builder.Default
    private Integer attemptNo = 1;

    @Column(name = "provider_ref", length = 100)
    private String providerRef;

    @Column(name = "model_config_ref", length = 100)
    private String modelConfigRef;

    @Column(name = "planned_cost", precision = 10, scale = 4)
    private BigDecimal plannedCost;

    @Column(name = "actual_cost", precision = 10, scale = 4)
    private BigDecimal actualCost;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Transient holder so that adapter-level usage (tokens, image count, etc.)
     * can be passed from the adapter through the MeteredProviderExecutor
     * to the StepWorker for persistence without tight coupling.
     */
    @Transient
    private StepRunUsage usage;
}
