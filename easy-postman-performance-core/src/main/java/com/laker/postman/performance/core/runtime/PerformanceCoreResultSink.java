package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;

public interface PerformanceCoreResultSink {
    PerformanceCoreResultSink NOOP = new PerformanceCoreResultSink() {
    };

    default void onSample(PerformanceSampleRecord record) {
    }

    default boolean acceptsSamples() {
        return false;
    }

    default void onProgress(PerformanceRunProgress progress) {
    }

    default void onError(PerformanceRunError error) {
    }

    default void onComplete(PerformanceRunSummary summary) {
    }
}
