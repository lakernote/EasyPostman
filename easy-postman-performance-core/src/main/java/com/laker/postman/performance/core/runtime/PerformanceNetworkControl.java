package com.laker.postman.performance.core.runtime;

public interface PerformanceNetworkControl {
    PerformanceNetworkControl NOOP = new PerformanceNetworkControl() {
    };

    default int activeHttpCallCount() {
        return 0;
    }

    default int activeWebSocketCount() {
        return 0;
    }

    default int activeSseCount() {
        return 0;
    }

    default void cancelAll() {
    }
}
