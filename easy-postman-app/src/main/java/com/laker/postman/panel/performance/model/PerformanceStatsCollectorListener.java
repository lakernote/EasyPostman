package com.laker.postman.panel.performance.model;

import com.laker.postman.performance.core.model.PerformanceStatsCollector;


import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class PerformanceStatsCollectorListener implements PerformanceResultListener {
    private final PerformanceStatsCollector statsCollector;

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (statsCollector == null || event == null || event.getSampleResult() == null) {
            return;
        }
        statsCollector.record(event.getSampleResult().toRequestResult());
    }
}
