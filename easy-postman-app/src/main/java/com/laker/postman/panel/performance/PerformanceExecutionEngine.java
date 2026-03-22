package com.laker.postman.panel.performance;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.execution.PerformanceResultRecorder;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

@Slf4j
final class PerformanceExecutionEngine {

    private final Component dialogParent;
    private final BooleanSupplier runningSupplier;
    private final BooleanSupplier efficientModeSupplier;
    private final CsvDataPanel csvDataPanel;
    private final PerformanceRequestExecutor requestExecutor;
    private final PerformanceResultRecorder resultRecorder;
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger virtualUserCounter = new AtomicInteger(0);
    private final ThreadLocal<Integer> threadVirtualUserIndex = new ThreadLocal<>();
    private final Set<EventSource> activeSseSources = ConcurrentHashMap.newKeySet();
    private final Set<WebSocket> activeWebSockets = ConcurrentHashMap.newKeySet();

    @Getter
    private volatile long startTime;

    PerformanceExecutionEngine(Component dialogParent,
                               BooleanSupplier runningSupplier,
                               BooleanSupplier efficientModeSupplier,
                               CsvDataPanel csvDataPanel,
                               List<RequestResult> allRequestResults,
                               Map<String, List<Long>> apiCostMap,
                               Map<String, Integer> apiSuccessMap,
                               Map<String, Integer> apiFailMap,
                               Object statsLock,
                               PerformanceResultTablePanel performanceResultTablePanel) {
        this.dialogParent = dialogParent;
        this.runningSupplier = runningSupplier;
        this.efficientModeSupplier = efficientModeSupplier;
        this.csvDataPanel = csvDataPanel;
        this.requestExecutor = new PerformanceRequestExecutor(
                runningSupplier,
                this::isCancelledOrInterrupted,
                activeSseSources,
                activeWebSockets
        );
        this.resultRecorder = new PerformanceResultRecorder(
                allRequestResults,
                apiCostMap,
                apiSuccessMap,
                apiFailMap,
                statsLock,
                performanceResultTablePanel,
                SettingManager::getJmeterSlowRequestThreshold
        );
    }

    int getActiveThreads() {
        return activeThreads.get();
    }

    void beginRun(long startTime) {
        this.startTime = startTime;
    }

    void resetVirtualUsers() {
        virtualUserCounter.set(0);
    }

    int getTotalThreads(DefaultMutableTreeNode rootNode) {
        int total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                if (!jtNode.enabled) {
                    continue;
                }
                ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();
                switch (tg.threadMode) {
                    case FIXED -> total += tg.numThreads;
                    case RAMP_UP -> total += tg.rampUpEndThreads;
                    case SPIKE -> total += tg.spikeMaxThreads;
                    case STAIRS -> total += tg.stairsEndThreads;
                }
            }
        }
        return total;
    }

    long estimateTotalRequests(DefaultMutableTreeNode rootNode) {
        final double avgRequestDuration = 0.3;

        long total = 0;
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Object userObj = groupNode.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.THREAD_GROUP) {
                if (!jtNode.enabled) {
                    continue;
                }

                ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();
                int enabledRequests = countEnabledRequests(groupNode);
                if (enabledRequests == 0) {
                    continue;
                }

                switch (tg.threadMode) {
                    case FIXED -> {
                        if (tg.useTime) {
                            double requestsPerSecondPerThread = 1.0 / avgRequestDuration;
                            total += (long) (tg.numThreads * tg.duration * requestsPerSecondPerThread * enabledRequests);
                        } else {
                            total += (long) tg.numThreads * tg.loops * enabledRequests;
                        }
                    }
                    case RAMP_UP -> {
                        int avgThreads = (tg.rampUpStartThreads + tg.rampUpEndThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / avgRequestDuration;
                        total += (long) (avgThreads * tg.rampUpDuration * requestsPerSecondPerThread * enabledRequests);
                    }
                    case SPIKE -> {
                        int totalTime = tg.spikeRampUpTime + tg.spikeHoldTime + tg.spikeRampDownTime;
                        int avgThreads = (tg.spikeMinThreads + tg.spikeMaxThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / avgRequestDuration;
                        total += (long) (avgThreads * totalTime * requestsPerSecondPerThread * enabledRequests);
                    }
                    case STAIRS -> {
                        int avgThreads = (tg.stairsStartThreads + tg.stairsEndThreads) / 2;
                        double requestsPerSecondPerThread = 1.0 / avgRequestDuration;
                        total += (long) (avgThreads * tg.stairsDuration * requestsPerSecondPerThread * enabledRequests);
                    }
                }
            }
        }
        return total;
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
                Thread thread = new Thread(() -> runJMeterTreeWithProgress(tgNode, totalThreads, progressUpdater));
                tgThreads.add(thread);
                thread.start();
            }
            for (Thread thread : tgThreads) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            return;
        }

        if (Objects.requireNonNull(jtNode.type) != NodeType.THREAD_GROUP) {
            log.warn("不支持的节点类型: {}", jtNode.type);
            return;
        }

        ThreadGroupData tg = jtNode.threadGroupData != null ? jtNode.threadGroupData : new ThreadGroupData();
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

    private int countEnabledRequests(DefaultMutableTreeNode groupNode) {
        int enabledRequests = 0;
        for (int j = 0; j < groupNode.getChildCount(); j++) {
            DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) groupNode.getChildAt(j);
            Object reqObj = requestNode.getUserObject();
            if (reqObj instanceof JMeterTreeNode reqJtNode
                    && reqJtNode.type == NodeType.REQUEST
                    && reqJtNode.enabled) {
                enabledRequests++;
            }
        }
        return enabledRequests;
    }

    private void runFixedThreads(DefaultMutableTreeNode groupNode,
                                 ThreadGroupData tg,
                                 BiConsumer<Integer, Integer> progressUpdater,
                                 int totalThreads) {
        int numThreads = tg.numThreads;
        int loops = tg.loops;
        boolean useTime = tg.useTime;
        int durationSeconds = tg.duration;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long threadGroupStartTime = System.currentTimeMillis();
        long endTime = useTime ? (threadGroupStartTime + (durationSeconds * 1000L)) : Long.MAX_VALUE;

        for (int i = 0; i < numThreads; i++) {
            if (!runningSupplier.getAsBoolean()) {
                executor.shutdownNow();
                return;
            }
            final int vuIndex = virtualUserCounter.getAndIncrement();
            executor.submit(() -> {
                threadVirtualUserIndex.set(vuIndex);
                activeThreads.incrementAndGet();
                updateProgress(progressUpdater, totalThreads);
                try {
                    if (useTime) {
                        while (System.currentTimeMillis() < endTime && runningSupplier.getAsBoolean()) {
                            runTaskIteration(groupNode);
                        }
                    } else {
                        runTask(groupNode, loops);
                    }
                } finally {
                    activeThreads.decrementAndGet();
                    updateProgress(progressUpdater, totalThreads);
                    threadVirtualUserIndex.remove();
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
            JOptionPane.showMessageDialog(
                    dialogParent,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_EXECUTION_INTERRUPTED, exception.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                    JOptionPane.ERROR_MESSAGE
            );
            log.error(exception.getMessage(), exception);
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        ExecutorService executor = Executors.newCachedThreadPool();
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
                    final int vuIndex = virtualUserCounter.getAndIncrement();
                    executor.submit(() -> {
                        threadVirtualUserIndex.set(vuIndex);
                        activeThreads.incrementAndGet();
                        updateProgress(progressUpdater, totalThreads);
                        try {
                            while (runningSupplier.getAsBoolean()
                                    && System.currentTimeMillis() - startTime < totalDuration * 1000L) {
                                runTaskIteration(groupNode);
                            }
                        } finally {
                            activeWorkerThreads.decrementAndGet();
                            activeThreads.decrementAndGet();
                            updateProgress(progressUpdater, totalThreads);
                            threadVirtualUserIndex.remove();
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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
            final int vuIndex = virtualUserCounter.getAndIncrement();
            Thread thread = new Thread(() -> {
                threadVirtualUserIndex.set(vuIndex);
                activeThreads.incrementAndGet();
                updateProgress(progressUpdater, totalThreads);
                try {
                    Thread currentThread = Thread.currentThread();
                    while (runningSupplier.getAsBoolean()
                            && System.currentTimeMillis() - startTime < totalTime * 1000L
                            && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                        runTaskIteration(groupNode);
                    }
                } finally {
                    activeWorkerThreads.decrementAndGet();
                    activeThreads.decrementAndGet();
                    updateProgress(progressUpdater, totalThreads);
                    threadEndTimes.remove(Thread.currentThread());
                    threadVirtualUserIndex.remove();
                }
            });
            threadEndTimes.put(thread, Long.MAX_VALUE);
            thread.start();
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
                            .filter(t -> t.isAlive() && threadEndTimes.get(t) == Long.MAX_VALUE)
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

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicInteger activeWorkerThreads = new AtomicInteger(0);
        ConcurrentHashMap<Thread, Long> threadEndTimes = new ConcurrentHashMap<>();
        int totalSteps = Math.max(1, (endThreads - startThreads) / step);
        AtomicInteger currentStair = new AtomicInteger(0);
        AtomicLong lastStairChangeTime = new AtomicLong(System.currentTimeMillis());

        for (int i = 0; i < startThreads; i++) {
            if (!runningSupplier.getAsBoolean()) {
                return;
            }
            activeWorkerThreads.incrementAndGet();
            final int vuIndex = virtualUserCounter.getAndIncrement();
            Thread thread = new Thread(() -> {
                threadVirtualUserIndex.set(vuIndex);
                activeThreads.incrementAndGet();
                updateProgress(progressUpdater, totalThreads);
                try {
                    Thread currentThread = Thread.currentThread();
                    while (runningSupplier.getAsBoolean()
                            && System.currentTimeMillis() - startTime < totalTime * 1000L
                            && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                        runTaskIteration(groupNode);
                    }
                } finally {
                    activeWorkerThreads.decrementAndGet();
                    activeThreads.decrementAndGet();
                    updateProgress(progressUpdater, totalThreads);
                    threadEndTimes.remove(Thread.currentThread());
                    threadVirtualUserIndex.remove();
                }
            });
            threadEndTimes.put(thread, Long.MAX_VALUE);
            thread.start();
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
                final int vuIndex = virtualUserCounter.getAndIncrement();
                Thread thread = new Thread(() -> {
                    threadVirtualUserIndex.set(vuIndex);
                    activeThreads.incrementAndGet();
                    updateProgress(progressUpdater, totalThreads);
                    try {
                        Thread currentThread = Thread.currentThread();
                        while (runningSupplier.getAsBoolean()
                                && System.currentTimeMillis() - startTime < totalTime * 1000L
                                && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                            runTaskIteration(groupNode);
                        }
                    } finally {
                        activeWorkerThreads.decrementAndGet();
                        activeThreads.decrementAndGet();
                        updateProgress(progressUpdater, totalThreads);
                        threadEndTimes.remove(Thread.currentThread());
                        threadVirtualUserIndex.remove();
                    }
                });
                threadEndTimes.put(thread, Long.MAX_VALUE);
                thread.start();
            }
        } else if (current > targetThreads) {
            int threadsToRemove = current - targetThreads;
            long now = System.currentTimeMillis();
            List<Thread> availableThreads = threadEndTimes.keySet().stream()
                    .filter(t -> t.isAlive() && threadEndTimes.get(t) == Long.MAX_VALUE)
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
            final int vuIndex = virtualUserCounter.getAndIncrement();
            Thread thread = new Thread(() -> {
                threadVirtualUserIndex.set(vuIndex);
                activeThreads.incrementAndGet();
                updateProgress(progressUpdater, totalThreads);
                try {
                    Thread currentThread = Thread.currentThread();
                    while (runningSupplier.getAsBoolean()
                            && System.currentTimeMillis() - startTime < totalTime * 1000L
                            && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                        runTaskIteration(groupNode);
                    }
                } finally {
                    activeWorkerThreads.decrementAndGet();
                    activeThreads.decrementAndGet();
                    updateProgress(progressUpdater, totalThreads);
                    threadEndTimes.remove(Thread.currentThread());
                    threadVirtualUserIndex.remove();
                }
            });
            threadEndTimes.put(thread, Long.MAX_VALUE);
            thread.start();
        }
    }

    private void runTaskIteration(DefaultMutableTreeNode groupNode) {
        for (int i = 0; i < groupNode.getChildCount() && runningSupplier.getAsBoolean(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && !jtNode.enabled) {
                continue;
            }
            executeRequestNode(userObj, child);
        }
    }

    private void runTask(DefaultMutableTreeNode groupNode, int loops) {
        for (int l = 0; l < loops && runningSupplier.getAsBoolean(); l++) {
            runTaskIteration(groupNode);
        }
    }

    private void executeRequestNode(Object userObj, DefaultMutableTreeNode child) {
        if (!runningSupplier.getAsBoolean()) {
            return;
        }

        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST && jtNode.httpRequestItem != null) {
            PerformanceRequestExecutionResult executionResult = requestExecutor.execute(
                    child,
                    jtNode,
                    resolveCsvRowForCurrentThread()
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
        Integer vuIndex = threadVirtualUserIndex.get();
        int rowIdx = (vuIndex != null ? vuIndex : 0) % rowCount;
        return csvDataPanel.getRowData(rowIdx);
    }

    private void updateProgress(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        progressUpdater.accept(activeThreads.get(), totalThreads);
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
