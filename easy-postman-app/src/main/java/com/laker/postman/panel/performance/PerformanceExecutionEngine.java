package com.laker.postman.panel.performance;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.threadgroup.PerformanceThreadGroupPlanner;
import com.laker.postman.service.setting.SettingManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@Slf4j
final class PerformanceExecutionEngine {

    private final BooleanSupplier runningSupplier;
    private final PerformanceThreadGroupPlanner threadGroupPlanner = new PerformanceThreadGroupPlanner();
    private final PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
    private final Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();
    private final PerformanceRealtimeMetrics realtimeMetrics = new PerformanceRealtimeMetrics();
    private final PerformanceThreadGroupRunner threadGroupRunner;

    @Getter
    private volatile long startTime;

    PerformanceExecutionEngine(Component dialogParent,
                               BooleanSupplier runningSupplier,
                               BooleanSupplier efficientModeSupplier,
                               IntSupplier responseBodyPreviewLimitKbSupplier,
                               CsvDataPanel csvDataPanel,
                               PerformanceStatsCollector statsCollector,
                               PerformanceResultTablePanel performanceResultTablePanel) {
        this.runningSupplier = runningSupplier;
        PerformanceRequestExecutor requestExecutor = new PerformanceRequestExecutor(
                runningSupplier,
                this::isCancelledOrInterrupted,
                activeSseSources,
                activeWebSockets,
                realtimeMetrics,
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier
        );
        PerformanceResultRecorder resultRecorder = new PerformanceResultRecorder(
                statsCollector,
                performanceResultTablePanel,
                SettingManager::getJmeterSlowRequestThreshold
        );
        PerformanceSamplerExecutor samplerExecutor = new PerformanceSamplerExecutor(
                runningSupplier,
                efficientModeSupplier,
                requestExecutor,
                resultRecorder
        );
        PerformanceIterationContextFactory iterationContextFactory = new PerformanceIterationContextFactory(
                csvDataPanel,
                virtualUsers
        );
        PerformancePlanExecutor planExecutor = new PerformancePlanExecutor(runningSupplier, samplerExecutor);
        this.threadGroupRunner = new PerformanceThreadGroupRunner(
                dialogParent,
                runningSupplier,
                () -> this.startTime,
                this::cancelAllNetworkCalls,
                virtualUsers,
                iterationContextFactory,
                planExecutor
        );
    }

    int getActiveThreads() {
        return virtualUsers.getActiveThreads();
    }

    int getActiveWebSockets() {
        return activeWebSockets.size();
    }

    int getActiveSseStreams() {
        return activeSseSources.size();
    }

    void beginRun(long startTime) {
        this.startTime = startTime;
        realtimeMetrics.reset(startTime);
    }

    void resetVirtualUsers() {
        virtualUsers.resetVirtualUsers();
    }

    PerformanceRealtimeMetrics.Sample sampleRealtimeMetrics(long nowMs) {
        return realtimeMetrics.sample(nowMs);
    }

    PerformanceRealtimeMetrics.LiveSnapshot liveRealtimeMetrics(long nowMs) {
        return realtimeMetrics.liveSnapshot(nowMs);
    }

    int getTotalThreads(DefaultMutableTreeNode rootNode) {
        return threadGroupPlanner.getTotalThreads(PerformanceTestPlanCompiler.compile(rootNode));
    }

    int getTotalThreads(PerformanceTestPlan plan) {
        return threadGroupPlanner.getTotalThreads(plan);
    }

    long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        return threadGroupPlanner.estimateTotalRequests(PerformanceTestPlanCompiler.compile(rootNode));
    }

    long estimateTotalRequests(PerformanceTestPlan plan) {
        return threadGroupPlanner.estimateTotalRequests(plan);
    }

    void runJMeterTreeWithProgress(DefaultMutableTreeNode rootNode,
                                   int totalThreads,
                                   BiConsumer<Integer, Integer> progressUpdater) {
        runPlanWithProgress(PerformanceTestPlanCompiler.compile(rootNode), totalThreads, progressUpdater);
    }

    void runPlanWithProgress(PerformanceTestPlan plan,
                             int totalThreads,
                             BiConsumer<Integer, Integer> progressUpdater) {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }
        threadGroupRunner.run(plan, totalThreads, progressUpdater);
    }

    void cancelAllNetworkCalls() {
        com.laker.postman.service.http.okhttp.OkHttpClientManager.cancelAllCalls();
        for (EventSource eventSource : new ArrayList<>(activeSseSources)) {
            try {
                eventSource.cancel();
            } catch (Exception e) {
                log.debug("取消 SSE EventSource 失败", e);
            }
        }
        activeSseSources.clear();
        for (WebSocket webSocket : new ArrayList<>(activeWebSockets)) {
            try {
                webSocket.close(1000, "Performance stopped");
            } catch (Exception ignored) {
            }
            try {
                webSocket.cancel();
            } catch (Exception e) {
                log.debug("取消 WebSocket 失败", e);
            }
        }
        activeWebSockets.clear();
    }

    static void joinThreadGroupThreads(List<Thread> threadGroupThreads, Runnable cancellationAction) {
        PerformanceThreadGroupRunner.joinThreadGroupThreads(threadGroupThreads, cancellationAction);
    }

    static int calculateStairsTotalSteps(int startThreads, int endThreads, int step) {
        return PerformanceThreadGroupRunner.calculateStairsTotalSteps(startThreads, endThreads, step);
    }

    private boolean isCancelledOrInterrupted(Throwable ex) {
        if (ex == null) {
            return false;
        }
        if (ex instanceof java.io.InterruptedIOException) {
            return true;
        }
        if (ex instanceof java.io.IOException ioException) {
            String message = ioException.getMessage();
            if (message != null && message.contains("Canceled")) {
                return true;
            }
        }
        if (ex instanceof InterruptedException) {
            return true;
        }
        return isCancelledOrInterrupted(ex.getCause());
    }
}
