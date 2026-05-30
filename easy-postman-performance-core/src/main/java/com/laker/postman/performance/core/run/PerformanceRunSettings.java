package com.laker.postman.performance.core.run;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceRunSettings {
    boolean efficientMode;

    @Builder
    public PerformanceRunSettings(Boolean efficientMode) {
        this.efficientMode = efficientMode == null || efficientMode;
    }

    public static PerformanceRunSettings defaults() {
        return PerformanceRunSettings.builder().build();
    }
}
