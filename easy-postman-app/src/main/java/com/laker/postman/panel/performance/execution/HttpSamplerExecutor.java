package com.laker.postman.panel.performance.execution;


import com.laker.postman.service.http.HttpSingleRequestExecutor;

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
                HttpSingleRequestExecutor.executeHttp(context.getRequest(), null, networkRuntime, networkRuntime),
                "",
                false,
                false,
                List.of()
        );
    }
}
