package com.laker.postman.panel.performance.result;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import javax.swing.UIManager;
import java.awt.Color;

@UtilityClass
class PerformanceTheme {

    Color chartBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color chartPanelBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    Color chartGridLine() {
        return uiColor("Performance.chart.gridColor", ModernColors.getBorderMediumColor());
    }

    Color chartText() {
        return uiColor("Performance.chart.textColor", ModernColors.getTextPrimary());
    }

    Color chartBorder() {
        return uiColor("Performance.chart.axisColor", ModernColors.getBorderMediumColor());
    }

    Color chartCurveLine() {
        return uiColor("Performance.chart.curveColor", ModernColors.getPrimary());
    }

    Color chartThreadsLine() {
        return uiColor("Performance.chart.threadsLine", chartCurveLine());
    }

    Color chartResponseTimeLine() {
        return uiColor("Performance.chart.responseTimeLine", ModernColors.getWarning());
    }

    Color chartQpsLine() {
        return uiColor("Performance.chart.qpsLine", ModernColors.getSuccess());
    }

    Color chartMatchedLine() {
        return uiColor("Performance.chart.matchedLine", ModernColors.getSecondary());
    }

    Color chartDurationLine() {
        return uiColor("Performance.chart.durationLine", ModernColors.getInfo());
    }

    Color chartErrorRateLine() {
        return uiColor("Performance.chart.errorRateLine", ModernColors.getError());
    }

    Color reportTotalForeground() {
        return uiColor("Performance.report.totalForeground", ModernColors.getTextPrimary());
    }

    Color reportTotalBackground() {
        return uiColor("Performance.report.totalBackground", ModernColors.getHoverBackgroundColor());
    }

    Color reportSuccessForeground() {
        return uiColor("Performance.report.successColor", ModernColors.getSuccess());
    }

    Color reportWarningForeground() {
        return uiColor("Performance.report.warningColor", ModernColors.getWarningDark());
    }

    Color reportErrorForeground() {
        return uiColor("Performance.report.errorColor", ModernColors.getError());
    }

    Color resultSuccessForeground() {
        return uiColor("Performance.result.successForeground", ModernColors.getSuccessDark());
    }

    Color resultWarningForeground() {
        return uiColor("Performance.result.warningForeground", ModernColors.getWarningDarker());
    }

    Color resultFailureForeground() {
        return uiColor("Performance.result.failureForeground", ModernColors.getErrorDark());
    }

    Color resultMutedForeground() {
        return uiColor("Performance.result.mutedForeground", ModernColors.getTextHint());
    }

    Color resultGridColor() {
        return uiColor("Performance.result.gridColor", ModernColors.getBorderLightColor());
    }

    Color resultSuccessStripe() {
        return uiColor("Performance.result.successStripe", resultSuccessForeground());
    }

    Color resultFailureStripe() {
        return uiColor("Performance.result.failureStripe", resultFailureForeground());
    }

    Color tableForeground() {
        return uiColor("Table.foreground", ModernColors.getTextPrimary());
    }

    Color tableBackground() {
        return ModernColors.getCardBackgroundColor();
    }

    private Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}
