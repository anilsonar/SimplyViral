package com.simplyviral.workflow.repository;

import com.simplyviral.workflow.entity.WorkflowDefinition;
import com.simplyviral.workflow.entity.WorkflowDefinitionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, WorkflowDefinitionId> {
    List<WorkflowDefinition> findByActiveFlagTrue();
}
