package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.plan.PerformanceThreadGroupPlan;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

@Slf4j
final class PerformanceThreadGroupRunner {

    private final Component dialogParent;
    private final BooleanSupplier runningSupplier;
    private final LongSupplier startTimeSupplier;
    private final Runnable cancellationAction;
    private final PerformanceVirtualUserCoordinator virtualUsers;
    private final PerformanceIterationContextFactory iterationContextFactory;
    private final PerformancePlanExecutor planExecutor;

    PerformanceThreadGroupRunner(Component dialogParent,
                                 BooleanSupplier runningSupplier,
                                 LongSupplier startTimeSupplier,
                                 Runnable cancellationAction,
                                 PerformanceVirtualUserCoordinator virtualUsers,
                                 PerformanceIterationContextFactory iterationContextFactory,
                                 PerformancePlanExecutor planExecutor) {
        this.dialogParent = dialogParent;
        this.runningSupplier = runningSupplier;
        this.startTimeSupplier = startTimeSupplier;
        this.cancellationAction = cancellationAction;
        this.virtualUsers = virtualUsers;
        this.iterationContextFactory = iterationContextFactory;
        this.planExecutor = planExecutor;
    }

    void run(PerformanceTestPlan plan,
             int totalThreads,
             BiConsumer<Integer, Integer> progressUpdater) {
        if (!runningSupplier.getAsBoolean() || plan == null) {
            return;
        }

        List<Thread> threadGroupThreads = new ArrayList<>();
        for (PerformanceThreadGroupPlan threadGroup : plan.getThreadGroups()) {
            Thread thread = PerformanceThreadFactory.newDaemonThread(
                    "PerformanceThreadGroup",
                    () -> runThreadGroup(threadGroup, progressUpdater, totalThreads)
            );
            threadGroupThreads.add(thread);
            thread.start();
        }
        joinThreadGroupThreads(threadGroupThreads, cancellationAction);
    }

    private void runThreadGroup(PerformanceThreadGroupPlan groupPlan,
                                BiConsumer<Integer, Integer> progressUpdater,
                                int totalThreads) {
        ThreadGroupData threadGroupData = groupPlan.getThreadGroupData();
        switch (threadGroupData.threadMode) {
            case FIXED -> runFixedThreads(groupPlan, threadGroupData, progressUpdater, totalThreads);
            case RAMP_UP -> runRampUpThreads(groupPlan, threadGroupData, progressUpdater, totalThreads);
            case SPIKE -> runSpikeThreads(groupPlan, threadGroupData, progressUpdater, totalThreads);
            case STAIRS -> runStairsThreads(groupPlan, threadGroupData, progressUpdater, totalThreads);
        }
    }

    private void runFixedThreads(PerformanceThreadGroupPlan groupPlan,
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
                        runTaskIteration(groupPlan, 0);
                    }
                } else {
                    runTask(groupPlan, loops);
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
                cancellationAction.run();
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

    private void runRampUpThreads(PerformanceThreadGroupPlan groupPlan,
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

            int currentSecond = (int) (System.currentTimeMillis() - startTimeSupplier.getAsLong()) / 1000;
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
                                    && System.currentTimeMillis() - startTimeSupplier.getAsLong() < totalDuration * 1000L) {
                                runTaskIteration(groupPlan, 0);
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

    private void runSpikeThreads(PerformanceThreadGroupPlan groupPlan,
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
                    groupPlan,
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

            long elapsedSeconds = (System.currentTimeMillis() - startTimeSupplier.getAsLong()) / 1000;
            if (elapsedSeconds >= totalTime) {
                scheduler.shutdown();
                return;
            }

            long now = System.currentTimeMillis();
            int targetThreads;

            if (elapsedSeconds < adjustedRampUpTime) {
                double progress = (double) elapsedSeconds / adjustedRampUpTime;
                targetThreads = minThreads + (int) (progress * (maxThreads - minThreads));
                adjustSpikeThreadCount(groupPlan, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
            } else if (elapsedSeconds < adjustedRampUpTime + adjustedHoldTime) {
                targetThreads = maxThreads;
                adjustSpikeThreadCount(groupPlan, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
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
                adjustSpikeThreadCount(groupPlan, tg, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
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

    private void runStairsThreads(PerformanceThreadGroupPlan groupPlan,
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
                    groupPlan,
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

            long elapsedSeconds = (System.currentTimeMillis() - startTimeSupplier.getAsLong()) / 1000;
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

            adjustStairsThreadCount(groupPlan, activeWorkerThreads, targetThreads, totalTime, progressUpdater, totalThreads, threadEndTimes);
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

    void adjustSpikeThreadCount(PerformanceThreadGroupPlan groupPlan,
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
                        groupPlan,
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

                long elapsedSeconds = (now - startTimeSupplier.getAsLong()) / 1000;
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

    private void adjustStairsThreadCount(PerformanceThreadGroupPlan groupPlan,
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
                    groupPlan,
                    activeWorkerThreads,
                    totalTime,
                    progressUpdater,
                    totalThreads,
                    threadEndTimes
            );
        }
    }

    private void startWindowedVirtualUser(String threadNamePrefix,
                                          PerformanceThreadGroupPlan groupPlan,
                                          AtomicInteger activeWorkerThreads,
                                          int totalTime,
                                          BiConsumer<Integer, Integer> progressUpdater,
                                          int totalThreads,
                                          ConcurrentHashMap<Thread, Long> threadEndTimes) {
        Thread thread = virtualUsers.newThread(threadNamePrefix, progressUpdater, totalThreads, () -> {
            try {
                Thread currentThread = Thread.currentThread();
                while (runningSupplier.getAsBoolean()
                        && System.currentTimeMillis() - startTimeSupplier.getAsLong() < totalTime * 1000L
                        && System.currentTimeMillis() < threadEndTimes.getOrDefault(currentThread, Long.MAX_VALUE)) {
                    runTaskIteration(groupPlan, 0);
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
        int threadRange = Math.max(0, endThreads - startThreads);
        int safeStep = Math.max(1, step);
        return Math.max(1, (threadRange + safeStep - 1) / safeStep);
    }

    private static boolean isOpenEndedWorker(ConcurrentHashMap<Thread, Long> threadEndTimes, Thread thread) {
        return Long.valueOf(Long.MAX_VALUE).equals(threadEndTimes.get(thread));
    }

    private void runTaskIteration(PerformanceThreadGroupPlan groupPlan, int iterationCount) {
        ExecutionVariableContext iterationContext = iterationContextFactory.create(iterationCount);
        planExecutor.executeIteration(groupPlan, iterationContext);
    }

    private void runTask(PerformanceThreadGroupPlan groupPlan, int loops) {
        for (int l = 0; l < loops && runningSupplier.getAsBoolean(); l++) {
            runTaskIteration(groupPlan, loops);
        }
    }

    private void updateProgress(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        progressUpdater.accept(virtualUsers.getActiveThreads(), totalThreads);
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
}
