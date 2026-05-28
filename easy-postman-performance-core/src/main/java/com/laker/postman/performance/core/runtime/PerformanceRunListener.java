package com.laker.postman.performance.core.runtime;

public interface PerformanceRunListener {
    PerformanceRunListener NOOP = new PerformanceRunListener() {
    };

    default void onProgress(PerformanceRunProgress progress) {
    }

    default void onError(PerformanceRunError error) {
    }
}
