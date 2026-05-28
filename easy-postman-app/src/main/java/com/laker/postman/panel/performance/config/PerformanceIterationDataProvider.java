package com.laker.postman.panel.performance.config;

import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;



import java.util.Map;

@FunctionalInterface
public interface PerformanceIterationDataProvider {
    Map<String, String> dataForVirtualUser(PerformanceThreadGroupPlan groupPlan, int virtualUserIndex);

    static PerformanceIterationDataProvider empty() {
        return (groupPlan, virtualUserIndex) -> null;
    }
}
