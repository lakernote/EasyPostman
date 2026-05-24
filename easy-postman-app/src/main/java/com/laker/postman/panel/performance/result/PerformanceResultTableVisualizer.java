package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceResultRetentionPolicy;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;
import com.laker.postman.panel.performance.model.PerformanceSampleResult;
import lombok.RequiredArgsConstructor;

import java.util.function.IntSupplier;

@RequiredArgsConstructor
public final class PerformanceResultTableVisualizer implements PerformanceResultListener {
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (resultTablePanel == null || event == null || event.getSampleResult() == null) {
            return;
        }
        PerformanceSampleResult sampleResult = event.getSampleResult();
        int slowRequestThresholdMs = slowRequestThresholdSupplier == null
                ? 0
                : slowRequestThresholdSupplier.getAsInt();
        if (!PerformanceResultRetentionPolicy.shouldRecord(
                event.isEfficientMode(),
                sampleResult.isSuccessful(),
                sampleResult.getElapsedTimeMs(),
                slowRequestThresholdMs)) {
            return;
        }
        resultTablePanel.addResult(PerformanceResultDisplayMapper.toDisplayNodeInfo(sampleResult));
    }
}
