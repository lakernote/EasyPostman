package com.laker.postman.performance.core.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceTestPlan {
    private final List<PerformanceThreadGroupPlan> threadGroups;

    public PerformanceTestPlan(List<PerformanceThreadGroupPlan> threadGroups) {
        this.threadGroups = Collections.unmodifiableList(new ArrayList<>(
                threadGroups == null ? List.of() : threadGroups
        ));
    }

    public List<PerformanceThreadGroupPlan> getThreadGroups() {
        return threadGroups;
    }
}
