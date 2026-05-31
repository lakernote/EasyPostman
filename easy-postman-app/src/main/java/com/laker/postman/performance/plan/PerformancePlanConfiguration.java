package com.laker.postman.performance.plan;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformancePlanConfiguration {
    PerformancePlanDocument planDocument;
    boolean efficientMode;
    boolean trendEnabled;
    boolean reportRealtimeEnabled;
    PerformanceRemoteWorkerSettings remoteWorkerSettings;

    @Builder
    public PerformancePlanConfiguration(PerformancePlanDocument planDocument,
                                        Boolean efficientMode,
                                        Boolean trendEnabled,
                                        Boolean reportRealtimeEnabled,
                                        PerformanceRemoteWorkerSettings remoteWorkerSettings) {
        this.planDocument = planDocument;
        this.efficientMode = efficientMode == null || efficientMode;
        this.trendEnabled = trendEnabled == null || trendEnabled;
        this.reportRealtimeEnabled = reportRealtimeEnabled != null && reportRealtimeEnabled;
        this.remoteWorkerSettings = remoteWorkerSettings == null
                ? PerformanceRemoteWorkerSettings.disabled()
                : remoteWorkerSettings;
    }
}
