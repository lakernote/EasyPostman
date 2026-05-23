package com.laker.postman.panel.performance;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceStatsSnapshot;
import com.laker.postman.panel.performance.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

@Slf4j
@RequiredArgsConstructor
final class PerformanceRunControlSupport {

    private final JComponent parentComponent;
    private final BooleanSupplier runningSupplier;
    private final Consumer<Boolean> runningSetter;
    private final LongSupplier startTimeSupplier;
    private final LongConsumer startTimeSetter;
    private final PerformancePropertyPanelSupport propertyPanelSupport;
    private final PerformanceExecutionEngine executionEngine;
    private final PerformanceStatisticsCoordinator statisticsCoordinator;
    private final PerformanceTimerManager timerManager;
    private final StartButton runBtn;
    private final StopButton stopBtn;
    private final RefreshButton refreshBtn;
    private final JCheckBox efficientCheckBox;
    private final JTabbedPane resultTabbedPane;
    private final PerformanceResultTablePanel performanceResultTablePanel;
    private final PerformanceReportPanel performanceReportPanel;
    private final PerformanceTrendPanel performanceTrendPanel;
    private final PerformanceStatsCollector statsCollector;
    private final Runnable clearCachedPerformanceResultsAction;

    Thread startRun(DefaultMutableTreeNode rootNode,
                    JLabel progressLabel,
                    boolean efficientMode,
                    Consumer<Boolean> efficientModeSetter) {
        propertyPanelSupport.saveAllPropertyPanelData();
        DefaultMutableTreeNode executionRootNode = PerformanceTreeSnapshot.copy(rootNode);
        PerformanceTestPlan executionPlan = PerformanceTestPlanCompiler.compile(executionRootNode);

        long estimatedRequests = executionEngine.estimateTotalRequests(executionPlan);
        final int highConcurrencyThreshold = 5000;
        if (estimatedRequests >= highConcurrencyThreshold && !efficientMode) {
            String message = I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_WARNING_MSG,
                    String.format("%,d", estimatedRequests)
            );
            int result = JOptionPane.showConfirmDialog(
                    parentComponent,
                    message,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_WARNING_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                efficientModeSetter.accept(true);
                efficientCheckBox.setSelected(true);
                propertyPanelSupport.saveAllPropertyPanelData();
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_ENABLED));
            }
        }

        OkHttpClientManager.setConnectionPoolConfig(
                SettingManager.getJmeterMaxIdleConnections(),
                SettingManager.getJmeterKeepAliveSeconds()
        );
        OkHttpClientManager.setDispatcherConfig(
                SettingManager.getJmeterMaxRequests(),
                SettingManager.getJmeterMaxRequestsPerHost()
        );

        if (runningSupplier.getAsBoolean()) {
            return null;
        }

        runningSetter.accept(true);
        runBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        refreshBtn.setEnabled(false);
        clearCachedPerformanceResultsAction.run();

        long startTime = System.currentTimeMillis();
        startTimeSetter.accept(startTime);
        executionEngine.beginRun(startTime);
        timerManager.startAll();

        int totalThreads = executionEngine.getTotalThreads(executionPlan);
        progressLabel.setText("0/" + totalThreads);

        Thread runThread = PerformanceThreadFactory.newDaemonThread("PerformanceRun", () -> {
            try {
                executionEngine.runPlanWithProgress(
                        executionPlan,
                        totalThreads,
                        (active, total) -> SwingUtilities.invokeLater(() -> progressLabel.setText(active + "/" + total))
                );
            } finally {
                boolean stopped = !runningSupplier.getAsBoolean() || Thread.currentThread().isInterrupted();
                waitForFinalStats();
                if (!stopped) {
                    SwingUtilities.invokeLater(this::finishRunUi);
                }
            }
        });
        runThread.start();
        return runThread;
    }

    void stopRun(Thread runThread) {
        runningSetter.accept(false);
        if (runThread != null && runThread.isAlive()) {
            runThread.interrupt();
        }

        executionEngine.cancelAllNetworkCalls();
        runBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        refreshBtn.setEnabled(true);
        timerManager.stopAll();

        PerformanceThreadFactory.newDaemonThread("PerformanceStopFlush", () -> {
            waitForFinalStats();
            SwingUtilities.invokeLater(this::flushUiAfterStop);
        }).start();

        OkHttpClientManager.setDefaultConnectionPoolConfig();
    }

    private void waitForFinalStats() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void finishRunUi() {
        runningSetter.accept(false);
        runBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        refreshBtn.setEnabled(true);
        timerManager.stopAll();

        flushPendingAndCharts("完成时");
        OkHttpClientManager.setDefaultConnectionPoolConfig();
        statisticsCoordinator.updateReportWithLatestDataSync();

        long totalTime = System.currentTimeMillis() - startTimeSupplier.getAsLong();
        PerformanceStatsSnapshot statsSnapshot = statsCollector.snapshot();
        long totalRequests = statsSnapshot.totalRequests();
        long successCount = statsSnapshot.successRequests();

        String message = I18nUtil.getMessage(
                MessageKeys.PERFORMANCE_MSG_EXECUTION_COMPLETED,
                totalRequests,
                successCount,
                totalTime / 1000.0
        );
        NotificationUtil.showSuccess(message);
    }

    private void flushUiAfterStop() {
        flushPendingAndCharts("停止时");
        statisticsCoordinator.updateReportWithLatestDataSync();
    }

    private void flushPendingAndCharts(String phase) {
        try {
            performanceResultTablePanel.flushPendingResults();
        } catch (Exception e) {
            log.warn("{}刷新结果树失败", phase, e);
        }

        try {
            statisticsCoordinator.sampleTrendDataSync();
        } catch (Exception e) {
            log.warn("{}最后一次趋势图采样失败", phase, e);
        }
    }
}
