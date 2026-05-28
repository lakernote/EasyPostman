package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;


import com.laker.postman.panel.performance.model.PerformanceSampleEvent;

public interface PerformanceResultSink extends PerformanceCoreResultSink {
    PerformanceResultSink NOOP = new PerformanceResultSink() {
    };

    @Override
    default void onSample(PerformanceSampleRecord record) {
    }

    default void onSample(PerformanceSampleEvent event) {
        onSample(event == null ? null : event.sampleRecord());
    }
}
