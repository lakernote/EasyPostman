package com.laker.postman.performance.master;

@FunctionalInterface
public interface PerformanceMasterRunListener {
    PerformanceMasterRunListener NOOP = progress -> {
    };

    void onProgress(PerformanceMasterRunProgress progress);
}
