package com.laker.postman.panel.performance.result;


import com.laker.postman.performance.model.PerformanceResultListener;
import com.laker.postman.performance.model.PerformanceResultRetentionPolicy;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.model.PerformanceSampleEvent;
import com.laker.postman.performance.model.PerformanceSampleResult;
import com.laker.postman.performance.result.PerformanceResultDisplayMapper;
import lombok.RequiredArgsConstructor;

import java.util.function.IntSupplier;

@RequiredArgsConstructor
public final class PerformanceResultTableVisualizer implements PerformanceResultListener {
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (resultTablePanel == null || event == null || event.sampleRecord() == null) {
            return;
        }
        PerformanceSampleRecord sampleRecord = event.sampleRecord();
        int slowRequestThresholdMs = slowRequestThresholdSupplier == null
                ? 0
                : slowRequestThresholdSupplier.getAsInt();
        if (!PerformanceResultRetentionPolicy.shouldRecord(
                event.isEfficientMode(),
                sampleRecord.isSuccessful(),
                sampleRecord.getElapsedTimeMs(),
                slowRequestThresholdMs)) {
            return;
        }
        PerformanceSampleResult sampleResult = event.getSampleResult();
        if (sampleResult == null) {
            return;
        }
        resultTablePanel.addResult(
                PerformanceResultDisplayMapper.toDisplayNodeInfo(sampleResult, event.isEfficientMode()),
                event.isEfficientMode()
        );
    }
}
