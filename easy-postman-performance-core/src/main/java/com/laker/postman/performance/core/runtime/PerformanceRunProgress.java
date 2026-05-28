package com.laker.postman.performance.core.runtime;

import lombok.Value;

@Value
public class PerformanceRunProgress {
    int activeThreads;
    int totalThreads;
}
