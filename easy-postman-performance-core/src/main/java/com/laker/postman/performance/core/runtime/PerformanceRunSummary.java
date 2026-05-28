package com.laker.postman.performance.core.runtime;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerformanceRunSummary {
    long startTimeMs;
    long endTimeMs;
    long elapsedTimeMs;
    boolean stopped;
    Throwable error;
}
