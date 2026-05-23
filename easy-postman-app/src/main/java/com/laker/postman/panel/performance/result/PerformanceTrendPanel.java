package com.laker.postman.panel.performance.result;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.common.component.button.SegmentedToggleButton;
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
import org.jfree.data.time.DateRange;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class PerformanceTrendPanel extends SingletonBasePanel {

    private static final String SEPARATE_VIEW = "separate";
    private static final String COMBINED_VIEW = "combined";
    private static final int MAX_TREND_POINTS = 3_600;
    private static final long EMPTY_DOMAIN_WINDOW_MS = 60_000L;
    private static final String JFREE_CHART_BUNDLE = "org.jfree.chart.LocalizationBundle";
    private static final String SAVE_AS_PNG_COMMAND = "SAVE_AS_PNG";
    private static final String SAVE_AS_SVG_COMMAND = "SAVE_AS_SVG";
    private static final String SAVE_AS_PDF_COMMAND = "SAVE_AS_PDF";
    private static final Map<String, String> CHART_POPUP_ACTION_KEYS = Map.ofEntries(
            Map.entry(ChartPanel.PROPERTIES_COMMAND, "Properties..."),
            Map.entry(ChartPanel.COPY_COMMAND, "Copy"),
            Map.entry(ChartPanel.SAVE_COMMAND, "Save_as..."),
            Map.entry(SAVE_AS_PNG_COMMAND, "PNG..."),
            Map.entry(SAVE_AS_SVG_COMMAND, "SVG..."),
            Map.entry(SAVE_AS_PDF_COMMAND, "PDF..."),
            Map.entry(ChartPanel.PRINT_COMMAND, "Print..."),
            Map.entry(ChartPanel.ZOOM_IN_BOTH_COMMAND, "All_Axes"),
            Map.entry(ChartPanel.ZOOM_IN_DOMAIN_COMMAND, "Domain_Axis"),
            Map.entry(ChartPanel.ZOOM_IN_RANGE_COMMAND, "Range_Axis"),
            Map.entry(ChartPanel.ZOOM_OUT_BOTH_COMMAND, "All_Axes"),
            Map.entry(ChartPanel.ZOOM_OUT_DOMAIN_COMMAND, "Domain_Axis"),
            Map.entry(ChartPanel.ZOOM_OUT_RANGE_COMMAND, "Range_Axis"),
            Map.entry(ChartPanel.ZOOM_RESET_BOTH_COMMAND, "All_Axes"),
            Map.entry(ChartPanel.ZOOM_RESET_DOMAIN_COMMAND, "Domain_Axis"),
            Map.entry(ChartPanel.ZOOM_RESET_RANGE_COMMAND, "Range_Axis")
    );

    private final TimeSeries httpVirtualUsersSeries = new TimeSeries(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_VIRTUAL_USERS));
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
    private String chartMode = SEPARATE_VIEW;

    @Override
    protected void initUI() {
        configureSeriesRetention();
        setLayout(new BorderLayout());
        JPanel protocolCards = new JPanel(new CardLayout());
        protocolCards.add(createHttpPanel(), PerformanceProtocol.HTTP.name());
        protocolCards.add(createWebSocketPanel(), PerformanceProtocol.WEBSOCKET.name());
        protocolCards.add(createSsePanel(), PerformanceProtocol.SSE.name());
        add(createToolbar(protocolCards), BorderLayout.NORTH);
        add(protocolCards, BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // Charts are updated by PerformanceStatisticsCoordinator.
    }

    private void configureSeriesRetention() {
        for (TimeSeries series : allSeries()) {
            series.setMaximumItemCount(MAX_TREND_POINTS);
        }
    }

    private JPanel createHttpPanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(httpVirtualUsersSeries, getThreadsLineColor(), true, AxisFormat.INTEGER),
                new SeriesSpec(httpRpsSeries, getQpsLineColor(), true, AxisFormat.DECIMAL),
                new SeriesSpec(httpAvgResponseSeries, getResponseTimeLineColor(), true, AxisFormat.DECIMAL),
                new SeriesSpec(httpErrorRateSeries, getErrorRateLineColor(), true, AxisFormat.DECIMAL)
        );
    }

    private JPanel createWebSocketPanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(wsActiveSeries, getThreadsLineColor(), true, AxisFormat.INTEGER),
                new SeriesSpec(wsSentRateSeries, getMatchedLineColor(), true, AxisFormat.DECIMAL),
                new SeriesSpec(wsReceivedRateSeries, getQpsLineColor(), true, AxisFormat.DECIMAL),
                new SeriesSpec(wsErrorRateSeries, getErrorRateLineColor(), true, AxisFormat.DECIMAL)
        );
    }

    private JPanel createSsePanel() {
        return createTrendView(
                MessageKeys.PERFORMANCE_TREND_METRICS,
                new SeriesSpec(sseActiveSeries, getThreadsLineColor(), true, AxisFormat.INTEGER),
                new SeriesSpec(sseEventRateSeries, getQpsLineColor(), true, AxisFormat.DECIMAL),
                new SeriesSpec(sseErrorRateSeries, getErrorRateLineColor(), true, AxisFormat.DECIMAL)
        );
    }

    private JPanel createTrendView(String titleKey, SeriesSpec... specs) {
        TrendView view = new TrendView(titleKey, specs);
        trendViews.add(view);
        return view.panel();
    }

    private JPanel createToolbar(JPanel protocolCards) {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        toolbar.add(createProtocolSwitcher(protocolCards), BorderLayout.WEST);
        toolbar.add(createModeSwitcher(), BorderLayout.EAST);
        return toolbar;
    }

    private JPanel createProtocolSwitcher(JPanel protocolCards) {
        ButtonGroup protocolGroup = new ButtonGroup();
        JPanel switcher = new SegmentedButtonGroupPanel(FlowLayout.LEFT);
        for (PerformanceProtocol protocol : PerformanceProtocol.values()) {
            JToggleButton button = new SegmentedToggleButton(
                    protocol.getDisplayName(),
                    protocol == PerformanceProtocol.HTTP
            );
            button.addActionListener(e -> {
                CardLayout layout = (CardLayout) protocolCards.getLayout();
                layout.show(protocolCards, protocol.name());
            });
            protocolGroup.add(button);
            switcher.add(button);
        }
        return switcher;
    }

    private JPanel createModeSwitcher() {
        JToggleButton separateButton = new SegmentedToggleButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS),
                true
        );
        JToggleButton combinedButton = new SegmentedToggleButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_COMBINED_CHART),
                false
        );
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(separateButton);
        modeGroup.add(combinedButton);
        separateButton.addActionListener(e -> showChartMode(SEPARATE_VIEW));
        combinedButton.addActionListener(e -> showChartMode(COMBINED_VIEW));

        JPanel modePanel = new SegmentedButtonGroupPanel(FlowLayout.RIGHT);
        modePanel.add(separateButton);
        modePanel.add(combinedButton);
        return modePanel;
    }

    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String titleKey) {
        return createChartPanel(dataset, I18nUtil.getMessage(titleKey), true);
    }

    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String title, boolean legend) {
        return createChartPanel(dataset, title, legend, AxisFormat.DECIMAL);
    }

    private ChartPanel createChartPanel(TimeSeriesCollection dataset, String title, boolean legend, AxisFormat axisFormat) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                title,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_TIME),
                title,
                dataset,
                legend,
                true,
                false
        );
        chart.setBackgroundPaint(getChartBackgroundColor());
        chart.getTitle().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 0));
        chart.getTitle().setPaint(getTextColor());
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(FontsUtil.getDefaultFont(Font.PLAIN));
            chart.getLegend().setItemPaint(getTextColor());
            chart.getLegend().setBackgroundPaint(getChartBackgroundColor());
        }

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
        rangeAxis.setAutoRangeMinimumSize(1.0);
        rangeAxis.setUpperMargin(0.2);
        rangeAxis.setAutoRangeIncludesZero(true);
        if (axisFormat == AxisFormat.INTEGER) {
            rangeAxis.setNumberFormatOverride(NumberFormat.getIntegerInstance());
        }

        ChartPanel panel = new ChartPanel(chart);
        panel.setMouseWheelEnabled(true);
        panel.setBackground(getChartPanelBackgroundColor());
        panel.setDisplayToolTips(true);
        panel.setMinimumDrawWidth(0);
        panel.setMinimumDrawHeight(0);
        panel.setMaximumDrawWidth(Integer.MAX_VALUE);
        panel.setMaximumDrawHeight(Integer.MAX_VALUE);
        installLocalizedChartPopupMenu(panel);
        return panel;
    }

    private static void installLocalizedChartPopupMenu(ChartPanel panel) {
        JPopupMenu popupMenu = panel.getPopupMenu();
        if (popupMenu == null) {
            return;
        }
        localizeChartPopupMenu(popupMenu);
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                localizeChartPopupMenu(popupMenu);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    private static void localizeChartPopupMenu(JPopupMenu popupMenu) {
        ResourceBundle bundle = chartPopupResourceBundle();
        popupMenu.setLabel(chartPopupText(bundle, "Chart") + ":");
        for (Component component : popupMenu.getComponents()) {
            localizeChartPopupComponent(component, bundle);
        }
    }

    private static void localizeChartPopupComponent(Component component, ResourceBundle bundle) {
        if (component instanceof JMenu menu) {
            String menuKey = resolveChartPopupMenuKey(menu);
            if (menuKey != null) {
                menu.setText(chartPopupText(bundle, menuKey));
            }
            for (Component child : menu.getMenuComponents()) {
                localizeChartPopupComponent(child, bundle);
            }
            return;
        }

        if (component instanceof JMenuItem menuItem) {
            String key = CHART_POPUP_ACTION_KEYS.get(menuItem.getActionCommand());
            if (key != null) {
                menuItem.setText(chartPopupText(bundle, key));
            }
        }
    }

    private static String resolveChartPopupMenuKey(JMenu menu) {
        if (containsChartPopupCommand(menu, ChartPanel.SAVE_COMMAND, SAVE_AS_PNG_COMMAND,
                SAVE_AS_SVG_COMMAND, SAVE_AS_PDF_COMMAND)) {
            return "Save_as";
        }
        if (containsChartPopupCommand(menu, ChartPanel.ZOOM_IN_BOTH_COMMAND,
                ChartPanel.ZOOM_IN_DOMAIN_COMMAND, ChartPanel.ZOOM_IN_RANGE_COMMAND)) {
            return "Zoom_In";
        }
        if (containsChartPopupCommand(menu, ChartPanel.ZOOM_OUT_BOTH_COMMAND,
                ChartPanel.ZOOM_OUT_DOMAIN_COMMAND, ChartPanel.ZOOM_OUT_RANGE_COMMAND)) {
            return "Zoom_Out";
        }
        if (containsChartPopupCommand(menu, ChartPanel.ZOOM_RESET_BOTH_COMMAND,
                ChartPanel.ZOOM_RESET_DOMAIN_COMMAND, ChartPanel.ZOOM_RESET_RANGE_COMMAND)) {
            return "Auto_Range";
        }
        return null;
    }

    private static boolean containsChartPopupCommand(JMenu menu, String... commands) {
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenu childMenu && containsChartPopupCommand(childMenu, commands)) {
                return true;
            }
            if (component instanceof JMenuItem menuItem && matchesCommand(menuItem.getActionCommand(), commands)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCommand(String actionCommand, String... commands) {
        for (String command : commands) {
            if (command.equals(actionCommand)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceBundle chartPopupResourceBundle() {
        Locale locale = I18nUtil.isChinese() ? Locale.SIMPLIFIED_CHINESE : Locale.ENGLISH;
        return ResourceBundle.getBundle(JFREE_CHART_BUNDLE, locale);
    }

    private static String chartPopupText(ResourceBundle bundle, String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    private XYLineAndShapeRenderer createTrendRenderer() {
        XYLineAndShapeRenderer renderer = new SinglePointAwareRenderer();
        renderer.setDefaultShape(new Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
        renderer.setDefaultShapesFilled(true);
        renderer.setDrawOutlines(false);
        return renderer;
    }

    public void clearTrendDataset() {
        long resetTimeMs = System.currentTimeMillis();
        for (TimeSeries series : allSeries()) {
            series.clear();
        }
        for (TrendView trendView : trendViews) {
            trendView.resetAxes(resetTimeMs);
        }
    }

    private static void resetAxes(ChartPanel chartPanel, long resetTimeMs) {
        if (chartPanel == null || chartPanel.getChart() == null) {
            return;
        }
        XYPlot plot = chartPanel.getChart().getXYPlot();
        if (plot.getDomainAxis() instanceof DateAxis dateAxis) {
            DateRange resetRange = new DateRange(
                    new Date(Math.max(0, resetTimeMs - EMPTY_DOMAIN_WINDOW_MS)),
                    new Date(resetTimeMs)
            );
            dateAxis.setAutoRange(true);
            dateAxis.setRange(resetRange, false, true);
        }
        if (plot.getRangeAxis() != null) {
            plot.getRangeAxis().setAutoRange(true);
        }
    }

    private TimeSeries[] allSeries() {
        return new TimeSeries[]{
                httpVirtualUsersSeries, httpRpsSeries, httpAvgResponseSeries, httpErrorRateSeries,
                wsActiveSeries, wsSentRateSeries, wsReceivedRateSeries, wsFirstMessageLatencySeries,
                wsSessionDurationSeries, wsErrorRateSeries,
                sseActiveSeries, sseEventRateSeries, sseMatchedRateSeries, sseFirstEventLatencySeries,
                sseStreamDurationSeries, sseErrorRateSeries
        };
    }

    public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
        if (period == null || snapshot == null) {
            return;
        }

        httpVirtualUsersSeries.addOrUpdate(period, snapshot.activeUsers());
        httpRpsSeries.addOrUpdate(period, snapshot.http().sampleRate());
        httpAvgResponseSeries.addOrUpdate(period, nullableMetric(snapshot.http().avgDurationMs()));
        httpErrorRateSeries.addOrUpdate(period, snapshot.http().failurePercent());

        wsActiveSeries.addOrUpdate(period, snapshot.activeWebSocketConnections());
        wsSentRateSeries.addOrUpdate(period, snapshot.webSocket().sentRate());
        wsReceivedRateSeries.addOrUpdate(period, snapshot.webSocket().receivedRate());
        wsFirstMessageLatencySeries.addOrUpdate(period, nullableMetric(snapshot.webSocket().avgFirstMessageLatencyMs()));
        wsSessionDurationSeries.addOrUpdate(period, snapshot.webSocket().avgDurationMs());
        wsErrorRateSeries.addOrUpdate(period, snapshot.webSocket().failurePercent());

        sseActiveSeries.addOrUpdate(period, snapshot.activeSseStreams());
        sseEventRateSeries.addOrUpdate(period, snapshot.sse().receivedRate());
        sseMatchedRateSeries.addOrUpdate(period, snapshot.sse().matchedRate());
        sseFirstEventLatencySeries.addOrUpdate(period, nullableMetric(snapshot.sse().avgFirstMessageLatencyMs()));
        sseStreamDurationSeries.addOrUpdate(period, snapshot.sse().avgDurationMs());
        sseErrorRateSeries.addOrUpdate(period, snapshot.sse().failurePercent());
    }

    private Number nullableMetric(double value) {
        return Double.isFinite(value) ? value : null;
    }

    public void addOrUpdate(RegularTimePeriod period, double users,
                            double responseTime, double qps, double errorPercent) {
        if (period == null) {
            return;
        }
        httpVirtualUsersSeries.addOrUpdate(period, users);
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

    private void showChartMode(String mode) {
        chartMode = mode;
        for (TrendView trendView : trendViews) {
            trendView.showChartMode(mode);
        }
    }

    private final class TrendView {
        private final JPanel panel;
        private final JPanel splitChartsPanel = new ScrollableChartGridPanel();
        private final JPanel chartCards = new JPanel(new CardLayout());
        private final TimeSeriesCollection combinedDataset = new TimeSeriesCollection();
        private final ChartPanel combinedChartPanel;
        private final List<SeriesControl> controls = new ArrayList<>();
        private final List<SplitChart> splitCharts = new ArrayList<>();

        private TrendView(String titleKey, SeriesSpec... specs) {
            panel = new JPanel(new BorderLayout(8, 0));
            panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            controlsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            for (SeriesSpec spec : specs) {
                JCheckBox checkBox = new JCheckBox(spec.series().getKey().toString(), spec.selected());
                checkBox.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
                checkBox.addActionListener(e -> {
                    if (selectedCount() == 0) {
                        checkBox.setSelected(true);
                    }
                    rebuildCharts();
                });
                controls.add(new SeriesControl(spec, checkBox));
                controlsPanel.add(checkBox);
                splitCharts.add(new SplitChart(spec, createSplitChartPanel(spec)));
            }

            JScrollPane splitScrollPane = new JScrollPane(splitChartsPanel);
            splitScrollPane.setBorder(BorderFactory.createEmptyBorder());
            splitScrollPane.getVerticalScrollBar().setUnitIncrement(16);

            combinedChartPanel = createChartPanel(combinedDataset, titleKey);
            chartCards.add(splitScrollPane, SEPARATE_VIEW);
            chartCards.add(combinedChartPanel, COMBINED_VIEW);

            panel.add(controlsPanel, BorderLayout.NORTH);
            panel.add(chartCards, BorderLayout.CENTER);
            rebuildCharts();
            showChartMode(chartMode);
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

        private void rebuildCharts() {
            rebuildCombinedChart();
            rebuildSplitCharts();
        }

        private void rebuildCombinedChart() {
            combinedDataset.removeAllSeries();
            XYLineAndShapeRenderer renderer = createTrendRenderer();
            int visibleIndex = 0;
            for (SeriesControl control : controls) {
                if (!control.checkBox().isSelected()) {
                    continue;
                }
                combinedDataset.addSeries(control.spec().series());
                renderer.setSeriesPaint(visibleIndex++, control.spec().color());
            }
            combinedChartPanel.getChart().getXYPlot().setRenderer(renderer);
            combinedChartPanel.repaint();
        }

        private void rebuildSplitCharts() {
            int selectedCount = selectedCount();
            splitChartsPanel.removeAll();
            splitChartsPanel.setLayout(new GridLayout(0, selectedCount == 1 ? 1 : 2, 12, 12));
            splitChartsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            for (SplitChart splitChart : splitCharts) {
                if (isSelected(splitChart.spec())) {
                    splitChartsPanel.add(splitChart.chartPanel());
                }
            }
            splitChartsPanel.revalidate();
            splitChartsPanel.repaint();
        }

        private boolean isSelected(SeriesSpec spec) {
            for (SeriesControl control : controls) {
                if (control.spec() == spec) {
                    return control.checkBox().isSelected();
                }
            }
            return false;
        }

        private ChartPanel createSplitChartPanel(SeriesSpec spec) {
            TimeSeriesCollection dataset = new TimeSeriesCollection();
            dataset.addSeries(spec.series());
            ChartPanel chartPanel = createChartPanel(dataset, spec.series().getKey().toString(), false, spec.axisFormat());
            chartPanel.setPreferredSize(new Dimension(420, 220));
            XYLineAndShapeRenderer renderer = createTrendRenderer();
            renderer.setSeriesPaint(0, spec.color());
            chartPanel.getChart().getXYPlot().setRenderer(renderer);
            return chartPanel;
        }

        private void showChartMode(String mode) {
            CardLayout layout = (CardLayout) chartCards.getLayout();
            layout.show(chartCards, mode);
            chartCards.revalidate();
            chartCards.repaint();
        }

        private void resetAxes(long resetTimeMs) {
            PerformanceTrendPanel.resetAxes(combinedChartPanel, resetTimeMs);
            for (SplitChart splitChart : splitCharts) {
                PerformanceTrendPanel.resetAxes(splitChart.chartPanel(), resetTimeMs);
            }
        }
    }

    private record SeriesSpec(TimeSeries series, Color color, boolean selected, AxisFormat axisFormat) {
    }

    private enum AxisFormat {
        DECIMAL,
        INTEGER
    }

    private record SeriesControl(SeriesSpec spec, JCheckBox checkBox) {
    }

    private record SplitChart(SeriesSpec spec, ChartPanel chartPanel) {
    }

    private static final class ScrollableChartGridPanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(24, visibleRect.height - 24);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            Container parent = getParent();
            if (parent instanceof JViewport viewport) {
                return getPreferredSize().height <= viewport.getHeight();
            }
            return false;
        }
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
            return dataset != null
                    && finiteItemCount(dataset, series) == 1
                    && isFinite(dataset.getYValue(series, item));
        }

        private int finiteItemCount(XYDataset dataset, int series) {
            int count = 0;
            for (int i = 0; i < dataset.getItemCount(series); i++) {
                if (isFinite(dataset.getYValue(series, i))) {
                    count++;
                }
            }
            return count;
        }

        private boolean isFinite(double value) {
            return Double.isFinite(value);
        }
    }
}
