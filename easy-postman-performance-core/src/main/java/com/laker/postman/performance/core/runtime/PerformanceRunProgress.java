package com.laker.postman.performance.core.runtime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PerformanceRunProgress {
    int activeThreads;
    int totalThreads;
    long sequence;

    public static PerformanceRunProgress sequenced(int activeThreads, int totalThreads, long sequence) {
        return new PerformanceRunProgress(activeThreads, totalThreads, Math.max(0L, sequence));
    }

    public PerformanceRunProgress(int activeThreads, int totalThreads) {
        this(activeThreads, totalThreads, 0L);
    }
}
