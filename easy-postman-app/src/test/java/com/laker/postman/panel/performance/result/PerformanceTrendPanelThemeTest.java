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
                "Performance.chart.curveColor"
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
    public void primaryLineShouldUsePerformanceCurveToken() {
        Color curve = new Color(31, 32, 33);
        UIManager.put("Performance.chart.curveColor", curve);

        assertEquals(PerformanceTrendTheme.threadsLine(), curve);
    }
}
