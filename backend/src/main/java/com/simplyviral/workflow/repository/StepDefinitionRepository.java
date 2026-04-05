package com.simplyviral.workflow.repository;

import com.simplyviral.workflow.entity.StepDefinition;
import com.simplyviral.workflow.entity.StepDefinitionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepDefinitionRepository extends JpaRepository<StepDefinition, StepDefinitionId> {
    List<StepDefinition> findByWorkflowIdAndWorkflowVersion(String workflowId, Integer workflowVersion);
}
