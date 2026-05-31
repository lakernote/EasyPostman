package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.model.PreparedRequest;
import com.laker.postman.performance.plan.PerformanceRequestSampler;
import lombok.Value;

@Value
class PerformanceProtocolSamplerContext {
    PreparedRequest request;
    PerformanceRequestSampler requestSampler;
    PerformanceRequestSnapshot requestSnapshot;
    String requestBodyTemplate;
    PerformanceScriptRuntime scriptRuntime;
    PerformanceResponseCapturePlan capturePlan;

    String getRequestId() {
        return requestSnapshot == null ? "" : requestSnapshot.getId();
    }

    String getRequestName() {
        return requestSnapshot == null ? "" : requestSnapshot.getName();
    }
}
