package com.laker.postman.performance.execution;


import com.laker.postman.http.runtime.transport.HttpRuntimeExecutor;

import java.util.List;

final class HttpSamplerExecutor implements PerformanceProtocolSamplerExecutor {
    private final PerformanceNetworkRuntime networkRuntime;

    HttpSamplerExecutor() {
        this(new DefaultPerformanceNetworkRuntime());
    }

    HttpSamplerExecutor(PerformanceNetworkRuntime networkRuntime) {
        this.networkRuntime = networkRuntime == null ? new DefaultPerformanceNetworkRuntime() : networkRuntime;
    }

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception {
        return new ProtocolExecutionResult(
                HttpRuntimeExecutor.executeHttp(context.getRequest(), null, networkRuntime, networkRuntime),
                "",
                false,
                false,
                List.of()
        );
    }
}
