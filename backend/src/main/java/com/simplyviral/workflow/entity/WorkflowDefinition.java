package com.simplyviral.workflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "workflow_definitions")
@IdClass(WorkflowDefinitionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowDefinition {

    @Id
    @Column(name = "workflow_id", length = 100)
    private String workflowId;

    @Id
    @Column(name = "version")
    private Integer version;

    @Column(name = "plan_type", nullable = false, length = 50)
    private String planType;

    @Column(name = "active_flag", nullable = false)
    @Builder.Default
    private Boolean activeFlag = true;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema_ref", columnDefinition = "jsonb")
    private String inputSchemaRef;
}
