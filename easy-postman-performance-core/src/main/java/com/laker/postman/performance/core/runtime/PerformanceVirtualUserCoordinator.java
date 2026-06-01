package com.laker.postman.performance.core.runtime;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public final class PerformanceVirtualUserCoordinator {

    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private final AtomicInteger peakActiveThreads = new AtomicInteger(0);
    private final AtomicInteger virtualUserCounter = new AtomicInteger(0);
    private final ThreadLocal<Integer> threadVirtualUserIndex = new ThreadLocal<>();
    private final ThreadLocal<Integer> threadIterationIndex = ThreadLocal.withInitial(() -> 0);

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

    public int nextIterationIndex() {
        int iterationIndex = threadIterationIndex.get();
        threadIterationIndex.set(iterationIndex + 1);
        return iterationIndex;
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
        executor.submit(() -> run(progressUpdater, totalThreads, nextVirtualUserIndex(virtualUserIndexSupplier), task));
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
        return PerformanceThreadFactory.newDaemonThread(
                namePrefix,
                () -> run(progressUpdater, totalThreads, nextVirtualUserIndex(virtualUserIndexSupplier), task)
        );
    }

    private void run(BiConsumer<Integer, Integer> progressUpdater,
                     int totalThreads,
                     int vuIndex,
                     Runnable task) {
        threadVirtualUserIndex.set(vuIndex);
        threadIterationIndex.set(0);
        int active = activeThreads.incrementAndGet();
        updatePeakActiveThreads(active);
        updateProgress(progressUpdater, totalThreads);
        try {
            task.run();
        } finally {
            activeThreads.decrementAndGet();
            updateProgress(progressUpdater, totalThreads);
            threadVirtualUserIndex.remove();
            threadIterationIndex.remove();
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
}
