package com.laker.postman.panel.performance;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.service.variable.IterationDataRuntimeSupport;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

@Slf4j
final class PerformanceExecutionEngine {

    private final Component dialogParent;
    private final BooleanSupplier runningSupplier;
    private final BooleanSupplier efficientModeSupplier;
    private final CsvDataPanel csvDataPanel;
    private final PerformanceRequestExecutor requestExecutor;
    private final PerformanceResultRecorder resultRecorder;
    private final PerformanceThreadGroupPlanner threadGroupPlanner = new PerformanceThreadGroupPlanner();
    private final PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
    private final Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();
    private final PerformanceRealtimeMetrics realtimeMetrics = new PerformanceRealtimeMetrics();

    @Getter
    private volatile long startTime;

    PerformanceExecutionEngine(Component dialogParent,
                               BooleanSupplier runningSupplier,
                               BooleanSupplier efficientModeSupplier,
                               IntSupplier responseBodyPreviewLimitKbSupplier,
                               CsvDataPanel csvDataPanel,
                               PerformanceStatsCollector statsCollector,
                               PerformanceResultTablePanel performanceResultTablePanel) {
        this.dialogParent = dialogParent;
        this.runningSupplier = runningSupplier;
        this.efficientModeSupplier = efficientModeSupplier;
        this.csvDataPanel = csvDataPanel;
        this.requestExecutor = new PerformanceRequestExecutor(
                runningSupplier,
                this::isCancelledOrInterrupted,
                activeSseSources,
                activeWebSockets,
                realtimeMetrics,
                efficientModeSupplier,
                responseBodyPreviewLimitKbSupplier
        );
        this.resultRecorder = new PerformanceResultRecorder(
                statsCollector,
                performanceResultTablePanel,
                SettingManager::getJmeterSlowRequestThreshold
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

    int getTotalThreads(DefaultMutableTreeNode rootNode) {
        return threadGroupPlanner.getTotalThreads(rootNode);
    }

    long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        return threadGroupPlanner.estimateTotalRequests(rootNode);
    }

    void runJMeterTreeWithProgress(DefaultMutableTreeNode rootNode,
                                   int totalThreads,
                                   BiConsumer<Integer, Integer> progressUpdater) {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }
        Object userObj = rootNode.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode)) {
            return;
        }

        if (Objects.requireNonNull(jtNode.type) == NodeType.ROOT) {
            List<Thread> tgThreads = new ArrayList<>();
            for (int i = 0; i < rootNode.getChildCount(); i++) {
                DefaultMutableTreeNode tgNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
                Object tgUserObj = tgNode.getUserObject();
                if (tgUserObj instanceof JMeterTreeNode tgJtNode && !tgJtNode.enabled) {
                    continue;
                }
                Thread thread = PerformanceThreadFactory.newDaemonThread(
                        "PerformanceThreadGroup",
                        () -> runJMeterTreeWithProgress(tgNode, totalThreads, progressUpdater)
                );
                tgThreads.add(thread);
                thread.start();
            }
            joinThreadGroupThreads(tgThreads, this::cancelAllNetworkCalls);
            return;
        }

        if (Objects.requireNonNull(jtNode.type) != NodeType.THREAD_GROUP) {
            log.warn("不支持的节点类型: {}", jtNode.type);
            return;
        }

        ThreadGroupData tg = resolveThreadGroupData(jtNode);
        switch (tg.threadMode) {
            case FIXED -> runFixedThreads(rootNode, tg, progressUpdater, totalThreads);
            case RAMP_UP -> runRampUpThreads(rootNode, tg, progressUpdater, totalThreads);
            case SPIKE -> runSpikeThreads(rootNode, tg, progressUpdater, totalThreads);
            case STAIRS -> runStairsThreads(rootNode, tg, progressUpdater, totalThreads);
        }
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
        boolean interrupted = false;
        for (Thread thread : threadGroupThreads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException ignored) {
                    interrupted = true;
                    if (cancellationAction != null) {
                        cancellationAction.run();
                    }
                    for (Thread candidate : threadGroupThreads) {
                        if (candidate.isAlive()) {
                            candidate.interrupt();
                        }
                    }
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private ThreadGroupData resolveThreadGroupData(JMeterTreeNode node) {
        return PerformanceThreadGroupPlanner.resolveThreadGroupData(node);
    }

    private void runFixedThreads(DefaultMutableTreeNode groupNode,
                                 ThreadGroupData tg,
                                 BiConsumer<Integer, Integer> progressUpdater,
                                 int totalThreads) {
        int numThreads = tg.numThreads;
        int loops = tg.loops;
        boolean useTime = tg.useTime;
        int durationSeconds = tg.duration;

        ExecutorService executor = Executors.newFixedThreadPool(
                numThreads,
                PerformanceThreadFactory.daemonFactory("PerformanceFixedWorker")
        );
        long threadGroupStartTime = System.currentTimeMillis();
        long endTime = useTime ? (threadGroupStartTime + (durationSeconds * 1000L)) : Long.MAX_VALUE;

        for (int i = 0; i < numThreads; i++) {
            if (!runningSupplier.getAsBoolean()) {
                executor.shutdownNow();
                return;
            }
            virtualUsers.submit(executor, progressUpdater, totalThreads, () -> {
                if (useTime) {
                    while (System.currentTimeMillis() < endTime && runningSupplier.getAsBoolean()) {
                        runTaskIteration(groupNode, 0);
                    }
                } else {
                    runTask(groupNode, loops);
                }
            });
        }
        executor.shutdown();
        try {
            boolean terminated;
            if (useTime) {
                terminated = executor.awaitTermination(durationSeconds + 5L, TimeUnit.SECONDS);
            } else {
                terminated = executor.awaitTermination(1, TimeUnit.HOURS);
            }

            if (!terminated || !runningSupplier.getAsBoolean()) {
                log.warn("线程池未能在预期时间内完成，强制关闭剩余线程");
                cancelAllNetworkCalls();
                List<Runnable> pendingTasks = executor.shutdownNow();
                log.debug("已取消 {} 个待执行任务", pendingTasks.size());
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    log.warn("部分线程在强制关闭后仍未终止，这是正常的，线程会在网络操作完成后自动退出");
                } else {
                    log.info("所有线程已成功终止");
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
            if (runningSupplier.getAsBoolean()) {
                String message = I18nUtil.getMessage(
                        MessageKeys.PERFORMANCE_MSG_EXECUTION_INTERRUPTED,
                        exception.getMessage()
                );
                String title = I18nUtil.getMessage(MessageKeys.GENERAL_ERROR);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        dialogParent,
                        message,
                        title,
                        JOptionPane.ERROR_MESSAGE
                ));
                log.error(exception.getMessage(), exception);
            } else {
                log.debug("固定线程模式已停止");
            }
        }
    }

    private void runRampUpThreads(DefaultMutableTreeNode groupNode,
                                  ThreadGroupData tg,
                                  BiConsumer<Integer, Integer> progressUpdater,
                                  int totalThreads) {
        int startThreads = tg.rampUpStartThreads;
        int endThreads = tg.rampUpEndThreads;
        int rampUpTime = tg.rampUpTime;
        int totalDuration = tg.rampUpDuration;
        double threadsPerSecond = (double) (endThreads - startThreads) / rampUpTime;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
                1,
                PerformanceThreadFactory.daemonFactory("PerformanceRampScheduler")
        );
        ExecutorService executor = Executors.newCachedThreadPool(
                PerformanceThreadFactory.daemonFactory("PerformanceRampWorker")
        );
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);

        scheduler.scheduleAtFixedRate(() -> {
            if (!runningSupplier.getAsBoolean()) {
                scheduler.shutdownNow();
                executor.shutdownNow();
                return;
            }

            int currentSecond = (int) (System.currentTimeMillis() - startTime) / 1000;
            if (currentSecond > totalDuration) {
                scheduler.shutdown();
                return;
            }

            if (currentSecond <= rampUpTime) {
                int targetThreads = startThreads + (int) (threadsPerSecond * currentSecond);
                targetThreads = Math.min(targetThreads, endThreads);

                while (runningSupplier.getAsBoolean()) {
                    int current = activeWorkerThreads.get();
                    if (current >= targetThreads) {
                        break;
                    }
                    if (!activeWorkerThreads.compareAndSet(current, current + 1)) {
                        continue;
                    }
                    virtualUsers.submit(executor, progressUpdater, totalThreads, () -> {
                        try {
                            while (runningSupplier.getAsBoolean()
                                    && System.currentTimeMillis() - startTime < totalDuration * 1000L) {
                                runTaskIteration(groupNode, 0);
                            }
                        } finally {
                            activeWorkerThreads.decrementAndGet();
                        }
                    });
                }
            }
        }, 0, 1, TimeUnit.SECONDS);

        try {
            boolean schedulerTerminated = scheduler.awaitTermination(totalDuration + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !runningSupplier.getAsBoolean()) {
                log.warn("递增模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            executor.shutdown();
            boolean executorTerminated = executor.awaitTermination(10, TimeUnit.SECONDS);
            if (!executorTerminated || !runningSupplier.getAsBoolean()) {
                log.warn("递增模式执行器未能正常终止，强制关闭");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("递增模式部分线程在强制关闭后仍未终止");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            executor.shutdownNow();
            log.error("递增线程执行中断", e);
        }
    }

    private void runSpikeThreads(DefaultMutableTreeNode groupNode,
                                 ThreadGroupData tg,
                                 BiConsumer<Integer, Integer> progressUpdater,
                                 int totalThreads) {
        int minThreads = tg.spikeMinThreads;
        int maxThreads = tg.spikeMaxThreads;
        int rampUpTime = tg.spikeRampUpTime;
        int holdTime = tg.spikeHoldTime;
        int rampDownTime = tg.spikeRampDownTime;
        int totalTime = tg.spikeDuration;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
                1,
                PerformanceThreadFactory.daemonFactory("PerformanceSpikeScheduler")
        );
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);
        ConcurrentHashMap<Thread, Long> threadEndTimes = new ConcurrentHashMap<>();

        int phaseSum = rampUpTime + holdTime + rampDownTime;
        int adjustedRampUpTime = totalTime * rampUpTime / phaseSum;
        int adjustedHoldTime = totalTime * holdTime / phaseSum;
        int adjustedRampDownTime = totalTime - adjustedRampUpTime - adjustedHoldTime;

        for (int i = 0; i < minThreads; i++) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            activeWorkerThreads.incrementAndGet();
            startWindowedVirtualUser(
                    "PerformanceSpikeWorker",
                    groupNode,
                    activeWorkerThreads,
                    totalTime,
                    progressUpdater,
                    totalThreads,
                    threadEndTimes
            );
        }

        scheduler.scheduleAtFixedRate(() -> {
            if (!runningSupplier.getAsBoolean()) {
                scheduler.shutdownNow();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds >= totalTime) {
                scheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();
            int targetThreads;

            if (elapsedSeconds < adjustedRampUpTime) {
                double progress = (double) elapsedSeconds / adjustedRampUpTime;
                targetThreads = minThreads + (int) (progress * (maxThreads - minThreads));
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
            } else if (elapsedSeconds < adjustedRampUpTime + adjustedHoldTime) {
                targetThreads = maxThreads;
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
            } else {
                double progress = (double) (elapsedSeconds - adjustedRampUpTime - adjustedHoldTime) / adjustedRampDownTime;
                targetThreads = maxThreads - (int) (progress * (maxThreads - minThreads));
                targetThreads = Math.max(targetThreads, minThreads);

                int threadsToRemove = activeWorkerThreads.get() - targetThreads;
                if (threadsToRemove > 0) {
                    threadEndTimes.keySet().stream()
                            .filter(t -> t.isAlive() && isOpenEndedWorker(threadEndTimes, t))
                            .limit(threadsToRemove)
                            .forEach(t -> threadEndTimes.put(t, now + 500));
                }
                adjustSpikeThreadCount(groupNode, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
            }

            updateProgress(progressUpdater, totalThreads);
        }, 1, 1, TimeUnit.SECONDS);

        try {
            boolean schedulerTerminated = scheduler.awaitTermination(totalTime + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !runningSupplier.getAsBoolean()) {
                log.warn("尖刺模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            for (Thread thread : threadEndTimes.keySet()) {
                try {
                    if (thread.isAlive()) {
                        thread.join(5000);
                        if (thread.isAlive() && !runningSupplier.getAsBoolean()) {
                            thread.interrupt();
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            for (Thread thread : threadEndTimes.keySet()) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }
            log.error("尖刺模式执行中断", e);
        }
    }

    private void runStairsThreads(DefaultMutableTreeNode groupNode,
                                  ThreadGroupData tg,
                                  BiConsumer<Integer, Integer> progressUpdater,
                                  int totalThreads) {
        int startThreads = tg.stairsStartThreads;
        int endThreads = tg.stairsEndThreads;
        int step = tg.stairsStep;
        int holdTime = tg.stairsHoldTime;
        int totalTime = tg.stairsDuration;

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
                1,
                PerformanceThreadFactory.daemonFactory("PerformanceStairsScheduler")
        );
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);
        ConcurrentHashMap<Thread, Long> threadEndTimes = new ConcurrentHashMap<>();
        int totalSteps = calculateStairsTotalSteps(startThreads, endThreads, step);
        AtomicInteger currentStair = new AtomicInteger(0);
        AtomicLong lastStairChangeTime = new AtomicLong(System.currentTimeMillis());

        for (int i = 0; i < startThreads; i++) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            activeWorkerThreads.incrementAndGet();
            startWindowedVirtualUser(
                    "PerformanceStairsWorker",
                    groupNode,
                    activeWorkerThreads,
                    totalTime,
                    progressUpdater,
                    totalThreads,
                    threadEndTimes
            );
        }

        scheduler.scheduleAtFixedRate(() -> {
            if (!runningSupplier.getAsBoolean()) {
                scheduler.shutdownNow();
                return;
            }

            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            if (elapsedSeconds >= totalTime) {
                scheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();
            long timeSinceLastChange = now - lastStairChangeTime.get();
            int stair = currentStair.get();

            if (timeSinceLastChange >= holdTime * 1000L && stair < totalSteps) {
                stair = currentStair.incrementAndGet();
                lastStairChangeTime.set(now);
            }

            int targetThreads = startThreads;
            if (stair > 0 && stair <= totalSteps) {
                targetThreads = startThreads + stair * step;
                targetThreads = Math.min(targetThreads, endThreads);
            }

            adjustStairsThreadCount(groupNode, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
            updateProgress(progressUpdater, totalThreads);
        }, 1, 1, TimeUnit.SECONDS);

        try {
            boolean schedulerTerminated = scheduler.awaitTermination(totalTime + 10L, TimeUnit.SECONDS);
            if (!schedulerTerminated || !runningSupplier.getAsBoolean()) {
                log.warn("阶梯模式调度器未能正常终止，强制关闭");
                scheduler.shutdownNow();
            }

            for (Thread thread : threadEndTimes.keySet()) {
                try {
                    if (thread.isAlive()) {
                        thread.join(5000);
                        if (thread.isAlive() && !runningSupplier.getAsBoolean()) {
                            thread.interrupt();
                        }
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("等待线程完成时中断", ie);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
            for (Thread thread : threadEndTimes.keySet()) {
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }
            log.error("阶梯模式执行中断", e);
        }
    }

    private void adjustSpikeThreadCount(DefaultMutableTreeNode groupNode,
                                        ThreadGroupData tg,
                                        AtomicInteger activeWorkerThreads,
                                        int targetThreads,
                                        int totalTime,
                                        BiConsumer<Integer, Integer> progressUpdater,
                                        int totalThreads,
                                        ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = activeWorkerThreads.get();

        if (current < targetThreads) {
            int threadsToAdd = targetThreads - current;
            for (int i = 0; i < threadsToAdd; i++) {
                if (!runningSupplier.getAsBoolean()) {
                    return;
                }

                activeWorkerThreads.incrementAndGet();
                startWindowedVirtualUser(
                        "PerformanceSpikeWorker",
                        groupNode,
                        activeWorkerThreads,
                        totalTime,
                        progressUpdater,
                        totalThreads,
                        threadEndTimes
                );
            }
        } else if (current > targetThreads) {
            int threadsToRemove = current - targetThreads;
            long now = System.currentTimeMillis();
            List<Thread> availableThreads = threadEndTimes.keySet().stream()
                    .filter(t -> t.isAlive() && isOpenEndedWorker(threadEndTimes, t))
                    .limit(threadsToRemove)
                    .toList();

            if (!availableThreads.isEmpty()) {
                int rampDownTime = tg.spikeRampDownTime;
                int totalSpikeTime = tg.spikeDuration;
                int rampUpTime = tg.spikeRampUpTime;
                int holdTime = tg.spikeHoldTime;
                int phaseSum = rampUpTime + holdTime + rampDownTime;
                int adjustedRampDownTime = totalSpikeTime * rampDownTime / phaseSum;
                adjustedRampDownTime = Math.max(adjustedRampDownTime, 1);

                long elapsedSeconds = (now - startTime) / 1000;
                long rampDownStartTime = (long) (rampUpTime + holdTime) * totalSpikeTime / phaseSum;
                long timeLeftInRampDown = Math.max(1, adjustedRampDownTime - (elapsedSeconds - rampDownStartTime));

                for (int i = 0; i < availableThreads.size(); i++) {
                    Thread thread = availableThreads.get(i);
                    long delayMs = now + (long) (i + 1) * timeLeftInRampDown * 1000 / (availableThreads.size() + 1);
                    threadEndTimes.put(thread, delayMs);
                }
            }
        }
    }

    private void adjustStairsThreadCount(DefaultMutableTreeNode groupNode,
                                         AtomicInteger activeWorkerThreads,
                                         int targetThreads,
                                         int totalTime,
                                         BiConsumer<Integer, Integer> progressUpdater,
                                         int totalThreads,
                                         ConcurrentHashMap<Thread, Long> threadEndTimes) {
        int current = activeWorkerThreads.get();
        if (current >= targetThreads) {
            return;
        }

        int threadsToAdd = targetThreads - current;
        for (int i = 0; i < threadsToAdd; i++) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }

            activeWorkerThreads.incrementAndGet();
            startWindowedVirtualUser(
                    "PerformanceStairsWorker",
                    groupNode,
                    activeWorkerThreads,
                    totalTime,
                    progressUpdater,
                    totalThreads,
                    threadEndTimes
            );
        }
    }

    private void startWindowedVirtualUser(String threadNamePrefix,
                                          DefaultMutableTreeNode groupNode,
                                          AtomicInteger activeWorkerThreads,
                                          int totalTime,
                                          BiConsumer<Integer, Integer> progressUpdater,
                                          int totalThreads,
                                          ConcurrentHashMap<Thread, Long> threadEndTimes) {
        Thread thread = virtualUsers.newThread(threadNamePrefix, progressUpdater, totalThreads, () -> {
            try {
                Thread currentThread = Thread.currentThread();
                while (runningSupplier.getAsBoolean()
                        && System.currentTimeMillis() - startTime < totalTime * 1000L
                        && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                    runTaskIteration(groupNode, 0);
                }
            } finally {
                activeWorkerThreads.decrementAndGet();
                threadEndTimes.remove(Thread.currentThread());
            }
        });
        threadEndTimes.put(thread, Long.MAX_VALUE);
        thread.start();
    }

    static int calculateStairsTotalSteps(int startThreads, int endThreads, int step) {
        // 阶梯步长不能整除线程差时也要补齐最后一阶，否则执行永远到不了用户设置的最终线程数。
        int threadRange = Math.max(0, endThreads - startThreads);
        int safeStep = Math.max(1, step);
        return Math.max(1, (threadRange + safeStep - 1) / safeStep);
    }

    private static boolean isOpenEndedWorker(ConcurrentHashMap<Thread, Long> threadEndTimes, Thread thread) {
        // worker 退出时会并发 remove 自己；调度线程看到旧 key 时要按“已结束”处理，避免拆箱 null。
        return Long.valueOf(Long.MAX_VALUE).equals(threadEndTimes.get(thread));
    }

    private void runTaskIteration(DefaultMutableTreeNode groupNode, int iterationCount) {
        int iterationIndex = virtualUsers.nextIterationIndex();
        ExecutionVariableContext iterationContext = new ExecutionVariableContext();
        iterationContext.setIterationInfo(iterationIndex, iterationCount);
        iterationContext.replaceIterationData(
                IterationDataRuntimeSupport.prepare(resolveCsvRowForCurrentThread()));
        for (int i = 0; i < groupNode.getChildCount() && runningSupplier.getAsBoolean(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && !jtNode.enabled) {
                continue;
            }
            executeRequestNode(userObj, child, iterationContext);
        }
    }

    private void runTask(DefaultMutableTreeNode groupNode, int loops) {
        for (int l = 0; l < loops && runningSupplier.getAsBoolean(); l++) {
            runTaskIteration(groupNode, loops);
        }
    }

    private void executeRequestNode(Object userObj, DefaultMutableTreeNode child,
                                    ExecutionVariableContext iterationContext) {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }

        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
            PerformanceRequestExecutionResult executionResult = requestExecutor.execute(
                    child,
                    jtNode,
                    iterationContext
            );
            if (executionResult == null) {
                return;
            }
            resultRecorder.record(executionResult, efficientModeSupplier.getAsBoolean());
            if (executionResult.interrupted) {
                log.debug("跳过被中断请求的统计: {}", jtNode.httpRequestItem.getName());
            }

            if (!runningSupplier.getAsBoolean()) {
                return;
            }

            for (int j = 0; j < child.getChildCount() && runningSupplier.getAsBoolean() && !executionResult.webSocketRequest; j++) {
                DefaultMutableTreeNode sub = (DefaultMutableTreeNode) child.getChildAt(j);
                Object subObj = sub.getUserObject();
                if (subObj instanceof JMeterTreeNode subNode && subNode.type == NodeType.TIMER && subNode.timerData != null) {
                    if (!subNode.enabled) {
                        continue;
                    }
                    try {
                        TimeUnit.MILLISECONDS.sleep(subNode.timerData.delayMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private Map<String, String> resolveCsvRowForCurrentThread() {
        if (csvDataPanel == null || !csvDataPanel.hasData()) {
            return null;
        }
        int rowCount = csvDataPanel.getRowCount();
        if (rowCount <= 0) {
            return null;
        }
        Integer vuIndex = virtualUsers.currentVirtualUserIndex();
        int rowIdx = (vuIndex != null ? vuIndex : 0) % rowCount;
        return csvDataPanel.getRowData(rowIdx);
    }

    private void updateProgress(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        progressUpdater.accept(virtualUsers.getActiveThreads(), totalThreads);
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
