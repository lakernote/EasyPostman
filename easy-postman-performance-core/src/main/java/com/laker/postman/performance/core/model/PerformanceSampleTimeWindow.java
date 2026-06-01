package com.laker.postman.performance.core.model;

/**
 * 样本时间范围。累计报表用完整 sample span，趋势图用外部 step interval，避免两者语义混在一起。
 */
final class PerformanceSampleTimeWindow {
    private long firstStartMs = Long.MAX_VALUE;
    private long firstEndMs = Long.MAX_VALUE;
    private long lastEndMs;

    void record(long startTimeMs, long endTimeMs) {
        long safeStart = Math.max(0L, startTimeMs);
        long safeEnd = Math.max(safeStart, endTimeMs);
        firstStartMs = Math.min(firstStartMs, safeStart);
        firstEndMs = Math.min(firstEndMs, safeEnd);
        lastEndMs = Math.max(lastEndMs, safeEnd);
    }

    long firstStartMs() {
        return firstStartMs == Long.MAX_VALUE ? 0L : firstStartMs;
    }

    long firstEndMs() {
        return firstEndMs == Long.MAX_VALUE ? 0L : firstEndMs;
    }

    long lastEndMs() {
        return lastEndMs;
    }

    double spanSeconds() {
        if (firstStartMs == Long.MAX_VALUE || lastEndMs <= firstStartMs) {
            return 0;
        }
        return Math.max(0.001, (lastEndMs - firstStartMs) / 1000.0);
    }

    double completionSpanSeconds() {
        if (firstEndMs == Long.MAX_VALUE || lastEndMs <= firstEndMs) {
            return 0;
        }
        return Math.max(0.001, (lastEndMs - firstEndMs) / 1000.0);
    }

    void clear() {
        firstStartMs = Long.MAX_VALUE;
        firstEndMs = Long.MAX_VALUE;
        lastEndMs = 0;
    }
}
