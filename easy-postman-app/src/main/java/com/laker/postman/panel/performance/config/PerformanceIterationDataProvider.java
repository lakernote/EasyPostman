package com.laker.postman.panel.performance.config;

import java.util.Map;

@FunctionalInterface
public interface PerformanceIterationDataProvider {
    Map<String, String> dataForVirtualUser(int virtualUserIndex);

    static PerformanceIterationDataProvider empty() {
        return virtualUserIndex -> null;
    }
}
