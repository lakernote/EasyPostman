package com.laker.postman.performance.core.model;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SsePerformanceDataSupport {

    public void applyConnectConfig(SsePerformanceData target, SsePerformanceData source) {
        if (target == null || source == null) {
            return;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
    }

    public void applyReadConfig(SsePerformanceData target, SsePerformanceData source) {
        if (target == null || source == null) {
            return;
        }
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.eventNameFilter = source.eventNameFilter;
        target.messageFilter = source.messageFilter;
    }
}
