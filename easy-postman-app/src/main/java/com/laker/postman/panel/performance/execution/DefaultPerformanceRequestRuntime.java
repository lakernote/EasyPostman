package com.laker.postman.panel.performance.execution;

import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceRequestSnapshotMapper;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.variable.ExecutionVariableContext;

final class DefaultPerformanceRequestRuntime implements PerformanceRequestRuntime {

    @Override
    public PerformancePreparedRequest prepare(PerformanceRequestSnapshot requestSnapshot,
                                              PerformanceRequestSampler requestSampler,
                                              ExecutionVariableContext iterationContext,
                                              PerformanceExecutionConfig executionConfig) {
        HttpRequestItem requestItem = PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot);
        if (requestItem == null) {
            return null;
        }

        PerformanceExecutionConfig resolvedConfig = executionConfig == null
                ? PerformanceExecutionConfig.DEFAULT
                : executionConfig;
        PreparedRequest request = PreparedRequestBuilder.buildWithoutInheritance(requestItem);
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.builder()
                .request(request)
                .preScript(request.prescript)
                .postScript(request.postscript)
                .sharedExecutionContext(iterationContext)
                .requestExecutionScope(requestSampler == null ? null : requestSampler.getRequestExecutionScope())
                .deferredAuthorization(PreparedRequestBuilder.resolveDeferredAuthorizationWithoutInheritance(requestItem))
                .outputCallback(resolvedConfig.scriptOutputCallback())
                .environmentSupplier(resolvedConfig.environmentSupplier())
                .scriptExecutor(resolvedConfig.scriptExecutor())
                .build();

        return new PerformancePreparedRequest(
                requestItem.getId(),
                requestItem.getName(),
                request,
                request.body,
                new DefaultPerformanceScriptRuntime(pipeline)
        );
    }
}
