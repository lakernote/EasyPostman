package com.laker.postman.panel.performance.result;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceTrendSnapshot;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class PerformanceTrendPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldUseTopToolbarProtocolSwitcherAndGlobalViewMode() {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        JToggleButton httpButton = findToggleButton(panel, PerformanceProtocol.HTTP.getDisplayName());
        JToggleButton webSocketButton = findToggleButton(panel, PerformanceProtocol.WEBSOCKET.getDisplayName());
        JToggleButton sseButton = findToggleButton(panel, PerformanceProtocol.SSE.getDisplayName());

        JToggleButton separateButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS)
        );
        JToggleButton combinedButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_COMBINED_CHART)
        );

        if (!httpButton.isSelected()) {
            httpButton.doClick();
        }
        if (!separateButton.isSelected()) {
            separateButton.doClick();
        }

        assertTrue(findAll(panel, JTabbedPane.class).isEmpty());
        assertTrue(httpButton.isSelected());
        assertFalse(webSocketButton.isSelected());
        assertFalse(sseButton.isSelected());
        assertEquals(findAll(panel, SegmentedButtonGroupPanel.class).size(), 2);
        assertTrue(separateButton.isSelected());
        assertFalse(combinedButton.isSelected());
        assertEquals(countVisibleCharts(panel), 4);
        assertTrue(hasCheckBox(
                panel,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_VIRTUAL_USERS)
        ));
        assertTrue(((NumberAxis) findAll(panel, ChartPanel.class).get(0).getChart().getXYPlot().getRangeAxis())
                .getNumberFormatOverride() != null);

        combinedButton.doClick();

        assertTrue(combinedButton.isSelected());
        assertFalse(separateButton.isSelected());
        assertEquals(countVisibleCharts(panel), 1);
        assertFalse(findAll(panel, JCheckBox.class).isEmpty());

        separateButton.doClick();
    }

    @Test
    public void shouldStretchSeparateChartGridWhenViewportIsTallerThanPreferredHeight() {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        JToggleButton separateButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS)
        );
        if (!separateButton.isSelected()) {
            separateButton.doClick();
        }

        JScrollPane splitScrollPane = findFirst(panel, JScrollPane.class);
        JViewport viewport = splitScrollPane.getViewport();
        viewport.setExtentSize(new Dimension(1600, 900));

        Component view = viewport.getView();

        assertTrue(((Scrollable) view).getScrollableTracksViewportHeight());
    }

    @Test
    public void shouldLeaveGapWhenStreamLatencyWindowHasNoSample() throws Exception {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        panel.clearTrendDataset();
        TimeSeries series = getTimeSeries(panel, "wsFirstMessageLatencySeries");
        long base = System.currentTimeMillis();

        panel.addOrUpdate(new Millisecond(new Date(base)), snapshotWithWebSocketLatency(60.0));
        panel.addOrUpdate(new Millisecond(new Date(base + 1_000)), snapshotWithWebSocketLatency(Double.NaN));

        assertEquals(series.getValue(0).doubleValue(), 60.0);
        assertNull(series.getValue(1));
    }

    @Test
    public void shouldLeaveGapWhenHttpResponseWindowHasNoSample() throws Exception {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        panel.clearTrendDataset();
        TimeSeries series = getTimeSeries(panel, "httpAvgResponseSeries");
        long base = System.currentTimeMillis();

        panel.addOrUpdate(new Millisecond(new Date(base)), snapshotWithHttpResponse(69.0));
        panel.addOrUpdate(new Millisecond(new Date(base + 1_000)), snapshotWithHttpResponse(Double.NaN));

        assertEquals(series.getValue(0).doubleValue(), 69.0);
        assertNull(series.getValue(1));
    }

    @Test
    public void shouldRenderSingleFinitePointWhenLaterTrendSamplesAreEmpty() throws Exception {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        panel.clearTrendDataset();
        TimeSeries series = getTimeSeries(panel, "httpAvgResponseSeries");
        long base = System.currentTimeMillis();

        panel.addOrUpdate(new Millisecond(new Date(base)), snapshotWithHttpResponse(69.0));
        panel.addOrUpdate(new Millisecond(new Date(base + 1_000)), snapshotWithHttpResponse(Double.NaN));

        ChartPanel chartPanel = findChartPanelForSeries(panel, series);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) chartPanel.getChart().getXYPlot().getRenderer();

        assertTrue(renderer.getItemShapeVisible(0, 0));
        assertFalse(renderer.getItemShapeVisible(0, 1));
    }

    @Test
    public void shouldHideLatencyAndDurationChartsForStreamingProtocols() {
        PerformanceTrendPanel panel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        JToggleButton separateButton = findToggleButton(
                panel,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SEPARATE_CHARTS)
        );
        if (!separateButton.isSelected()) {
            separateButton.doClick();
        }

        findToggleButton(panel, PerformanceProtocol.WEBSOCKET.getDisplayName()).doClick();

        assertEquals(countVisibleCharts(panel), 4);
        assertFalse(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_FIRST_MESSAGE_LATENCY_MS)));
        assertFalse(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_SESSION_DURATION_MS)));
        assertTrue(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ACTIVE_WS)));

        findToggleButton(panel, PerformanceProtocol.SSE.getDisplayName()).doClick();

        assertEquals(countVisibleCharts(panel), 3);
        assertFalse(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_MATCHED_RATE)));
        assertFalse(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_FIRST_EVENT_LATENCY_MS)));
        assertFalse(hasCheckBox(panel, I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_STREAM_DURATION_MS)));
    }

    private static PerformanceTrendSnapshot snapshotWithHttpResponse(double responseMs) {
        PerformanceTrendSnapshot.ProtocolWindowMetrics empty = emptyMetrics();
        PerformanceTrendSnapshot.ProtocolWindowMetrics http = new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                0, 0, 0, 0, responseMs, 0, 0, 0, 0, 0, 0, Double.NaN
        );
        return new PerformanceTrendSnapshot(0, 0, 0, empty, http, empty, empty);
    }

    private static PerformanceTrendSnapshot snapshotWithWebSocketLatency(double latencyMs) {
        PerformanceTrendSnapshot.ProtocolWindowMetrics empty = emptyMetrics();
        PerformanceTrendSnapshot.ProtocolWindowMetrics ws = new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, latencyMs
        );
        return new PerformanceTrendSnapshot(0, 0, 0, empty, empty, ws, empty);
    }

    private static PerformanceTrendSnapshot.ProtocolWindowMetrics emptyMetrics() {
        return new PerformanceTrendSnapshot.ProtocolWindowMetrics(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, Double.NaN
        );
    }

    private static TimeSeries getTimeSeries(PerformanceTrendPanel panel, String fieldName) throws Exception {
        Field field = PerformanceTrendPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (TimeSeries) field.get(panel);
    }

    private static ChartPanel findChartPanelForSeries(Component root, TimeSeries series) {
        for (ChartPanel chartPanel : findAll(root, ChartPanel.class)) {
            if (chartPanel.getChart().getXYPlot().getDataset() instanceof TimeSeriesCollection dataset
                    && dataset.getSeriesCount() == 1
                    && dataset.getSeries(0) == series) {
                return chartPanel;
            }
        }
        throw new AssertionError("Chart panel not found for series: " + series.getKey());
    }

    private static boolean hasCheckBox(Component root, String text) {
        for (JCheckBox checkBox : findAll(root, JCheckBox.class)) {
            if (text.equals(checkBox.getText())) {
                return true;
            }
        }
        return false;
    }

    private static int countVisibleCharts(Component root) {
        int count = 0;
        for (ChartPanel chartPanel : findAll(root, ChartPanel.class)) {
            if (isEffectivelyVisible(chartPanel, root)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isEffectivelyVisible(Component component, Component root) {
        Component current = component;
        while (current != null) {
            if (current == root) {
                return true;
            }
            if (!current.isVisible()) {
                return false;
            }
            current = current.getParent();
        }
        return false;
    }

    private static JToggleButton findToggleButton(Component root, String text) {
        for (JToggleButton button : findAll(root, JToggleButton.class)) {
            if (text.equals(button.getText())) {
                return button;
            }
        }
        throw new AssertionError("Toggle button not found: " + text);
    }

    private static <T extends Component> T findFirst(Component root, Class<T> type) {
        List<T> components = findAll(root, type);
        if (components.isEmpty()) {
            throw new AssertionError("Component not found: " + type.getName());
        }
        return components.get(0);
    }

    private static <T extends Component> List<T> findAll(Component root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        collect(root, type, matches);
        return matches;
    }

    private static <T extends Component> void collect(Component component, Class<T> type, List<T> matches) {
        if (type.isInstance(component)) {
            matches.add(type.cast(component));
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collect(child, type, matches);
            }
        }
    }
}
