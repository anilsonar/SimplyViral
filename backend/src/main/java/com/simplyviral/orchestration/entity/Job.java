package com.simplyviral.orchestration.entity;

import com.simplyviral.identity.entity.User;
import com.simplyviral.shared.constant.JobStatus;
import com.simplyviral.workflow.entity.WorkflowDefinition;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "workflow_id", nullable = false, length = 100)
    private String workflowId;

    @Column(name = "workflow_version", nullable = false)
    private Integer workflowVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "workflow_id", referencedColumnName = "workflow_id", insertable = false, updatable = false),
        @JoinColumn(name = "workflow_version", referencedColumnName = "version", insertable = false, updatable = false)
    })
    private WorkflowDefinition workflowDefinition;

    @Column(name = "plan_type", nullable = false, length = 50)
    private String planType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;
}
