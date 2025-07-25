package com.laker.postman.panel.jmeter.result;

import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.util.FontUtil;
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
    private final TimeSeries userCountSeries = new TimeSeries("Threads");
    private final TimeSeries responseTimeSeries = new TimeSeries("Response Time(ms)");
    private final TimeSeries qpsSeries = new TimeSeries("QPS");
    private final TimeSeries errorPercentSeries = new TimeSeries("Error Rate(%)");

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        JPanel checkBoxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JCheckBox threadsCheckBox = new JCheckBox("Threads", true);
        JCheckBox responseTimeCheckBox = new JCheckBox("Response Time", true);
        JCheckBox qpsCheckBox = new JCheckBox("QPS", true);
        JCheckBox errorRateCheckBox = new JCheckBox("Error Rate", true);
        checkBoxPanel.add(threadsCheckBox);
        checkBoxPanel.add(responseTimeCheckBox);
        checkBoxPanel.add(qpsCheckBox);
        checkBoxPanel.add(errorRateCheckBox);
        JFreeChart trendChart = ChartFactory.createTimeSeriesChart(
                "API Performance Trend",
                "Time",
                "Metric Value",
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
        trendChart.getTitle().setFont(FontUtil.getDefaultFont(Font.PLAIN, 13));
        if (trendChart.getLegend() != null) trendChart.getLegend().setItemFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getDomainAxis().setTickLabelFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
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
        trendTopPanel.add(new JLabel("Metrics:"));
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
            plot.getRangeAxis().setLabel("No Metric Selected");
        } else if (selectedCount == 1) {
            if (threadsSelected) {
                plot.getRangeAxis().setLabel("Threads");
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
                }
            } else if (responseTimeSelected) {
                plot.getRangeAxis().setLabel("Response Time (ms)");
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(null);
                }
            } else if (qpsSelected) {
                plot.getRangeAxis().setLabel("QPS");
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(null);
                }
            } else {
                plot.getRangeAxis().setLabel("Error Rate (%)");
                NumberFormat percentFormat = NumberFormat.getNumberInstance();
                percentFormat.setMaximumFractionDigits(2);
                if (plot.getRangeAxis() instanceof NumberAxis numberAxis) {
                    numberAxis.setNumberFormatOverride(percentFormat);
                }
            }
        } else {
            plot.getRangeAxis().setLabel("Metric Value");
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