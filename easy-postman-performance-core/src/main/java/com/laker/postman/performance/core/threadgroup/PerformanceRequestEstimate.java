package com.laker.postman.performance.core.threadgroup;

public record PerformanceRequestEstimate(long estimatedRequests, boolean dynamic) {

    public PerformanceRequestEstimate {
        estimatedRequests = Math.max(0L, estimatedRequests);
    }

    public static PerformanceRequestEstimate fixed(long estimatedRequests) {
        return new PerformanceRequestEstimate(estimatedRequests, false);
    }

    public static PerformanceRequestEstimate dynamic(long estimatedRequests) {
        return new PerformanceRequestEstimate(estimatedRequests, true);
    }
}
