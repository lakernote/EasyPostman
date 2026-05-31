package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;



import java.util.Map;

public final class CsvDataSetPerformanceIterationDataProvider implements PerformanceIterationDataProvider {
    @Override
    public Map<String, String> dataForVirtualUser(PerformanceThreadGroupPlan groupPlan, int virtualUserIndex) {
        if (groupPlan == null) {
            return null;
        }
        return groupPlan.csvRowForVirtualUser(virtualUserIndex);
    }
}
