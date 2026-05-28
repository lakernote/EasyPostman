package com.laker.postman.panel.performance.threadgroup;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.threadgroup.PerformanceCoreThreadGroupPlanner;



public final class PerformanceThreadGroupPlanner {

    private final PerformanceCoreThreadGroupPlanner delegate = new PerformanceCoreThreadGroupPlanner();

    public int getTotalThreads(PerformanceTestPlan plan) {
        return delegate.getTotalThreads(plan);
    }

    public long estimateTotalRequests(PerformanceTestPlan plan) {
        return delegate.estimateTotalRequests(plan);
    }
}
