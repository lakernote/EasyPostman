package com.laker.postman.panel.performance.plan;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceRemoteWorkerSettings {
    boolean enabled;
    String workerEndpoints;

    @Builder
    public PerformanceRemoteWorkerSettings(Boolean enabled, String workerEndpoints) {
        this.enabled = enabled != null && enabled;
        this.workerEndpoints = workerEndpoints == null ? "" : workerEndpoints.trim();
    }

    public static PerformanceRemoteWorkerSettings disabled() {
        return PerformanceRemoteWorkerSettings.builder().build();
    }
}
