package com.laker.postman.performance.execution;


@FunctionalInterface
interface PerformanceProtocolSamplerExecutor {
    ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception;
}
