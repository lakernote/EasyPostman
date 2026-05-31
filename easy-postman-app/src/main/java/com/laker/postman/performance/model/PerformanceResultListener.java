package com.laker.postman.performance.model;


@FunctionalInterface
public interface PerformanceResultListener {
    void onSample(PerformanceSampleEvent event);
}
