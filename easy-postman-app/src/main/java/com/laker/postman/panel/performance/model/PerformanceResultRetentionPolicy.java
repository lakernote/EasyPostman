package com.laker.postman.panel.performance.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceResultRetentionPolicy {

    public boolean shouldRecord(boolean efficientMode,
                                boolean actualSuccess,
                                long costMs,
                                int slowRequestThresholdMs) {
        if (!efficientMode) {
            return true;
        }
        if (!actualSuccess) {
            return true;
        }
        return slowRequestThresholdMs > 0 && costMs >= slowRequestThresholdMs;
    }
}
