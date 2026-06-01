package com.laker.postman.performance.model;

import com.laker.postman.performance.core.model.PerformanceSampleRecord;


import com.laker.postman.performance.execution.PerformanceRequestExecutionResult;

import java.util.function.Supplier;

public class PerformanceSampleEvent {
    private volatile PerformanceSampleResult sampleResult;
    private final PerformanceSampleRecord sampleRecord;
    private final PerformanceRequestExecutionResult executionResult;
    private final boolean efficientMode;
    private final Supplier<PerformanceSampleResult> sampleResultSupplier;

    public PerformanceSampleEvent(PerformanceSampleResult sampleResult,
                                  PerformanceRequestExecutionResult executionResult,
                                  boolean efficientMode) {
        this(sampleResult == null ? null : sampleResult.toSampleRecord(),
                executionResult,
                efficientMode,
                () -> sampleResult);
        this.sampleResult = sampleResult;
    }

    private PerformanceSampleEvent(PerformanceSampleRecord sampleRecord,
                                   PerformanceRequestExecutionResult executionResult,
                                   boolean efficientMode,
                                   Supplier<PerformanceSampleResult> sampleResultSupplier) {
        this.sampleRecord = sampleRecord;
        this.executionResult = executionResult;
        this.efficientMode = efficientMode;
        this.sampleResultSupplier = sampleResultSupplier;
    }

    public static PerformanceSampleEvent lazy(PerformanceSampleRecord sampleRecord,
                                              PerformanceRequestExecutionResult executionResult,
                                              boolean efficientMode,
                                              Supplier<PerformanceSampleResult> sampleResultSupplier) {
        return new PerformanceSampleEvent(sampleRecord, executionResult, efficientMode, sampleResultSupplier);
    }

    public PerformanceSampleResult getSampleResult() {
        return sampleResult();
    }

    public PerformanceRequestExecutionResult getExecutionResult() {
        return executionResult;
    }

    public boolean isEfficientMode() {
        return efficientMode;
    }

    public PerformanceSampleResult sampleResult() {
        PerformanceSampleResult current = sampleResult;
        if (current != null || sampleResultSupplier == null) {
            return current;
        }
        synchronized (this) {
            if (sampleResult == null) {
                sampleResult = sampleResultSupplier.get();
            }
            return sampleResult;
        }
    }

    public PerformanceRequestExecutionResult executionResult() {
        return executionResult;
    }

    public PerformanceSampleRecord sampleRecord() {
        if (sampleRecord != null) {
            return sampleRecord;
        }
        PerformanceSampleResult current = sampleResult();
        return current == null ? null : current.toSampleRecord();
    }

    public boolean efficientMode() {
        return efficientMode;
    }
}
