package com.laker.postman.panel.performance.model;

@FunctionalInterface
public interface PerformanceResultListener {
    void onSample(PerformanceSampleEvent event);
}
