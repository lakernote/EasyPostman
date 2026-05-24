package com.laker.postman.panel.performance.execution;

import com.laker.postman.service.http.HttpSingleRequestExecutor;

import java.util.List;

final class HttpSamplerExecutor implements PerformanceProtocolSamplerExecutor {

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception {
        return new ProtocolExecutionResult(
                HttpSingleRequestExecutor.executeHttp(context.getRequest()),
                "",
                false,
                false,
                List.of()
        );
    }
}
