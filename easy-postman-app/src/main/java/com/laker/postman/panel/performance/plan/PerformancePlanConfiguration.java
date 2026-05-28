package com.laker.postman.panel.performance.plan;


import lombok.Builder;
import lombok.Value;

@Value
public class PerformancePlanConfiguration {
    PerformancePlanDocument planDocument;
    boolean efficientMode;
    boolean trendEnabled;
    boolean reportRealtimeEnabled;

    @Builder
    public PerformancePlanConfiguration(PerformancePlanDocument planDocument,
                                        Boolean efficientMode,
                                        Boolean trendEnabled,
                                        Boolean reportRealtimeEnabled) {
        this.planDocument = planDocument;
        this.efficientMode = efficientMode == null || efficientMode;
        this.trendEnabled = trendEnabled == null || trendEnabled;
        this.reportRealtimeEnabled = reportRealtimeEnabled != null && reportRealtimeEnabled;
    }
}
