package com.simplyviral.provider.client;

import com.simplyviral.orchestration.entity.StepRun;

public interface ProviderClient<T, R> {
    String getProviderKey();
    
    // T is the generic request payload, R is the generic response structure representing output and usage
    R execute(T request, StepRun stepContext);
}
