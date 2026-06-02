package com.laker.postman.util;

import java.util.concurrent.TimeUnit;

/**
 * Measures elapsed time with System.nanoTime while keeping a wall-clock start timestamp for reports.
 */
public final class MonotonicStopwatch {
    private final long startWallTimeMs;
    private final long startNanos;

    private MonotonicStopwatch(long startWallTimeMs, long startNanos) {
        this.startWallTimeMs = Math.max(0L, startWallTimeMs);
        this.startNanos = startNanos;
    }

    public static MonotonicStopwatch start() {
        return new MonotonicStopwatch(System.currentTimeMillis(), System.nanoTime());
    }

    public static MonotonicStopwatch startedAt(long startWallTimeMs, long startNanos) {
        return new MonotonicStopwatch(startWallTimeMs, startNanos);
    }

    public long startWallTimeMs() {
        return startWallTimeMs;
    }

    public long elapsedMs() {
        return elapsedMs(System.nanoTime());
    }

    public long elapsedMs(long nowNanos) {
        long elapsedNanos = Math.max(0L, nowNanos - startNanos);
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

    public long projectedEndTimeMs() {
        return projectedEndTimeMs(System.nanoTime());
    }

    public long projectedEndTimeMs(long nowNanos) {
        return startWallTimeMs + elapsedMs(nowNanos);
    }
}
