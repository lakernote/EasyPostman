package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;
import com.laker.postman.panel.performance.model.PerformanceSampleResult;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;

import java.util.function.IntSupplier;

final class PerformanceResultTableListener implements PerformanceResultListener {
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    PerformanceResultTableListener(PerformanceResultTablePanel resultTablePanel,
                                   IntSupplier slowRequestThresholdSupplier) {
        this.resultTablePanel = resultTablePanel;
        this.slowRequestThresholdSupplier = slowRequestThresholdSupplier;
    }

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (resultTablePanel == null || event == null || event.getSampleResult() == null) {
            return;
        }
        PerformanceSampleResult sampleResult = event.getSampleResult();
        int slowRequestThresholdMs = slowRequestThresholdSupplier == null
                ? 0
                : slowRequestThresholdSupplier.getAsInt();
        if (!PerformanceResultRecorder.shouldRecordResult(
                event.isEfficientMode(),
                sampleResult.isSuccessful(),
                sampleResult.getElapsedTimeMs(),
                slowRequestThresholdMs)) {
            return;
        }
        resultTablePanel.addResult(PerformanceResultNodeInfoMapper.toDisplayNodeInfo(sampleResult));
    }
}
