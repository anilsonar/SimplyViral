package com.simplyviral.workflow.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinitionId implements Serializable {
    private String workflowId;
    private Integer version;
}
