package com.laker.postman.performance.plan;

import lombok.Value;

import java.util.List;

@Value
public class PerformancePlanImportResult {
    List<PerformancePlanImportCandidate> plans;

    public PerformancePlanImportResult(List<PerformancePlanImportCandidate> plans) {
        this.plans = plans == null
                ? List.of()
                : List.copyOf(plans.stream()
                .filter(java.util.Objects::nonNull)
                .toList());
    }

    public boolean isEmpty() {
        return plans.isEmpty();
    }
}
