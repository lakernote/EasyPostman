package com.laker.postman.panel.performance.model;

import com.laker.postman.performance.core.model.PerformanceTrendWindowCollector;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PerformanceTrendWindowCollectorListener implements PerformanceResultListener {
    private final PerformanceTrendWindowCollector trendWindowCollector;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (trendWindowCollector == null || event == null || event.getSampleResult() == null) {
            return;
        }
        trendWindowCollector.record(event.getSampleResult().toRequestResult());
    }
}
