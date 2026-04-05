package com.simplyviral.orchestration.handler;

import com.simplyviral.orchestration.entity.StepRun;
import com.simplyviral.shared.constant.StepKey;

public interface StepHandler {
    StepKey getSupportedStep();
    
    // Executes the step logically (prompts DB, calls adapters, persists outputs)
    void handle(StepRun stepRunContext);
}
