package com.laker.postman.performance.core.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 样本时间范围。累计报表用完整 sample span，趋势图用外部 step interval，避免两者语义混在一起。
 */
final class PerformanceSampleTimeWindow {
    private final AtomicLong firstStartMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong firstEndMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong lastEndMs = new AtomicLong();

    void record(long startTimeMs, long endTimeMs) {
        long safeStart = Math.max(0L, startTimeMs);
        long safeEnd = Math.max(safeStart, endTimeMs);
        updateMin(firstStartMs, safeStart);
        updateMin(firstEndMs, safeEnd);
        updateMax(lastEndMs, safeEnd);
    }

    long firstStartMs() {
        long value = firstStartMs.get();
        return value == Long.MAX_VALUE ? 0L : value;
    }

    long firstEndMs() {
        long value = firstEndMs.get();
        return value == Long.MAX_VALUE ? 0L : value;
    }

    long lastEndMs() {
        return lastEndMs.get();
    }

    double spanSeconds() {
        long firstStart = firstStartMs.get();
        long lastEnd = lastEndMs.get();
        if (firstStart == Long.MAX_VALUE || lastEnd <= firstStart) {
            return 0;
        }
        return Math.max(0.001, (lastEnd - firstStart) / 1000.0);
    }

    double completionSpanSeconds() {
        long firstEnd = firstEndMs.get();
        long lastEnd = lastEndMs.get();
        if (firstEnd == Long.MAX_VALUE || lastEnd <= firstEnd) {
            return 0;
        }
        return Math.max(0.001, (lastEnd - firstEnd) / 1000.0);
    }

    void clear() {
        firstStartMs.set(Long.MAX_VALUE);
        firstEndMs.set(Long.MAX_VALUE);
        lastEndMs.set(0);
    }

    private static void updateMin(AtomicLong target, long value) {
        long observed;
        do {
            observed = target.get();
            if (value >= observed) {
                return;
            }
        } while (!target.compareAndSet(observed, value));
    }

    private static void updateMax(AtomicLong target, long value) {
        long observed;
        do {
            observed = target.get();
            if (value <= observed) {
                return;
            }
        } while (!target.compareAndSet(observed, value));
    }
}
