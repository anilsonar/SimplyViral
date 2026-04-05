package com.simplyviral.orchestration.entity;

import com.simplyviral.shared.constant.StepKey;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stores intermediate data produced by each step so that downstream steps
 * can read it without coupling. Each artifact_key is unique per job.
 */
@Entity
@Table(name = "job_artifacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "artifact_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_key", nullable = false, length = 100)
    private StepKey stepKey;

    @Column(name = "artifact_key", nullable = false, length = 100)
    private String artifactKey;

    @Column(name = "artifact_value", columnDefinition = "TEXT")
    private String artifactValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
