package com.laker.postman.panel.performance.result;

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

    @Override
    protected void initUI() {
        setLayout(new GridLayout(2, 2, 10, 10));

        add(createChartPanel(userCountDataset, MessageKeys.PERFORMANCE_TREND_THREADS, Color.BLUE, true, false));
        add(createChartPanel(responseTimeDataset, MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS, Color.ORANGE, false, false));
        add(createChartPanel(qpsDataset, MessageKeys.PERFORMANCE_TREND_QPS, Color.GREEN.darker(), false, false));
        add(createChartPanel(errorPercentDataset, MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT, Color.RED, false, true));
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

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(194, 211, 236));
        plot.setRangeGridlinePaint(new Color(194, 211, 236));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, lineColor);
        plot.setRenderer(renderer);

        DateAxis dateAxis = new DateAxis(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME));
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        dateAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        dateAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        plot.setDomainAxis(dateAxis);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));
        rangeAxis.setLabelFont(FontsUtil.getDefaultFont(Font.PLAIN));

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
        panel.setBackground(Color.WHITE);
        panel.setDisplayToolTips(true);

        chart.getTitle().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));

        return panel;
    }

    @Override
    protected void registerListeners() {
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
            // 恢复通知并只触发一次更新（触发dataset的通知即可，无需逐个series通知）
            userCountSeries.setNotify(true);
            responseTimeSeries.setNotify(true);
            qpsSeries.setNotify(true);
            errorPercentSeries.setNotify(true);

            // 只触发一次dataset更新，减少重绘次数
            userCountDataset.setNotify(true);
            responseTimeDataset.setNotify(true);
            qpsDataset.setNotify(true);
            errorPercentDataset.setNotify(true);
        }
    }
}
