package com.laker.postman.panel.performance.execution;

@FunctionalInterface
interface PerformanceProtocolSamplerExecutor {
    ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception;
}
