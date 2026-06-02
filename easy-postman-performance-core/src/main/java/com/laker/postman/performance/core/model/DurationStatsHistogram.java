package com.laker.postman.performance.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

final class DurationStatsHistogram {
    private static final int[] DURATION_BUCKET_UPPER_BOUNDS = buildDurationBucketUpperBounds();

    private final ConcurrentMap<Integer, LongAdder> countsByBucket = new ConcurrentHashMap<>();
    private final LongAdder count = new LongAdder();
    private final LongAdder sum = new LongAdder();
    private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong max = new AtomicLong();

    void record(long durationMs) {
        long normalized = Math.max(0, durationMs);
        sum.add(normalized);
        updateMin(normalized);
        updateMax(normalized);
        int bucket = bucketIndex(normalized);
        countsByBucket.computeIfAbsent(bucket, ignored -> new LongAdder()).increment();
        count.increment();
    }

    void clear() {
        countsByBucket.clear();
        count.reset();
        sum.reset();
        min.set(Long.MAX_VALUE);
        max.set(0);
    }

    long avg() {
        long currentCount = count.sum();
        return currentCount == 0 ? 0 : sum.sum() / currentCount;
    }

    PerformanceStatsSnapshot.DurationStats snapshot() {
        long currentCount = count.sum();
        if (currentCount == 0) {
            return PerformanceStatsSnapshot.DurationStats.empty();
        }
        return new PerformanceStatsSnapshot.DurationStats(
                avg(),
                min.get(),
                max.get(),
                percentile(currentCount, 0.90),
                percentile(currentCount, 0.95),
                percentile(currentCount, 0.99)
        );
    }

    private long percentile(long currentCount, double percentile) {
        if (currentCount == 0) {
            return 0;
        }
        long target = Math.max(1, (long) Math.ceil(currentCount * percentile));
        long seen = 0;
        for (Map.Entry<Integer, Long> entry : snapshotCountsByBucket().entrySet()) {
            seen += entry.getValue();
            if (seen >= target) {
                long upperBound = DURATION_BUCKET_UPPER_BOUNDS[entry.getKey()];
                return Math.min(upperBound, max.get());
            }
        }
        return max.get();
    }

    private Map<Integer, Long> snapshotCountsByBucket() {
        Map<Integer, Long> snapshot = new java.util.TreeMap<>();
        countsByBucket.forEach((bucket, bucketCount) -> {
            long value = bucketCount.sum();
            if (value > 0) {
                snapshot.put(bucket, value);
            }
        });
        return snapshot;
    }

    private void updateMin(long value) {
        long observed;
        do {
            observed = min.get();
            if (value >= observed) {
                return;
            }
        } while (!min.compareAndSet(observed, value));
    }

    private void updateMax(long value) {
        long observed;
        do {
            observed = max.get();
            if (value <= observed) {
                return;
            }
        } while (!max.compareAndSet(observed, value));
    }

    private static int bucketIndex(long durationMs) {
        int normalized = durationMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationMs;
        int low = 0;
        int high = DURATION_BUCKET_UPPER_BOUNDS.length - 1;
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (normalized <= DURATION_BUCKET_UPPER_BOUNDS[mid]) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    private static int[] buildDurationBucketUpperBounds() {
        List<Integer> bounds = new ArrayList<>(16_500);
        addRange(bounds, 0, 1_000, 1);
        addRange(bounds, 1_010, 60_000, 10);
        addRange(bounds, 60_100, 600_000, 100);
        addRange(bounds, 601_000, 3_600_000, 1_000);
        bounds.add(Integer.MAX_VALUE);
        int[] result = new int[bounds.size()];
        for (int i = 0; i < bounds.size(); i++) {
            result[i] = bounds.get(i);
        }
        return result;
    }

    private static void addRange(List<Integer> bounds, int start, int end, int step) {
        for (int value = start; value <= end; value += step) {
            bounds.add(value);
        }
    }
}
