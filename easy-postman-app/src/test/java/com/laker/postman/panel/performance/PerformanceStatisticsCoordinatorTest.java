package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
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
    public void updateReportShouldRenderLiveWebSocketTotalsBeforeSampleCompletes() throws Exception {
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
            assertEquals(webSocketModel.getRowCount(), 1);
            assertEquals(webSocketModel.getValueAt(0, 0), "WS API");
            assertEquals(webSocketModel.getValueAt(0, 1), 1L);
            assertEquals(webSocketModel.getValueAt(0, 2), "-");
            assertEquals(webSocketModel.getValueAt(0, 3), "-");
            assertEquals(webSocketModel.getValueAt(0, 4), "-");
            assertEquals(webSocketModel.getValueAt(0, 5), 2L);
            assertEquals(webSocketModel.getValueAt(0, 6), 1L);
            assertEquals(webSocketModel.getValueAt(0, 7), 1L);
        } finally {
            coordinator.dispose();
        }
    }

    @Test
    public void updateReportShouldRenderLiveSseTotalsBeforeSampleCompletes() throws Exception {
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
            assertEquals(sseModel.getRowCount(), 1);
            assertEquals(sseModel.getValueAt(0, 0), "SSE API");
            assertEquals(sseModel.getValueAt(0, 1), 1L);
            assertEquals(sseModel.getValueAt(0, 2), "-");
            assertEquals(sseModel.getValueAt(0, 3), "-");
            assertEquals(sseModel.getValueAt(0, 4), "-");
            assertEquals(sseModel.getValueAt(0, 5), 2L);
            assertEquals(sseModel.getValueAt(0, 6), 1L);
        } finally {
            coordinator.dispose();
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
