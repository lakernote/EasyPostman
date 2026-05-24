package com.laker.postman.panel.performance;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.panel.performance.control.PerformanceStatisticsCoordinator;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceTrendWindowCollector;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.jfree.data.time.TimeSeries;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceStatisticsCoordinatorTest extends AbstractSwingUiTest {

    @Test
    public void refreshReportShouldReadSelectedTabOnEdt() throws Exception {
        ThreadCheckingTabbedPane tabbedPane = new ThreadCheckingTabbedPane();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                null,
                null,
                null,
                null,
                tabbedPane,
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> true,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        try {
            coordinator.refreshReport();

            assertTrue(tabbedPane.awaitAccess());
            SwingUtilities.invokeAndWait(() -> {
            });
            assertFalse(tabbedPane.accessedOffEdt.get());
        } finally {
            coordinator.dispose();
        }
    }

    @Test
    public void updateReportShouldShowLiveWebSocketRowsBeforeSampleCompletes() throws Exception {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        Object webSocketSession = new Object();
        metrics.reset(0);
        metrics.recordWebSocketSessionStart(webSocketSession, 1_000, "ws-api", "WS API");
        metrics.recordWebSocketSent(webSocketSession);
        metrics.recordWebSocketSent(webSocketSession);
        metrics.recordWebSocketReceived(webSocketSession);
        metrics.recordWebSocketMatched(webSocketSession);

        PerformanceReportPanel reportPanel = new PerformanceReportPanel();
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("趋势", new JPanel());
        tabbedPane.addTab("报表", reportPanel);
        tabbedPane.setSelectedIndex(1);

        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                new PerformanceTrendWindowCollector(),
                reportPanel,
                null,
                tabbedPane,
                () -> 0,
                () -> 1,
                () -> 0,
                () -> 1000L,
                () -> true,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                metrics::liveSnapshot
        );

        try {
            SwingUtilities.invokeAndWait(coordinator::updateReportWithLatestDataSync);

            DefaultTableModel webSocketModel = getWebSocketReportTableModel(reportPanel);
            assertEquals(webSocketModel.getRowCount(), 2);
            assertEquals(webSocketModel.getValueAt(0, 0), "WS API");
            assertEquals(webSocketModel.getValueAt(0, 1), 1L);
            assertEquals(webSocketModel.getValueAt(0, 2), 1L);
            assertEquals(webSocketModel.getValueAt(0, 5), 2L);
            assertEquals(webSocketModel.getValueAt(0, 6), 1L);
            assertEquals(webSocketModel.getValueAt(0, 7), 1L);
        } finally {
            coordinator.dispose();
        }
    }

    @Test
    public void updateReportShouldShowLiveSseRowsBeforeSampleCompletes() throws Exception {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        Object sseSession = new Object();
        metrics.reset(0);
        metrics.recordSseSessionStart(sseSession, 1_000, "sse-api", "SSE API");
        metrics.recordSseReceived(sseSession);
        metrics.recordSseReceived(sseSession);
        metrics.recordSseMatched(sseSession);

        PerformanceReportPanel reportPanel = new PerformanceReportPanel();
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("趋势", new JPanel());
        tabbedPane.addTab("报表", reportPanel);
        tabbedPane.setSelectedIndex(1);

        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                new PerformanceTrendWindowCollector(),
                reportPanel,
                null,
                tabbedPane,
                () -> 0,
                () -> 0,
                () -> 1,
                () -> 1000L,
                () -> true,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                metrics::liveSnapshot
        );

        try {
            SwingUtilities.invokeAndWait(coordinator::updateReportWithLatestDataSync);

            DefaultTableModel sseModel = getSseReportTableModel(reportPanel);
            assertEquals(sseModel.getRowCount(), 2);
            assertEquals(sseModel.getValueAt(0, 0), "SSE API");
            assertEquals(sseModel.getValueAt(0, 1), 1L);
            assertEquals(sseModel.getValueAt(0, 2), 1L);
            assertEquals(sseModel.getValueAt(0, 5), 2L);
            assertEquals(sseModel.getValueAt(0, 6), 1L);
        } finally {
            coordinator.dispose();
        }
    }

    @Test
    public void sampleTrendShouldUseWindowPeakWebSocketSessionsWhenSocketsCloseBeforeSampling() throws Exception {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        PerformanceRealtimeMetrics metrics = new PerformanceRealtimeMetrics();
        Object firstSession = new Object();
        Object secondSession = new Object();
        metrics.reset(0);
        metrics.recordWebSocketSessionStart(firstSession, 1_000);
        metrics.recordWebSocketSessionStart(secondSession, 1_100);
        metrics.recordWebSocketSent(firstSession);
        metrics.recordWebSocketReceived(secondSession);
        metrics.recordWebSocketSessionEnd(firstSession);
        metrics.recordWebSocketSessionEnd(secondSession);

        PerformanceTrendPanel trendPanel = UiSingletonFactory.getInstance(PerformanceTrendPanel.class);
        trendPanel.clearTrendDataset();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                new PerformanceTrendWindowCollector(),
                null,
                trendPanel,
                new JTabbedPane(),
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> true,
                metrics::sample,
                metrics::liveSnapshot
        );

        try {
            SwingUtilities.invokeAndWait(coordinator::sampleTrendDataSync);

            TimeSeries webSocketSessions = getTrendTimeSeries(trendPanel, "wsActiveSeries");
            assertEquals(webSocketSessions.getValue(0).intValue(), 2);
        } finally {
            coordinator.dispose();
            trendPanel.clearTrendDataset();
        }
    }

    @Test
    public void resetForNewRunShouldDropQueuedTrendSamplesFromPreviousRun() throws Exception {
        PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
        PerformanceTrendPanel trendPanel = UiSingletonFactory.getInstance(PerformanceTrendPanel.class);
        trendPanel.clearTrendDataset();
        PerformanceStatisticsCoordinator coordinator = new PerformanceStatisticsCoordinator(
                statsCollector,
                new PerformanceTrendWindowCollector(),
                null,
                trendPanel,
                new JTabbedPane(),
                () -> 0,
                () -> 0,
                () -> 0,
                () -> 1000L,
                () -> true,
                now -> PerformanceRealtimeMetrics.Sample.empty(),
                now -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
        );

        try {
            coordinator.sampleTrendData();
            coordinator.resetForNewRun();
            waitForQueuedMetrics(coordinator);
            SwingUtilities.invokeAndWait(() -> {
            });

            TimeSeries usersSeries = getTrendTimeSeries(trendPanel, "httpVirtualUsersSeries");
            assertEquals(usersSeries.getItemCount(), 0);
        } finally {
            coordinator.dispose();
            trendPanel.clearTrendDataset();
        }
    }

    private static DefaultTableModel getWebSocketReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("webSocketReportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private static DefaultTableModel getSseReportTableModel(PerformanceReportPanel panel) throws Exception {
        Field field = PerformanceReportPanel.class.getDeclaredField("sseReportTableModel");
        field.setAccessible(true);
        return (DefaultTableModel) field.get(panel);
    }

    private static TimeSeries getTrendTimeSeries(PerformanceTrendPanel panel, String fieldName) throws Exception {
        Field field = PerformanceTrendPanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (TimeSeries) field.get(panel);
    }

    private static void waitForQueuedMetrics(PerformanceStatisticsCoordinator coordinator) throws Exception {
        Field field = PerformanceStatisticsCoordinator.class.getDeclaredField("metricsExecutor");
        field.setAccessible(true);
        ExecutorService metricsExecutor = (ExecutorService) field.get(coordinator);
        Future<?> future = metricsExecutor.submit(() -> {
        });
        future.get(1, TimeUnit.SECONDS);
    }

    private static final class ThreadCheckingTabbedPane extends JTabbedPane {
        private final CountDownLatch accessed = new CountDownLatch(1);
        private final AtomicBoolean accessedOffEdt = new AtomicBoolean(false);

        @Override
        public int getSelectedIndex() {
            if (!SwingUtilities.isEventDispatchThread()) {
                accessedOffEdt.set(true);
            }
            accessed.countDown();
            return 0;
        }

        private boolean awaitAccess() throws InterruptedException {
            return accessed.await(1, TimeUnit.SECONDS);
        }
    }
}
