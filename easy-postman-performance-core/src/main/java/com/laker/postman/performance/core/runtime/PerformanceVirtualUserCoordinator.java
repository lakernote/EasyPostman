package com.laker.postman.performance.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

public final class PerformanceVirtualUserCoordinator {

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger peakActiveThreads = new AtomicInteger(0);
    private final AtomicInteger virtualUserCounter = new AtomicInteger(0);
    private final AtomicBoolean acceptingSamples = new AtomicBoolean(true);
    private final ThreadLocal<Integer> threadVirtualUserIndex = new ThreadLocal<>();
    private final ThreadLocal<String> threadVirtualUserScope = new ThreadLocal<>();
    private final ThreadLocal<Integer> threadIterationIndex = ThreadLocal.withInitial(() -> 0);
    private final ThreadLocal<Long> threadLoadEndTimeMs = ThreadLocal.withInitial(() -> Long.MAX_VALUE);
    private final Object progressLock = new Object();

    public int getActiveThreads() {
        return activeThreads.get();
    }

    /**
     * 趋势图采样按窗口峰值展示并重置窗口，避免短请求在采样瞬间结束后被误画成 0 用户。
     */
    public int sampleWindowPeakActiveThreads() {
        int current = activeThreads.get();
        int peak = peakActiveThreads.getAndSet(current);
        return Math.max(current, peak);
    }

    public void resetVirtualUsers() {
        virtualUserCounter.set(0);
        peakActiveThreads.set(activeThreads.get());
    }

    public Integer currentVirtualUserIndex() {
        return threadVirtualUserIndex.get();
    }

    public String currentVirtualUserScope() {
        return threadVirtualUserScope.get();
    }

    public int nextIterationIndex() {
        int iterationIndex = threadIterationIndex.get();
        threadIterationIndex.set(iterationIndex + 1);
        return iterationIndex;
    }

    /**
     * 持续时间只限制下一个 sample 的启动，已经发出的 sample 继续等待响应。
     */
    public boolean canStartNextSample() {
        return acceptingSamples.get() && System.currentTimeMillis() < threadLoadEndTimeMs.get();
    }

    void startAcceptingSamples() {
        acceptingSamples.set(true);
    }

    void stopAcceptingSamples() {
        acceptingSamples.set(false);
    }

    public void runWithinLoadWindow(long endTimeMs, Runnable task) {
        threadLoadEndTimeMs.set(endTimeMs);
        try {
            task.run();
        } finally {
            threadLoadEndTimeMs.remove();
        }
    }

    void submit(ExecutorService executor,
                BiConsumer<Integer, Integer> progressUpdater,
                int totalThreads,
                Runnable task) {
        submit(executor, progressUpdater, totalThreads, virtualUserCounter::getAndIncrement, task);
    }

    void submit(ExecutorService executor,
                BiConsumer<Integer, Integer> progressUpdater,
                int totalThreads,
                IntSupplier virtualUserIndexSupplier,
                Runnable task) {
        submit(executor, progressUpdater, totalThreads, virtualUserIndexSupplier, null, task);
    }

    void submit(ExecutorService executor,
                BiConsumer<Integer, Integer> progressUpdater,
                int totalThreads,
                IntSupplier virtualUserIndexSupplier,
                IntFunction<String> virtualUserScopeFactory,
                Runnable task) {
        executor.submit(() -> {
            int vuIndex = nextVirtualUserIndex(virtualUserIndexSupplier);
            run(progressUpdater, totalThreads, vuIndex, resolveVirtualUserScope(vuIndex, virtualUserScopeFactory), task);
        });
    }

    public Thread newThread(String namePrefix,
                            BiConsumer<Integer, Integer> progressUpdater,
                            int totalThreads,
                            Runnable task) {
        return newThread(namePrefix, progressUpdater, totalThreads, virtualUserCounter::getAndIncrement, task);
    }

    public Thread newThread(String namePrefix,
                            BiConsumer<Integer, Integer> progressUpdater,
                            int totalThreads,
                            IntSupplier virtualUserIndexSupplier,
                            Runnable task) {
        return newThread(namePrefix, progressUpdater, totalThreads, virtualUserIndexSupplier, null, task);
    }

    public Thread newThread(String namePrefix,
                            BiConsumer<Integer, Integer> progressUpdater,
                            int totalThreads,
                            IntSupplier virtualUserIndexSupplier,
                            IntFunction<String> virtualUserScopeFactory,
                            Runnable task) {
        return PerformanceThreadFactory.newDaemonThread(
                namePrefix,
                () -> {
                    int vuIndex = nextVirtualUserIndex(virtualUserIndexSupplier);
                    run(progressUpdater, totalThreads, vuIndex, resolveVirtualUserScope(vuIndex, virtualUserScopeFactory), task);
                }
        );
    }

    private void run(BiConsumer<Integer, Integer> progressUpdater,
                     int totalThreads,
                     int vuIndex,
                     String vuScope,
                     Runnable task) {
        threadVirtualUserIndex.set(vuIndex);
        threadVirtualUserScope.set(vuScope);
        threadIterationIndex.set(0);
        incrementActiveThreads(progressUpdater, totalThreads);
        try {
            task.run();
        } finally {
            decrementActiveThreads(progressUpdater, totalThreads);
            threadVirtualUserIndex.remove();
            threadVirtualUserScope.remove();
            threadIterationIndex.remove();
            threadLoadEndTimeMs.remove();
        }
    }

    private void incrementActiveThreads(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        synchronized (progressLock) {
            int active = activeThreads.incrementAndGet();
            updatePeakActiveThreads(active);
            updateProgress(progressUpdater, totalThreads);
        }
    }

    private void decrementActiveThreads(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        synchronized (progressLock) {
            activeThreads.decrementAndGet();
            updateProgress(progressUpdater, totalThreads);
        }
    }

    private void updateProgress(BiConsumer<Integer, Integer> progressUpdater, int totalThreads) {
        progressUpdater.accept(activeThreads.get(), totalThreads);
    }

    private void updatePeakActiveThreads(int active) {
        peakActiveThreads.updateAndGet(previous -> Math.max(previous, active));
    }

    private int nextVirtualUserIndex(IntSupplier virtualUserIndexSupplier) {
        return virtualUserIndexSupplier == null ? virtualUserCounter.getAndIncrement() : virtualUserIndexSupplier.getAsInt();
    }

    private String resolveVirtualUserScope(int vuIndex, IntFunction<String> virtualUserScopeFactory) {
        if (virtualUserScopeFactory != null) {
            String scope = virtualUserScopeFactory.apply(vuIndex);
            if (scope != null && !scope.isBlank()) {
                return scope;
            }
        }
        return "vu:" + vuIndex;
    }
}
