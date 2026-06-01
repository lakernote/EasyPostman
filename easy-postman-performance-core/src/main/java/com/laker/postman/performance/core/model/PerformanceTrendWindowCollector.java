package com.laker.postman.performance.core.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class PerformanceTrendWindowCollector {

    private final Map<PerformanceProtocol, PerformanceSampleMeterSet> protocolStats =
            new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private PerformanceSampleMeterSet overallStats = new PerformanceSampleMeterSet("", PerformanceProtocol.HTTP);
    private volatile boolean enabled = true;

    public void record(RequestResult result) {
        if (!enabled || result == null) {
            return;
        }
        lock.readLock().lock();
        try {
            if (!enabled) {
                return;
            }
            PerformanceProtocol protocol = result.protocol == null ? PerformanceProtocol.HTTP : result.protocol;
            protocolStats.computeIfAbsent(protocol, ignored -> new PerformanceSampleMeterSet("", protocol)).record(result);
            overallStats.record(result);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setEnabled(boolean enabled) {
        lock.writeLock().lock();
        try {
            if (this.enabled == enabled) {
                return;
            }
            this.enabled = enabled;
            if (!enabled) {
                clearUnlocked();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 读取当前趋势窗口并清空窗口累计值，调用方必须把它当作一次性采样。
     */
    public PerformanceTrendSnapshot drainWindowSnapshot(int activeUsers,
                                                        int activeWebSocketConnections,
                                                        int activeSseStreams,
                                                        long samplingIntervalMs,
                                                        PerformanceRealtimeMetrics.Sample realtimeMetrics) {
        lock.writeLock().lock();
        try {
            PerformanceTrendSnapshot snapshot = new PerformanceTrendSnapshot(
                    activeUsers,
                    activeWebSocketConnections,
                    activeSseStreams,
                    PerformanceWindowMetricsFactory.fromSnapshot(overallStats.snapshot(), null, samplingIntervalMs, realtimeMetrics),
                    PerformanceWindowMetricsFactory.fromSnapshot(snapshotOf(PerformanceProtocol.HTTP),
                            PerformanceProtocol.HTTP, samplingIntervalMs, realtimeMetrics),
                    PerformanceWindowMetricsFactory.fromSnapshot(snapshotOf(PerformanceProtocol.WEBSOCKET),
                            PerformanceProtocol.WEBSOCKET, samplingIntervalMs, realtimeMetrics),
                    PerformanceWindowMetricsFactory.fromSnapshot(snapshotOf(PerformanceProtocol.SSE),
                            PerformanceProtocol.SSE, samplingIntervalMs, realtimeMetrics)
            );
            clearUnlocked();
            return snapshot;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            clearUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void clearUnlocked() {
        protocolStats.clear();
        overallStats = new PerformanceSampleMeterSet("", PerformanceProtocol.HTTP);
    }

    private PerformanceSampleMeterSnapshot snapshotOf(PerformanceProtocol protocol) {
        PerformanceSampleMeterSet stats = protocolStats.get(protocol);
        return stats == null ? null : stats.snapshot();
    }
}
