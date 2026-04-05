package com.simplyviral.workflow.service;

import com.simplyviral.shared.exception.SimplyViralException;
import com.simplyviral.workflow.entity.StepDefinition;
import com.simplyviral.workflow.entity.WorkflowDefinition;
import com.simplyviral.workflow.repository.StepDefinitionRepository;
import com.simplyviral.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final WorkflowDefinitionRepository workflowRepository;
    private final StepDefinitionRepository stepRepository;

    public WorkflowDefinition getActiveWorkflow(String planType) {
        return workflowRepository.findByActiveFlagTrue().stream()
                .filter(w -> w.getPlanType().equalsIgnoreCase(planType))
                .findFirst()
                .orElseThrow(() -> new SimplyViralException("No active workflow found for plan: " + planType));
    }

    public List<StepDefinition> getStepsForWorkflow(String workflowId, Integer version) {
        return stepRepository.findByWorkflowIdAndWorkflowVersion(workflowId, version);
    }
}
