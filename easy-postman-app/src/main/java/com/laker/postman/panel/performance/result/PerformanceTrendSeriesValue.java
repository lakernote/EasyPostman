package com.laker.postman.panel.performance.result;

final class PerformanceTrendSeriesValue {

    private PerformanceTrendSeriesValue() {
    }

    static Number activeCount(int value) {
        return Math.max(0, value);
    }

    static Number activeCount(int value, boolean suppressZero) {
        return suppressZero ? null : activeCount(value);
    }

    static Number sampleMetric(double value) {
        return Double.isFinite(value) ? value : null;
    }
}
