package com.laker.postman.panel.performance;

import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.panel.performance.model.RequestResult;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private final List<RequestResult> allRequestResults;
    private final Object statsLock;
    private final Map<String, List<Long>> apiCostMap;
    private final Map<String, Integer> apiSuccessMap;
    private final Map<String, Integer> apiFailMap;
    private final Runnable clearCachedPerformanceResultsAction;

    Thread startRun(DefaultMutableTreeNode rootNode,
                    JLabel progressLabel,
                    boolean efficientMode,
                    Consumer<Boolean> efficientModeSetter) {
        propertyPanelSupport.saveAllPropertyPanelData();

        long estimatedRequests = executionEngine.estimateTotalRequests(rootNode);
        final int highConcurrencyThreshold = 5000;
        if (estimatedRequests >= highConcurrencyThreshold && !efficientMode) {
            String message = I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_MSG,
                    String.format("%,d", estimatedRequests)
            );
            int result = JOptionPane.showConfirmDialog(
                    parentComponent,
                    message,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_TITLE),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                efficientModeSetter.accept(true);
                efficientCheckBox.setSelected(true);
                propertyPanelSupport.saveAllPropertyPanelData();
                NotificationUtil.showInfo("✅ " + I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE) + " enabled");
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
        resultTabbedPane.setSelectedIndex(0);
        clearCachedPerformanceResultsAction.run();

        long startTime = System.currentTimeMillis();
        startTimeSetter.accept(startTime);
        executionEngine.beginRun(startTime);
        timerManager.startAll();

        int totalThreads = executionEngine.getTotalThreads(rootNode);
        progressLabel.setText("0/" + totalThreads);

        Thread runThread = new Thread(() -> {
            try {
                executionEngine.runJMeterTreeWithProgress(
                        rootNode,
                        totalThreads,
                        (active, total) -> SwingUtilities.invokeLater(() -> progressLabel.setText(active + "/" + total))
                );
            } finally {
                waitForFinalStats();
                SwingUtilities.invokeLater(() -> finishRunUi());
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

        CompletableFuture.runAsync(this::waitForFinalStats)
                .thenRun(() -> SwingUtilities.invokeLater(this::flushUiAfterStop));

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
        int totalRequests;
        long successCount;
        synchronized (statsLock) {
            totalRequests = allRequestResults.size();
            successCount = allRequestResults.stream().filter(r -> r.success).count();
        }

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
