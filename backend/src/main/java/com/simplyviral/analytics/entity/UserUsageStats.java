package com.simplyviral.analytics.entity;

import com.simplyviral.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks per-user usage metrics for billing, analytics, and rate-limiting.
 * Updated after each job completes to reflect cumulative usage.
 */
@Entity
@Table(name = "user_usage_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUsageStats {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period", nullable = false, length = 20)
    private String period; // e.g., "2026-04", "2026-W14"

    @Column(name = "total_jobs")
    @Builder.Default
    private Integer totalJobs = 0;

    @Column(name = "total_cost", precision = 12, scale = 4)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "total_tokens_used")
    @Builder.Default
    private Long totalTokensUsed = 0L;

    @Column(name = "total_images_generated")
    @Builder.Default
    private Integer totalImagesGenerated = 0;

    @Column(name = "total_audio_seconds", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAudioSeconds = BigDecimal.ZERO;

    @Column(name = "total_videos_rendered")
    @Builder.Default
    private Integer totalVideosRendered = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
