package com.laker.postman.panel.performance.result;

import com.laker.postman.common.SingletonBasePanel;
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

    // 四个指标的时间序列
    private final TimeSeries userCountSeries = new TimeSeries("用户数");
    private final TimeSeries responseTimeSeries = new TimeSeries("响应时间(ms)");
    private final TimeSeries qpsSeries = new TimeSeries("QPS");
    private final TimeSeries errorPercentSeries = new TimeSeries("错误率(%)");

    // 对应的数据集
    private final TimeSeriesCollection userCountDataset = new TimeSeriesCollection(userCountSeries);
    private final TimeSeriesCollection responseTimeDataset = new TimeSeriesCollection(responseTimeSeries);
    private final TimeSeriesCollection qpsDataset = new TimeSeriesCollection(qpsSeries);
    private final TimeSeriesCollection errorPercentDataset = new TimeSeriesCollection(errorPercentSeries);

    private final Font fontTitle = new Font("微软雅黑", Font.BOLD, 14);
    private final Font fontLabel = new Font("微软雅黑", Font.PLAIN, 12);

    @Override
    protected void initUI() {
        setLayout(new GridLayout(2, 2, 10, 10)); // 2行2列显示四个图

        add(createChartPanel(userCountDataset, "用户数", Color.BLUE, true, false));
        add(createChartPanel(responseTimeDataset, "响应时间(ms)", Color.ORANGE, false, false));
        add(createChartPanel(qpsDataset, "QPS(s)", Color.GREEN.darker(), false, false));
        add(createChartPanel(errorPercentDataset, "错误率(%)", Color.RED, false, true));
    }

    /**
     * 创建单个图表面板
     *
     * @param dataset       数据集
     * @param title         图表标题
     * @param lineColor     曲线颜色
     * @param integerFormat 是否整数格式
     * @param percentFormat 是否百分比格式
     */
    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String title, Color lineColor,
                                        boolean integerFormat, boolean percentFormat) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,       // 图表标题
                "时间",      // X轴标题
                title,       // Y轴标题
                dataset,
                false,       // 图例
                true,        // 提示
                false
        );

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(194, 211, 236));
        plot.setRangeGridlinePaint(new Color(194, 211, 236));

        // 渲染器
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, lineColor);
        plot.setRenderer(renderer);

        // X轴时间格式
        DateAxis dateAxis = new DateAxis("时间");
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        dateAxis.setTickLabelFont(fontLabel);
        dateAxis.setLabelFont(fontLabel);
        plot.setDomainAxis(dateAxis);

        // Y轴格式
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(fontLabel);
        rangeAxis.setLabelFont(fontLabel);

        if (integerFormat) {
            rangeAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
            rangeAxis.setAutoRangeIncludesZero(true); // 从0开始
        } else if (percentFormat) {
            NumberFormat percent = NumberFormat.getNumberInstance();
            percent.setMaximumFractionDigits(2);
            rangeAxis.setNumberFormatOverride(percent);
            rangeAxis.setAutoRangeIncludesZero(true); // 从0开始
        } else {
            rangeAxis.setNumberFormatOverride(null);
            rangeAxis.setAutoRangeIncludesZero(false); // 响应时间、QPS允许非零起点
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(400, 300));
        panel.setBackground(Color.WHITE);
        panel.setDisplayToolTips(true);

        // 标题字体
        chart.getTitle().setFont(fontTitle);

        return panel;
    }

    @Override
    protected void registerListeners() {
        // 可扩展动态显示/隐藏图表
    }

    /**
     * 清空所有数据
     */
    public void clearTrendDataset() {
        userCountSeries.clear();
        responseTimeSeries.clear();
        qpsSeries.clear();
        errorPercentSeries.clear();
    }

    /**
     * 增加或更新指标数据
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

        userCountSeries.addOrUpdate(period, users);
        responseTimeSeries.addOrUpdate(period, responseTime);
        qpsSeries.addOrUpdate(period, qps);
        errorPercentSeries.addOrUpdate(period, errorPercent);
    }
}
