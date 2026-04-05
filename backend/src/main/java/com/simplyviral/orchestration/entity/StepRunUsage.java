package com.simplyviral.orchestration.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "step_run_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepRunUsage {

    @Id
    @Column(name = "step_run_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "step_run_id")
    private StepRun stepRun;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "image_count")
    private Integer imageCount;

    @Column(name = "audio_seconds", precision = 10, scale = 2)
    private BigDecimal audioSeconds;

    @Column(name = "video_seconds", precision = 10, scale = 2)
    private BigDecimal videoSeconds;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_usage_json", columnDefinition = "jsonb")
    private String rawUsageJson;
}
