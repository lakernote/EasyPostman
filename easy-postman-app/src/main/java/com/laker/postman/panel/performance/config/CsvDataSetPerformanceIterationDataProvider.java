package com.laker.postman.panel.performance.config;

import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;

import java.util.Map;

public final class CsvDataSetPerformanceIterationDataProvider implements PerformanceIterationDataProvider {
    @Override
    public Map<String, String> dataForVirtualUser(PerformanceThreadGroupPlan groupPlan, int virtualUserIndex) {
        if (groupPlan == null) {
            return null;
        }
        CsvDataSetData data = groupPlan.getCsvDataSetData();
        if (data == null || !data.hasRows()) {
            return null;
        }
        return data.rowForVirtualUser(virtualUserIndex);
    }
}
