package com.laker.postman.panel.performance.result;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class PerformanceTrendPanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.SURFACE,
                ThemeColors.PRIMARY,
                "Performance.chart.gridColor",
                "Performance.chart.axisColor",
                "Performance.chart.textColor",
                "Performance.chart.curveColor",
                "Performance.chart.threadsLine",
                "Performance.chart.qpsLine",
                "Performance.chart.responseTimeLine",
                "Performance.chart.matchedLine",
                "Performance.chart.durationLine",
                "Performance.chart.errorRateLine"
        );
    }

    @AfterMethod
    public void restoreThemeTokens() {
        restore(previousThemeTokens);
    }

    @Test
    public void chartSurfaceColorsShouldUseThemeTokens() {
        Color surface = new Color(1, 2, 3);
        Color grid = new Color(4, 5, 6);
        Color axis = new Color(7, 8, 9);
        Color text = new Color(10, 11, 12);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put("Performance.chart.gridColor", grid);
        UIManager.put("Performance.chart.axisColor", axis);
        UIManager.put("Performance.chart.textColor", text);

        assertEquals(PerformanceTrendTheme.chartBackground(), surface);
        assertEquals(PerformanceTrendTheme.chartPanelBackground(), surface);
        assertEquals(PerformanceTrendTheme.gridLine(), grid);
        assertEquals(PerformanceTrendTheme.chartBorder(), axis);
        assertEquals(PerformanceTrendTheme.text(), text);
    }

    @Test
    public void metricLinesShouldUsePerformanceTokens() {
        Color threads = new Color(31, 32, 33);
        Color qps = new Color(34, 35, 36);
        Color responseTime = new Color(37, 38, 39);
        Color matched = new Color(40, 41, 42);
        Color duration = new Color(43, 44, 45);
        Color errorRate = new Color(46, 47, 48);
        UIManager.put("Performance.chart.threadsLine", threads);
        UIManager.put("Performance.chart.qpsLine", qps);
        UIManager.put("Performance.chart.responseTimeLine", responseTime);
        UIManager.put("Performance.chart.matchedLine", matched);
        UIManager.put("Performance.chart.durationLine", duration);
        UIManager.put("Performance.chart.errorRateLine", errorRate);

        assertEquals(PerformanceTrendTheme.threadsLine(), threads);
        assertEquals(PerformanceTrendTheme.qpsLine(), qps);
        assertEquals(PerformanceTrendTheme.responseTimeLine(), responseTime);
        assertEquals(PerformanceTrendTheme.matchedLine(), matched);
        assertEquals(PerformanceTrendTheme.durationLine(), duration);
        assertEquals(PerformanceTrendTheme.errorRateLine(), errorRate);
    }
}
