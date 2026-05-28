package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceTestPlan;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class PerformanceCoreRunSession {

    public interface ExecutionEngine {
        default void beginRun(long startTimeMs) {
        }

        default void beginRun(long startTimeMs, PerformanceCoreResultSink resultSink) {
            beginRun(startTimeMs);
        }

        int getTotalThreads(PerformanceTestPlan plan);

        void runTestPlan(PerformanceTestPlan plan, int totalThreads);

        void cancelAllNetworkCalls();

        default void endRun() {
        }
    }

    private final BooleanSupplier runningSupplier;
    private final Consumer<Boolean> runningSetter;
    private final ExecutionEngine executionEngine;
    private final AtomicReference<Thread> runThread = new AtomicReference<>();

    public PerformanceCoreRunSession(BooleanSupplier runningSupplier,
                                     Consumer<Boolean> runningSetter,
                                     ExecutionEngine executionEngine) {
        this.runningSupplier = runningSupplier == null ? () -> false : runningSupplier;
        this.runningSetter = runningSetter == null ? ignored -> {
        } : runningSetter;
        this.executionEngine = executionEngine;
    }

    public PerformanceRunHandle start(PerformanceTestPlan plan, PerformanceCoreResultSink resultSink) {
        if (executionEngine == null || runningSupplier.getAsBoolean()) {
            return new PerformanceRunHandle(null, this::stop);
        }

        PerformanceCoreResultSink resolvedResultSink = resultSink == null ? PerformanceCoreResultSink.NOOP : resultSink;
        long startTime = System.currentTimeMillis();
        runningSetter.accept(true);
        executionEngine.beginRun(startTime, resolvedResultSink);
        int totalThreads = executionEngine.getTotalThreads(plan);

        Thread thread = PerformanceThreadFactory.newDaemonThread("PerformanceRun", () ->
                runToCompletion(plan, totalThreads, startTime, resolvedResultSink));
        runThread.set(thread);
        thread.start();
        return new PerformanceRunHandle(thread, this::stop);
    }

    public void stop() {
        runningSetter.accept(false);
        Thread thread = runThread.getAndSet(null);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
        if (executionEngine != null) {
            executionEngine.cancelAllNetworkCalls();
        }
    }

    public boolean isRunning() {
        return runningSupplier.getAsBoolean();
    }

    private void runToCompletion(PerformanceTestPlan plan,
                                 int totalThreads,
                                 long startTime,
                                 PerformanceCoreResultSink resultSink) {
        Throwable error = null;
        try {
            executionEngine.runTestPlan(plan, totalThreads);
        } catch (Throwable throwable) {
            error = throwable;
            resultSink.onError(PerformanceRunError.builder()
                    .message(throwable.getMessage())
                    .cause(throwable)
                    .build());
        } finally {
            boolean stopped = !runningSupplier.getAsBoolean() || Thread.currentThread().isInterrupted();
            runningSetter.accept(false);
            executionEngine.cancelAllNetworkCalls();
            long endTime = System.currentTimeMillis();
            try {
                resultSink.onComplete(PerformanceRunSummary.builder()
                        .startTimeMs(startTime)
                        .endTimeMs(endTime)
                        .elapsedTimeMs(endTime - startTime)
                        .stopped(stopped)
                        .error(error)
                        .build());
            } finally {
                executionEngine.endRun();
                runThread.compareAndSet(Thread.currentThread(), null);
            }
        }
    }
}
