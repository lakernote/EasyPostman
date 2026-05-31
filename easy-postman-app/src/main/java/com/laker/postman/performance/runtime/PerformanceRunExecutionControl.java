package com.laker.postman.performance.runtime;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.PerformanceStatsCollector;
import com.laker.postman.performance.core.model.PerformanceStatsProgressSnapshot;
import com.laker.postman.performance.core.model.PerformanceStatsSnapshot;
import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.worker.PerformanceWorkerResultDetail;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.LongFunction;
import java.util.function.Supplier;

public class PerformanceRunExecutionControl {
    private static final PerformanceStatsSnapshot EMPTY_STATS_SNAPSHOT = new PerformanceStatsCollector().snapshot();

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<PerformanceRunHandle> handle = new AtomicReference<>();
    private final AtomicInteger activeUsers = new AtomicInteger();
    private final AtomicInteger totalUsers = new AtomicInteger();
    private final AtomicReference<PerformanceStatsCollector> statsCollector = new AtomicReference<>();
    private final AtomicReference<Supplier<List<PerformanceWorkerResultDetail>>> resultDetailsSupplier =
            new AtomicReference<>(List::of);
    private final AtomicReference<LongFunction<PerformanceRealtimeMetrics.LiveSnapshot>> liveMetricsSupplier =
            new AtomicReference<>(nowMs -> PerformanceRealtimeMetrics.LiveSnapshot.empty());
    private final AtomicReference<IntSupplier> activeWebSocketConnectionsSupplier = new AtomicReference<>(() -> 0);
    private final AtomicReference<IntSupplier> activeSseStreamsSupplier = new AtomicReference<>(() -> 0);

    public boolean isRunning() {
        return running.get();
    }

    public void setRunning(boolean running) {
        this.running.set(running);
    }

    public void bind(PerformanceRunHandle runHandle) {
        handle.set(runHandle);
    }

    public void bindStatsCollector(PerformanceStatsCollector collector) {
        statsCollector.set(collector);
    }

    public void bindResultDetailsSupplier(Supplier<List<PerformanceWorkerResultDetail>> supplier) {
        resultDetailsSupplier.set(supplier == null ? List::of : supplier);
    }

    public void bindRealtimeMetrics(LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> liveSupplier,
                                    IntSupplier activeWebSocketConnectionsSupplier,
                                    IntSupplier activeSseStreamsSupplier) {
        this.liveMetricsSupplier.set(liveSupplier == null
                ? nowMs -> PerformanceRealtimeMetrics.LiveSnapshot.empty()
                : liveSupplier);
        this.activeWebSocketConnectionsSupplier.set(activeWebSocketConnectionsSupplier == null
                ? () -> 0
                : activeWebSocketConnectionsSupplier);
        this.activeSseStreamsSupplier.set(activeSseStreamsSupplier == null
                ? () -> 0
                : activeSseStreamsSupplier);
    }

    public void recordProgress(int activeUsers, int totalUsers) {
        this.activeUsers.set(Math.max(0, activeUsers));
        this.totalUsers.set(Math.max(0, totalUsers));
    }

    public int getActiveUsers() {
        return activeUsers.get();
    }

    public int getTotalUsers() {
        return totalUsers.get();
    }

    public PerformanceStatsSnapshot statsSnapshot() {
        PerformanceStatsCollector collector = statsCollector.get();
        return collector == null ? EMPTY_STATS_SNAPSHOT : collector.snapshot();
    }

    public PerformanceStatsProgressSnapshot progressSnapshot() {
        PerformanceStatsCollector collector = statsCollector.get();
        return collector == null ? PerformanceStatsProgressSnapshot.empty() : collector.progressSnapshot();
    }

    public PerformanceRealtimeMetrics.LiveSnapshot liveRealtimeMetrics(long nowMs) {
        LongFunction<PerformanceRealtimeMetrics.LiveSnapshot> supplier = liveMetricsSupplier.get();
        PerformanceRealtimeMetrics.LiveSnapshot snapshot = supplier == null ? null : supplier.apply(nowMs);
        return snapshot == null ? PerformanceRealtimeMetrics.LiveSnapshot.empty() : snapshot;
    }

    public int getActiveWebSocketConnections() {
        IntSupplier supplier = activeWebSocketConnectionsSupplier.get();
        return Math.max(0, supplier == null ? 0 : supplier.getAsInt());
    }

    public int getActiveSseStreams() {
        IntSupplier supplier = activeSseStreamsSupplier.get();
        return Math.max(0, supplier == null ? 0 : supplier.getAsInt());
    }

    public List<PerformanceWorkerResultDetail> resultDetailsSnapshot() {
        Supplier<List<PerformanceWorkerResultDetail>> supplier = resultDetailsSupplier.get();
        List<PerformanceWorkerResultDetail> details = supplier == null ? List.of() : supplier.get();
        return details == null ? List.of() : List.copyOf(details);
    }

    public void stop() {
        running.set(false);
        PerformanceRunHandle runHandle = handle.get();
        if (runHandle != null) {
            runHandle.stop();
        }
    }
}
