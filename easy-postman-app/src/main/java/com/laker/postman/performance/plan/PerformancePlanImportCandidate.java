package com.laker.postman.performance.plan;

import lombok.Value;

@Value
public class PerformancePlanImportCandidate {
    String name;
    PerformancePlanConfiguration configuration;

    public PerformancePlanImportCandidate(String name, PerformancePlanConfiguration configuration) {
        this.name = name == null ? "" : name.trim();
        this.configuration = configuration == null
                ? PerformancePlanConfiguration.builder().build()
                : configuration;
    }
}
