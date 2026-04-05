package com.simplyviral.queue.entity;

import com.simplyviral.orchestration.entity.Job;
import com.simplyviral.shared.constant.StepKey;
import com.simplyviral.shared.constant.StepStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "step_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "queue_id")
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

    @Column(name = "next_attempt_at", nullable = false)
    private OffsetDateTime nextAttemptAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "claimed_by", length = 255)
    private String claimedBy;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;
}
