package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;

final class PerformanceStatsResultListener implements PerformanceResultListener {
    private final PerformanceStatsCollector statsCollector;

    PerformanceStatsResultListener(PerformanceStatsCollector statsCollector) {
        this.statsCollector = statsCollector;
    }

    @Override
    public void onSample(PerformanceSampleEvent event) {
        if (statsCollector == null || event == null || event.getSampleResult() == null) {
            return;
        }
        statsCollector.record(event.getSampleResult().toRequestResult());
    }
}
