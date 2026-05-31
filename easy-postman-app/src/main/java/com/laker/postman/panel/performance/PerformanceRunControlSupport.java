package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.control.PerformanceRunUiController;
import com.laker.postman.panel.performance.control.PerformanceStatisticsCoordinator;
import com.laker.postman.panel.performance.control.PerformanceTimerManager;
import com.laker.postman.performance.plan.PerformancePlanDocumentCompiler;
import com.laker.postman.panel.performance.tree.PerformanceSwingTreePlanAdapter;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.performance.runtime.PerformanceExecutionEngine;
import com.laker.postman.performance.runtime.PerformanceResultSink;
import com.laker.postman.performance.runtime.PerformanceRunRequest;
import com.laker.postman.performance.runtime.PerformanceRunSession;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.runtime.PerformanceRunError;
import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.runtime.PerformanceRunSummary;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final PerformanceRunSession runSession;
    private final PerformanceStatisticsCoordinator statisticsCoordinator;
    private final PerformanceTimerManager timerManager;
    private final PerformanceRunUiController runUiController;
    private final JCheckBox efficientCheckBox;
    private final PerformanceResultTablePanel performanceResultTablePanel;
    private final PerformanceStatsCollector statsCollector;
    private final Runnable clearCachedPerformanceResultsAction;
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    Thread startRun(DefaultMutableTreeNode rootNode,
                    JLabel progressLabel,
                    boolean efficientMode,
                    Consumer<Boolean> efficientModeSetter) {
        propertyPanelSupport.saveAllPropertyPanelData();
        DefaultMutableTreeNode executionRootNode = PerformanceTreeSnapshot.copy(rootNode);
        PerformanceTestPlan executionPlan = PerformancePlanDocumentCompiler.compile(
                PerformanceSwingTreePlanAdapter.toDocument(executionRootNode)
        );

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

        if (runningSupplier.getAsBoolean() || stopping.get()) {
            return null;
        }

        runUiController.markRunning();
        clearCachedPerformanceResultsAction.run();

        long startTime = System.currentTimeMillis();
        startTimeSetter.accept(startTime);
        timerManager.startAll();

        int totalThreads = executionEngine.getTotalThreads(executionPlan);
        runUiController.initializeProgress(progressLabel, totalThreads);

        PerformanceRunHandle runHandle = runSession.start(PerformanceRunRequest.builder()
                .plan(executionPlan)
                .resultSink(new PerformanceResultSink() {
                    @Override
                    public void onError(PerformanceRunError error) {
                        showRunError(error);
                    }

                    @Override
                    public void onComplete(PerformanceRunSummary summary) {
                        waitForFinalStats();
                        if (summary == null || !summary.isStopped()) {
                            SwingUtilities.invokeLater(PerformanceRunControlSupport.this::finishRunUi);
                        } else {
                            SwingUtilities.invokeLater(PerformanceRunControlSupport.this::finishStoppedRunUi);
                        }
                    }
                })
                .build());
        return runHandle.threadOrNull();
    }

    void stopRun() {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }
        stopping.set(true);
        runSession.stop();
        timerManager.stopAll();
    }

    private void showRunError(PerformanceRunError error) {
        String detail = "";
        if (error != null && error.getMessage() != null) {
            detail = error.getMessage();
        } else if (error != null && error.getCause() != null && error.getCause().getMessage() != null) {
            detail = error.getCause().getMessage();
        }
        String message = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_EXECUTION_INTERRUPTED, detail);
        SwingUtilities.invokeLater(() -> NotificationUtil.showError(message));
    }

    private void waitForFinalStats() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void finishRunUi() {
        stopping.set(false);
        runningSetter.accept(false);
        runUiController.markIdle();
        timerManager.stopAll();

        flushPendingAndCharts("完成时");
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

    private void finishStoppedRunUi() {
        stopping.set(false);
        runningSetter.accept(false);
        runUiController.markIdle();
        timerManager.stopAll();
        flushUiAfterStop();
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
