package com.laker.postman.panel.performance.result;

import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
class PerformanceTrendTheme {
    Color chartBackground() {
        return PerformanceTheme.chartBackground();
    }

    Color chartPanelBackground() {
        return PerformanceTheme.chartPanelBackground();
    }

    Color gridLine() {
        return PerformanceTheme.chartGridLine();
    }

    Color text() {
        return PerformanceTheme.chartText();
    }

    Color chartBorder() {
        return PerformanceTheme.chartBorder();
    }

    Color threadsLine() {
        return PerformanceTheme.chartThreadsLine();
    }

    Color responseTimeLine() {
        return PerformanceTheme.chartResponseTimeLine();
    }

    Color qpsLine() {
        return PerformanceTheme.chartQpsLine();
    }

    Color matchedLine() {
        return PerformanceTheme.chartMatchedLine();
    }

    Color durationLine() {
        return PerformanceTheme.chartDurationLine();
    }

    Color errorRateLine() {
        return PerformanceTheme.chartErrorRateLine();
    }
}
