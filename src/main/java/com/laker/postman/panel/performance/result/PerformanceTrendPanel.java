package com.laker.postman.panel.performance.result;

import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.text.NumberFormat;

public class PerformanceTrendPanel extends SingletonBasePanel {
    private final TimeSeriesCollection trendDataset = new TimeSeriesCollection();
    private final TimeSeries userCountSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS));
    private final TimeSeries responseTimeSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS));
    private final TimeSeries qpsSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS));
    private final TimeSeries errorPercentSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JCheckBox threadsCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS), true);
        JCheckBox responseTimeCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME), true);
        JCheckBox qpsCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS), true);
        JCheckBox errorRateCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE), true);
        checkBoxPanel.add(threadsCheckBox);
        checkBoxPanel.add(responseTimeCheckBox);
        checkBoxPanel.add(qpsCheckBox);
        checkBoxPanel.add(errorRateCheckBox);
        JFreeChart trendChart = ChartFactory.createTimeSeriesChart(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_CHART_TITLE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_METRIC_VALUE),
                trendDataset,
                true,
                false,
                false
        );
        XYPlot plot = trendChart.getXYPlot();
        Color qps = new Color(222, 156, 1);
        Color responseTime = new Color(7, 123, 237);
        Color errorPercent = new Color(236, 38, 26);
        Color userCount = new Color(25, 23, 23);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, userCount);
        renderer.setSeriesPaint(1, responseTime);
        renderer.setSeriesPaint(2, qps);
        renderer.setSeriesPaint(3, errorPercent);
        plot.setDomainGridlinePaint(new Color(194, 211, 236)); // 中性色调的蓝色网格线
        plot.setRangeGridlinePaint(new Color(194, 211, 236)); // 中性色调的蓝色网格线
        trendChart.getTitle().setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 13));
        if (trendChart.getLegend() != null)
            trendChart.getLegend().setItemFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.setBackgroundPaint(Color.WHITE);
        trendChart.setBackgroundPaint(Color.WHITE);
        plot.getRangeAxis().setAutoRange(true);
        ActionListener checkBoxListener = e -> {
            trendDataset.removeAllSeries();
            int seriesIndex = 0;
            if (threadsCheckBox.isSelected()) {
                trendDataset.addSeries(userCountSeries);
                renderer.setSeriesPaint(seriesIndex++, userCount);
            }
            if (responseTimeCheckBox.isSelected()) {
                trendDataset.addSeries(responseTimeSeries);
                renderer.setSeriesPaint(seriesIndex++, responseTime);
            }
            if (qpsCheckBox.isSelected()) {
                trendDataset.addSeries(qpsSeries);
                renderer.setSeriesPaint(seriesIndex++, qps);
            }
            if (errorRateCheckBox.isSelected()) {
                trendDataset.addSeries(errorPercentSeries);
                renderer.setSeriesPaint(seriesIndex, errorPercent);
            }
            updateYAxisLabel(plot, threadsCheckBox.isSelected(), responseTimeCheckBox.isSelected(),
                    qpsCheckBox.isSelected(), errorRateCheckBox.isSelected());
        };
        threadsCheckBox.addActionListener(checkBoxListener);
        responseTimeCheckBox.addActionListener(checkBoxListener);
        qpsCheckBox.addActionListener(checkBoxListener);
        errorRateCheckBox.addActionListener(checkBoxListener);
        trendDataset.addSeries(userCountSeries);
        trendDataset.addSeries(responseTimeSeries);
        trendDataset.addSeries(qpsSeries);
        trendDataset.addSeries(errorPercentSeries);
        ChartPanel chartPanel = new ChartPanel(trendChart);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setDisplayToolTips(true);
        chartPanel.setPreferredSize(new Dimension(300, 300));
        JPanel trendTopPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        trendTopPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_METRICS)));
        trendTopPanel.add(checkBoxPanel);
        add(trendTopPanel, BorderLayout.NORTH);
        add(chartPanel, BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {

    }

    private void updateYAxisLabel(XYPlot plot, boolean threadsSelected, boolean responseTimeSelected,
                                  boolean qpsSelected, boolean errorRateSelected) {
        int selectedCount = 0;
        if (threadsSelected) selectedCount++;
        if (responseTimeSelected) selectedCount++;
        if (qpsSelected) selectedCount++;
        if (errorRateSelected) selectedCount++;
        if (selectedCount == 0) {
            plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_NO_METRIC_SELECTED));
        } else if (selectedCount == 1) {
            if (threadsSelected) {
                plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS));
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
                }
            } else if (responseTimeSelected) {
                plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS));
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(null);
                }
            } else if (qpsSelected) {
                plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS));
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(null);
                }
            } else {
                plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));
                NumberFormat percentFormat = NumberFormat.getNumberInstance();
                percentFormat.setMaximumFractionDigits(2);
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(percentFormat);
                }
            }
        } else {
            plot.getRangeAxis().setLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_METRIC_VALUE));
            if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                numberAxis.setNumberFormatOverride(null);
            }
        }
    }

    public void clearTrendDataset() {
        userCountSeries.clear();
        responseTimeSeries.clear();
        qpsSeries.clear();
        errorPercentSeries.clear();
    }

    public void addOrUpdate(RegularTimePeriod period, double users,
                            double responseTime, double qps, double errorPercent) {
        if (period == null) return;
        userCountSeries.addOrUpdate(period, users);
        responseTimeSeries.addOrUpdate(period, responseTime);
        qpsSeries.addOrUpdate(period, qps);
        errorPercentSeries.addOrUpdate(period, errorPercent);
    }
}