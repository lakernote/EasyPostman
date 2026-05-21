package com.laker.postman.panel.performance.result;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceTrendSnapshot;
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
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTrendPanel extends SingletonBasePanel {

    private final TimeSeries overviewUsersSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_THREADS));
    private final TimeSeries overviewSampleRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SAMPLE_RATE));
    private final TimeSeries overviewErrorRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));
    private final TimeSeries httpRateSeries = new TimeSeries(PerformanceProtocol.HTTP.getDisplayName());
    private final TimeSeries webSocketRateSeries = new TimeSeries(PerformanceProtocol.WEBSOCKET.getDisplayName());
    private final TimeSeries sseRateSeries = new TimeSeries(PerformanceProtocol.SSE.getDisplayName());

    private final TimeSeries httpRpsSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_QPS));
    private final TimeSeries httpAvgResponseSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RESPONSE_TIME_MS));
    private final TimeSeries httpErrorRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));

    private final TimeSeries wsActiveSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ACTIVE_WS));
    private final TimeSeries wsSentRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SENT_RATE));
    private final TimeSeries wsReceivedRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_RECEIVED_RATE));
    private final TimeSeries wsFirstMessageLatencySeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_FIRST_MESSAGE_LATENCY_MS));
    private final TimeSeries wsSessionDurationSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SESSION_DURATION_MS));
    private final TimeSeries wsErrorRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));

    private final TimeSeries sseActiveSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ACTIVE_SSE));
    private final TimeSeries sseEventRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_EVENT_RATE));
    private final TimeSeries sseMatchedRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_MATCHED_RATE));
    private final TimeSeries sseFirstEventLatencySeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_FIRST_EVENT_LATENCY_MS));
    private final TimeSeries sseStreamDurationSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_STREAM_DURATION_MS));
    private final TimeSeries sseErrorRateSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ERROR_RATE_PERCENT));

    private final List<TrendView> trendViews = new ArrayList<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        JTabbedPane protocolTabs = new JTabbedPane();
        protocolTabs.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_OVERVIEW), createOverviewPanel());
        protocolTabs.addTab(PerformanceProtocol.HTTP.getDisplayName(), createHttpPanel());
        protocolTabs.addTab(PerformanceProtocol.WEBSOCKET.getDisplayName(), createWebSocketPanel());
        protocolTabs.addTab(PerformanceProtocol.SSE.getDisplayName(), createSsePanel());
        add(protocolTabs, BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // Charts are updated by PerformanceStatisticsCoordinator.
    }

    private JPanel createOverviewPanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(overviewUsersSeries, getThreadsLineColor(), true),
                new SeriesSpec(overviewSampleRateSeries, getQpsLineColor(), true),
                new SeriesSpec(overviewErrorRateSeries, getErrorRateLineColor(), true),
                new SeriesSpec(httpRateSeries, getResponseTimeLineColor(), false),
                new SeriesSpec(webSocketRateSeries, getMatchedLineColor(), false),
                new SeriesSpec(sseRateSeries, getDurationLineColor(), false)
        );
    }

    private JPanel createHttpPanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(httpRpsSeries, getQpsLineColor(), true),
                new SeriesSpec(httpAvgResponseSeries, getResponseTimeLineColor(), true),
                new SeriesSpec(httpErrorRateSeries, getErrorRateLineColor(), true)
        );
    }

    private JPanel createWebSocketPanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(wsActiveSeries, getThreadsLineColor(), true),
                new SeriesSpec(wsSentRateSeries, getMatchedLineColor(), true),
                new SeriesSpec(wsReceivedRateSeries, getQpsLineColor(), true),
                new SeriesSpec(wsFirstMessageLatencySeries, getResponseTimeLineColor(), true),
                new SeriesSpec(wsSessionDurationSeries, getDurationLineColor(), true),
                new SeriesSpec(wsErrorRateSeries, getErrorRateLineColor(), true)
        );
    }

    private JPanel createSsePanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(sseActiveSeries, getThreadsLineColor(), true),
                new SeriesSpec(sseEventRateSeries, getQpsLineColor(), true),
                new SeriesSpec(sseMatchedRateSeries, getMatchedLineColor(), true),
                new SeriesSpec(sseFirstEventLatencySeries, getResponseTimeLineColor(), true),
                new SeriesSpec(sseStreamDurationSeries, getDurationLineColor(), true),
                new SeriesSpec(sseErrorRateSeries, getErrorRateLineColor(), true)
        );
    }

    private JPanel createTrendView(String titleKey, SeriesSpec... specs) {
        TrendView view = new TrendView(titleKey, specs);
        trendViews.add(view);
        return view.panel();
    }

    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String titleKey) {
        String title = I18nUtil.getMessage(titleKey);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME),
                title,
                dataset,
                true,
                true,
                false
        );
        chart.setBackgroundPaint(getChartBackgroundColor());
        chart.getTitle().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        chart.getTitle().setPaint(getTextColor());
        chart.getLegend().setItemFont(FontsUtil.getDefaultFont(Font.PLAIN));
        chart.getLegend().setItemPaint(getTextColor());
        chart.getLegend().setBackgroundPaint(getChartBackgroundColor());

        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(getChartBackgroundColor());
        plot.setDomainGridlinePaint(getGridLineColor());
        plot.setRangeGridlinePaint(getGridLineColor());
        plot.setOutlinePaint(getChartBorderColor());

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
        rangeAxis.setAutoRangeIncludesZero(true);

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setBackground(getChartPanelBackgroundColor());
        panel.setDisplayToolTips(true);
        return panel;
    }

    private XYLineAndShapeRenderer createTrendRenderer() {
        XYLineAndShapeRenderer renderer = new SinglePointAwareRenderer();
        renderer.setDefaultShape(new Ellipse2D.Double(-1.5, -1.5, 3.0, 3.0));
        renderer.setDefaultShapesFilled(true);
        renderer.setDrawOutlines(false);
        return renderer;
    }

    public void clearTrendDataset() {
        TimeSeries[] allSeries = {
                overviewUsersSeries, overviewSampleRateSeries, overviewErrorRateSeries,
                httpRateSeries, webSocketRateSeries, sseRateSeries,
                httpRpsSeries, httpAvgResponseSeries, httpErrorRateSeries,
                wsActiveSeries, wsSentRateSeries, wsReceivedRateSeries, wsFirstMessageLatencySeries,
                wsSessionDurationSeries, wsErrorRateSeries,
                sseActiveSeries, sseEventRateSeries, sseMatchedRateSeries, sseFirstEventLatencySeries,
                sseStreamDurationSeries, sseErrorRateSeries
        };
        for (TimeSeries series : allSeries) {
            series.clear();
        }
    }

    public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
        if (period == null || snapshot == null) {
            return;
        }

        overviewUsersSeries.addOrUpdate(period, snapshot.activeUsers());
        overviewSampleRateSeries.addOrUpdate(period, snapshot.overview().sampleRate());
        overviewErrorRateSeries.addOrUpdate(period, snapshot.overview().failurePercent());
        httpRateSeries.addOrUpdate(period, snapshot.http().sampleRate());
        webSocketRateSeries.addOrUpdate(period, snapshot.webSocket().sampleRate());
        sseRateSeries.addOrUpdate(period, snapshot.sse().sampleRate());

        httpRpsSeries.addOrUpdate(period, snapshot.http().sampleRate());
        httpAvgResponseSeries.addOrUpdate(period, snapshot.http().avgDurationMs());
        httpErrorRateSeries.addOrUpdate(period, snapshot.http().failurePercent());

        wsActiveSeries.addOrUpdate(period, snapshot.activeWebSocketConnections());
        wsSentRateSeries.addOrUpdate(period, snapshot.webSocket().sentRate());
        wsReceivedRateSeries.addOrUpdate(period, snapshot.webSocket().receivedRate());
        wsFirstMessageLatencySeries.addOrUpdate(period, snapshot.webSocket().avgFirstMessageLatencyMs());
        wsSessionDurationSeries.addOrUpdate(period, snapshot.webSocket().avgDurationMs());
        wsErrorRateSeries.addOrUpdate(period, snapshot.webSocket().failurePercent());

        sseActiveSeries.addOrUpdate(period, snapshot.activeSseStreams());
        sseEventRateSeries.addOrUpdate(period, snapshot.sse().receivedRate());
        sseMatchedRateSeries.addOrUpdate(period, snapshot.sse().matchedRate());
        sseFirstEventLatencySeries.addOrUpdate(period, snapshot.sse().avgFirstMessageLatencyMs());
        sseStreamDurationSeries.addOrUpdate(period, snapshot.sse().avgDurationMs());
        sseErrorRateSeries.addOrUpdate(period, snapshot.sse().failurePercent());
    }

    public void addOrUpdate(RegularTimePeriod period, double users,
                            double responseTime, double qps, double errorPercent) {
        if (period == null) {
            return;
        }
        overviewUsersSeries.addOrUpdate(period, users);
        overviewSampleRateSeries.addOrUpdate(period, qps);
        overviewErrorRateSeries.addOrUpdate(period, errorPercent);
        httpRpsSeries.addOrUpdate(period, qps);
        httpAvgResponseSeries.addOrUpdate(period, responseTime);
        httpErrorRateSeries.addOrUpdate(period, errorPercent);
    }

    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    private Color getChartBackgroundColor() {
        return isDarkTheme() ? new Color(43, 43, 43) : Color.WHITE;
    }

    private Color getChartPanelBackgroundColor() {
        return isDarkTheme() ? new Color(43, 43, 43) : Color.WHITE;
    }

    private Color getGridLineColor() {
        return isDarkTheme() ? new Color(90, 90, 90) : new Color(194, 211, 236);
    }

    private Color getTextColor() {
        return isDarkTheme() ? new Color(200, 200, 200) : Color.BLACK;
    }

    private Color getChartBorderColor() {
        return isDarkTheme() ? new Color(90, 90, 90) : new Color(194, 211, 236);
    }

    private Color getThreadsLineColor() {
        return isDarkTheme() ? new Color(100, 181, 246) : new Color(33, 150, 243);
    }

    private Color getResponseTimeLineColor() {
        return isDarkTheme() ? new Color(255, 183, 77) : new Color(255, 152, 0);
    }

    private Color getQpsLineColor() {
        return isDarkTheme() ? new Color(129, 199, 132) : new Color(56, 142, 60);
    }

    private Color getMatchedLineColor() {
        return isDarkTheme() ? new Color(186, 104, 200) : new Color(142, 36, 170);
    }

    private Color getDurationLineColor() {
        return isDarkTheme() ? new Color(77, 208, 225) : new Color(0, 137, 123);
    }

    private Color getErrorRateLineColor() {
        return isDarkTheme() ? new Color(239, 83, 80) : new Color(211, 47, 47);
    }

    private final class TrendView {
        private final JPanel panel;
        private final TimeSeriesCollection dataset = new TimeSeriesCollection();
        private final ChartPanel chartPanel;
        private final List<SeriesControl> controls = new ArrayList<>();

        private TrendView(String titleKey, SeriesSpec... specs) {
            panel = new JPanel(new BorderLayout(8, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 10, 10));
            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            for (SeriesSpec spec : specs) {
                JCheckBox checkBox = new JCheckBox(spec.series().getKey().toString(), spec.selected());
                checkBox.addActionListener(e -> {
                    if (selectedCount() == 0) {
                        checkBox.setSelected(true);
                    }
                    rebuildDataset();
                });
                controls.add(new SeriesControl(spec, checkBox));
                controlsPanel.add(checkBox);
            }
            chartPanel = createChartPanel(dataset, titleKey);
            panel.add(controlsPanel, BorderLayout.NORTH);
            panel.add(chartPanel, BorderLayout.CENTER);
            rebuildDataset();
        }

        private JPanel panel() {
            return panel;
        }

        private int selectedCount() {
            int count = 0;
            for (SeriesControl control : controls) {
                if (control.checkBox().isSelected()) {
                    count++;
                }
            }
            return count;
        }

        private void rebuildDataset() {
            dataset.removeAllSeries();
            XYLineAndShapeRenderer renderer = createTrendRenderer();
            int visibleIndex = 0;
            for (SeriesControl control : controls) {
                if (!control.checkBox().isSelected()) {
                    continue;
                }
                dataset.addSeries(control.spec().series());
                renderer.setSeriesPaint(visibleIndex++, control.spec().color());
            }
            chartPanel.getChart().getXYPlot().setRenderer(renderer);
            chartPanel.repaint();
        }
    }

    private record SeriesSpec(TimeSeries series, Color color, boolean selected) {
    }

    private record SeriesControl(SeriesSpec spec, JCheckBox checkBox) {
    }

    private static final class SinglePointAwareRenderer extends XYLineAndShapeRenderer {
        private SinglePointAwareRenderer() {
            super(true, false);
        }

        @Override
        public boolean getItemShapeVisible(int series, int item) {
            XYDataset dataset = null;
            if (getPlot() != null) {
                dataset = getPlot().getDataset();
            }
            return dataset != null && dataset.getItemCount(series) == 1;
        }
    }
}
