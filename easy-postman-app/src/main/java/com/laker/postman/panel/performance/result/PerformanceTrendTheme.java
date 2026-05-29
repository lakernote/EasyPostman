package com.laker.postman.panel.performance.result;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
import java.awt.Color;

@UtilityClass
class PerformanceTrendTheme {
    Color chartBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color chartPanelBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color gridLine() {
        return uiColor("Performance.chart.gridColor", ModernColors.getBorderMediumColor());
    }

    Color text() {
        return uiColor("Performance.chart.textColor", ModernColors.getTextPrimary());
    }

    Color chartBorder() {
        return uiColor("Performance.chart.axisColor", ModernColors.getBorderMediumColor());
    }

    Color threadsLine() {
        return uiColor("Performance.chart.curveColor", ModernColors.getPrimary());
    }

    Color responseTimeLine() {
        return ModernColors.getWarning();
    }

    Color qpsLine() {
        return ModernColors.getSuccess();
    }

    Color matchedLine() {
        return ModernColors.getSecondary();
    }

    Color durationLine() {
        return ModernColors.getInfo();
    }

    Color errorRateLine() {
        return ModernColors.getError();
    }

    private Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}
