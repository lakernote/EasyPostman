package com.laker.postman.panel.performance.result;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

public class PerformanceTrendPanel extends SingletonBasePanel {

    private final TimeSeries userCountSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS));
    private final TimeSeries responseTimeSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS));
    private final TimeSeries qpsSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS));
    private final TimeSeries errorPercentSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));

    private final TimeSeriesCollection userCountDataset = new TimeSeriesCollection(userCountSeries);
    private final TimeSeriesCollection responseTimeDataset = new TimeSeriesCollection(responseTimeSeries);
    private final TimeSeriesCollection qpsDataset = new TimeSeriesCollection(qpsSeries);
    private final TimeSeriesCollection errorPercentDataset = new TimeSeriesCollection(errorPercentSeries);


    private boolean isCombinedView = false;
    private JPanel chartContainer;
    private JButton toggleButton;

    // 缓存图表面板，避免切换时重复创建
    private JPanel separateChartsPanel;
    private ChartPanel combinedChartPanel;

    // 合并视图的指标选择复选框
    private JCheckBox threadsCheckBox;
    private JCheckBox responseTimeCheckBox;
    private JCheckBox qpsCheckBox;
    private JCheckBox errorRateCheckBox;

    // 日期格式化器（实例变量，避免线程安全问题）
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的图表背景色
     */
    private Color getChartBackgroundColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : Color.WHITE;
    }

    /**
     * 获取主题适配的图表面板背景色
     */
    private Color getChartPanelBackgroundColor() {
        return isDarkTheme() ? new Color(60, 63, 65) : Color.WHITE;
    }

    /**
     * 获取主题适配的网格线颜色
     */
    private Color getGridLineColor() {
        return isDarkTheme() ? new Color(80, 83, 85) : new Color(194, 211, 236);
    }

    /**
     * 获取主题适配的文本颜色
     */
    private Color getTextColor() {
        return isDarkTheme() ? new Color(187, 187, 187) : Color.BLACK;
    }

    /**
     * 获取主题适配的边框颜色
     */
    private Color getChartBorderColor() {
        return isDarkTheme() ? new Color(80, 83, 85) : new Color(194, 211, 236);
    }


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());


        // 先创建复选框（默认全选）- 必须在创建图表面板之前
        threadsCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS), true);
        responseTimeCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS), false);
        qpsCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS), true);
        errorRateCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT), true);

        // 预创建图表面板（避免切换时重复创建）
        separateChartsPanel = createSeparateChartsPanel();
        combinedChartPanel = createCombinedChartPanel();

        // 为复选框添加监听器，动态更新合并图表
        threadsCheckBox.addActionListener(e -> updateCombinedChart());
        responseTimeCheckBox.addActionListener(e -> updateCombinedChart());
        qpsCheckBox.addActionListener(e -> updateCombinedChart());
        errorRateCheckBox.addActionListener(e -> updateCombinedChart());

        // Create toggle button
        toggleButton = new JButton(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_COMBINED_CHART));
        toggleButton.addActionListener(e -> toggleChartView());

        // 创建顶部面板（包含切换按钮和复选框）
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(threadsCheckBox);
        topPanel.add(responseTimeCheckBox);
        topPanel.add(qpsCheckBox);
        topPanel.add(errorRateCheckBox);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(toggleButton);
        add(topPanel, BorderLayout.NORTH);

        // 初始时隐藏复选框（仅在合并视图时显示）
        threadsCheckBox.setVisible(false);
        responseTimeCheckBox.setVisible(false);
        qpsCheckBox.setVisible(false);
        errorRateCheckBox.setVisible(false);

        // Create chart container
        chartContainer = new JPanel(new BorderLayout());
        add(chartContainer, BorderLayout.CENTER);

        // Show separate charts by default
        showSeparateCharts();
    }

    /**
     * 创建单个图表面板
     *
     * @param dataset       数据集
     * @param titleKey      图表标题的国际化key
     * @param lineColor     曲线颜色
     * @param integerFormat 是否整数格式
     * @param percentFormat 是否百分比格式
     */
    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String titleKey, Color lineColor,
                                        boolean integerFormat, boolean percentFormat) {
        String title = I18nUtil.getMessage(titleKey);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME),
                title,
                dataset,
                false,
                true,
                false
        );

        // 设置图表背景色（主题适配）
        chart.setBackgroundPaint(getChartBackgroundColor());

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(getChartBackgroundColor());
        plot.setDomainGridlinePaint(getGridLineColor());
        plot.setRangeGridlinePaint(getGridLineColor());
        plot.setOutlinePaint(getChartBorderColor());

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, lineColor);
        plot.setRenderer(renderer);

        DateAxis dateAxis = new DateAxis(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME));
        dateAxis.setDateFormatOverride(timeFormat);
        dateAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        dateAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        dateAxis.setTickLabelPaint(getTextColor());
        dateAxis.setLabelPaint(getTextColor());
        plot.setDomainAxis(dateAxis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        rangeAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        rangeAxis.setTickLabelPaint(getTextColor());
        rangeAxis.setLabelPaint(getTextColor());

        // 设置Y轴上边距，避免曲线贴到顶部
        rangeAxis.setUpperMargin(0.2);

        if (integerFormat) {
            rangeAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
            rangeAxis.setAutoRangeIncludesZero(true);
        } else if (percentFormat) {
            NumberFormat percent = NumberFormat.getNumberInstance();
            percent.setMaximumFractionDigits(2);
            rangeAxis.setNumberFormatOverride(percent);
            rangeAxis.setAutoRangeIncludesZero(true);
        } else {
            rangeAxis.setNumberFormatOverride(null);
            rangeAxis.setAutoRangeIncludesZero(false);
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(400, 300));
        panel.setBackground(getChartPanelBackgroundColor());
        panel.setDisplayToolTips(true);

        chart.getTitle().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        chart.getTitle().setPaint(getTextColor());

        return panel;
    }

    /**
     * 创建合并图表面板（所有指标在一个图表中）
     */
    private ChartPanel createCombinedChartPanel() {
        // 根据复选框状态创建dataset
        TimeSeriesCollection dataset = createDynamicDataset();

        String title = I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_METRICS);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_METRICS),
                dataset,
                true,  // 显示图例
                true,
                false
        );

        // 设置图表背景色（主题适配）
        chart.setBackgroundPaint(getChartBackgroundColor());

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(getChartBackgroundColor());
        plot.setDomainGridlinePaint(getGridLineColor());
        plot.setRangeGridlinePaint(getGridLineColor());
        plot.setOutlinePaint(getChartBorderColor());

        // 设置渲染器颜色
        plot.setRenderer(createCombinedChartRenderer());

        DateAxis dateAxis = new DateAxis(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME));
        dateAxis.setDateFormatOverride(timeFormat);
        dateAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        dateAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        dateAxis.setTickLabelPaint(getTextColor());
        dateAxis.setLabelPaint(getTextColor());
        plot.setDomainAxis(dateAxis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        rangeAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        rangeAxis.setTickLabelPaint(getTextColor());
        rangeAxis.setLabelPaint(getTextColor());
        rangeAxis.setUpperMargin(0.2);
        rangeAxis.setAutoRangeIncludesZero(false);

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setBackground(getChartPanelBackgroundColor());
        panel.setDisplayToolTips(true);

        chart.getTitle().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        chart.getTitle().setPaint(getTextColor());
        chart.getLegend().setItemFont(FontsUtil.getDefaultFont(Font.PLAIN));
        chart.getLegend().setItemPaint(getTextColor());
        chart.getLegend().setBackgroundPaint(getChartBackgroundColor());

        return panel;
    }

    /**
     * 创建合并图表的渲染器，根据复选框状态设置颜色
     */
    private XYLineAndShapeRenderer createCombinedChartRenderer() {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        int seriesIndex = 0;
        if (threadsCheckBox.isSelected()) {
            renderer.setSeriesPaint(seriesIndex++, Color.BLUE);
        }
        if (responseTimeCheckBox.isSelected()) {
            renderer.setSeriesPaint(seriesIndex++, Color.ORANGE);
        }
        if (qpsCheckBox.isSelected()) {
            renderer.setSeriesPaint(seriesIndex++, Color.GREEN.darker());
        }
        if (errorRateCheckBox.isSelected()) {
            renderer.setSeriesPaint(seriesIndex, Color.RED);
        }
        return renderer;
    }

    /**
     * 根据复选框状态创建动态数据集
     */
    private TimeSeriesCollection createDynamicDataset() {
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        if (threadsCheckBox.isSelected()) {
            dataset.addSeries(userCountSeries);
        }
        if (responseTimeCheckBox.isSelected()) {
            dataset.addSeries(responseTimeSeries);
        }
        if (qpsCheckBox.isSelected()) {
            dataset.addSeries(qpsSeries);
        }
        if (errorRateCheckBox.isSelected()) {
            dataset.addSeries(errorPercentSeries);
        }
        return dataset;
    }

    /**
     * 更新合并图表（当复选框状态改变时）
     */
    private void updateCombinedChart() {
        if (!isCombinedView || combinedChartPanel == null) {
            return;
        }

        JFreeChart chart = combinedChartPanel.getChart();
        if (chart == null) {
            return;
        }

        XYPlot plot = chart.getXYPlot();

        // 更新数据集
        TimeSeriesCollection newDataset = createDynamicDataset();
        plot.setDataset(newDataset);

        // 更新渲染器颜色（复用提取的方法）
        plot.setRenderer(createCombinedChartRenderer());

        // 刷新图表
        combinedChartPanel.repaint();
    }

    /**
     * 创建分离图表面板（4个独立图表）
     */
    private JPanel createSeparateChartsPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));

        panel.add(createChartPanel(userCountDataset, MessageKeys.PERFORMANCE_TREND_THREADS, Color.BLUE, true, false));
        panel.add(createChartPanel(responseTimeDataset, MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS, Color.ORANGE, false, false));
        panel.add(createChartPanel(qpsDataset, MessageKeys.PERFORMANCE_TREND_QPS, Color.GREEN.darker(), false, false));
        panel.add(createChartPanel(errorPercentDataset, MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT, Color.RED, false, true));

        return panel;
    }

    /**
     * 切换图表视图模式
     */
    private void toggleChartView() {
        isCombinedView = !isCombinedView;

        if (isCombinedView) {
            // 显示复选框
            threadsCheckBox.setVisible(true);
            responseTimeCheckBox.setVisible(true);
            qpsCheckBox.setVisible(true);
            errorRateCheckBox.setVisible(true);

            showCombinedChart();
            toggleButton.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS));
        } else {
            // 隐藏复选框
            threadsCheckBox.setVisible(false);
            responseTimeCheckBox.setVisible(false);
            qpsCheckBox.setVisible(false);
            errorRateCheckBox.setVisible(false);

            showSeparateCharts();
            toggleButton.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_COMBINED_CHART));
        }
    }

    /**
     * 显示4个分离的图表
     */
    private void showSeparateCharts() {
        chartContainer.removeAll();
        chartContainer.add(separateChartsPanel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    /**
     * 显示1个合并的图表
     */
    private void showCombinedChart() {
        chartContainer.removeAll();
        chartContainer.add(combinedChartPanel, BorderLayout.CENTER);
        chartContainer.revalidate();
        chartContainer.repaint();
    }

    @Override
    protected void registerListeners() {
        // 监听器已在initUI中通过toggleButton.addActionListener注册
        // 无需额外的监听器注册
    }


    public void clearTrendDataset() {
        userCountSeries.clear();
        responseTimeSeries.clear();
        qpsSeries.clear();
        errorPercentSeries.clear();
    }

    /**
     * 增加或更新指标数据（批量更新优化性能）
     *
     * @param period       时间点
     * @param users        用户数
     * @param responseTime 响应时间
     * @param qps          QPS
     * @param errorPercent 错误率
     */
    public void addOrUpdate(RegularTimePeriod period, double users,
                            double responseTime, double qps, double errorPercent) {
        if (period == null) return;

        // 批量更新：暂时禁用通知，避免每次 addOrUpdate 都触发重绘
        userCountSeries.setNotify(false);
        responseTimeSeries.setNotify(false);
        qpsSeries.setNotify(false);
        errorPercentSeries.setNotify(false);

        try {
            userCountSeries.addOrUpdate(period, users);
            responseTimeSeries.addOrUpdate(period, responseTime);
            qpsSeries.addOrUpdate(period, qps);
            errorPercentSeries.addOrUpdate(period, errorPercent);
        } finally {
            // 恢复通知
            userCountSeries.setNotify(true);
            responseTimeSeries.setNotify(true);
            qpsSeries.setNotify(true);
            errorPercentSeries.setNotify(true);

            // 手动触发所有图表更新
            // 因为有4个独立的dataset，需要触发每个series的更新
            userCountSeries.fireSeriesChanged();
            responseTimeSeries.fireSeriesChanged();
            qpsSeries.fireSeriesChanged();
            errorPercentSeries.fireSeriesChanged();
        }
    }
}
