package com.simplyviral.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "step_definitions")
@IdClass(StepDefinitionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepDefinition {

    @Id
    @Column(name = "step_key", length = 100)
    private String stepKey;

    @Id
    @Column(name = "workflow_id", length = 100)
    private String workflowId;

    @Id
    @Column(name = "workflow_version")
    private Integer workflowVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "workflow_id", referencedColumnName = "workflow_id", insertable = false, updatable = false),
        @JoinColumn(name = "workflow_version", referencedColumnName = "version", insertable = false, updatable = false)
    })
    private WorkflowDefinition workflowDefinition;

    /** JSON: {"depends_on": ["STEP_A", "STEP_B"]} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dependency_rule", columnDefinition = "jsonb")
    private String dependencyRule;

    /** JSON: {"provider_key": "openai", "model_config_key": "gpt-4o-baseline"} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_policy_ref", columnDefinition = "jsonb")
    private String providerPolicyRef;

    /** JSON: {"max_attempts": 3, "initial_delay_ms": 2000, "backoff_multiplier": 2.0, "max_delay_ms": 30000} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retry_policy_ref", columnDefinition = "jsonb")
    private String retryPolicyRef;

    /** JSON: {"fallback_model_config_key": "gpt-4-turbo-baseline"} (nullable) */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fallback_policy_ref", columnDefinition = "jsonb")
    private String fallbackPolicyRef;

    /** JSON: {"persist": true} */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "storage_policy_ref", columnDefinition = "jsonb")
    private String storagePolicyRef;
}
