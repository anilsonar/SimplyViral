package com.simplyviral.provider.entity;

import com.simplyviral.shared.constant.StepKey;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "prompt_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplate {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "step_key", length = 100)
    private StepKey stepKey;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "user_prompt_template", nullable = false, columnDefinition = "TEXT")
    private String userPromptTemplate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
