package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import lombok.Value;

@Value
class PerformanceProtocolSamplerContext {
    PreparedRequest request;
    PerformanceRequestSampler requestSampler;
    HttpRequestItem requestItem;
    String requestBodyTemplate;
    ScriptExecutionPipeline pipeline;
}
