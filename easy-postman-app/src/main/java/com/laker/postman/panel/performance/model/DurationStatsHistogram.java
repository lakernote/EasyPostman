package com.laker.postman.panel.performance.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

final class DurationStatsHistogram {
    private static final int[] DURATION_BUCKET_UPPER_BOUNDS = buildDurationBucketUpperBounds();

    private final NavigableMap<Integer, Long> countsByBucket = new TreeMap<>();
    private long count;
    private long sum;
    private long min = Long.MAX_VALUE;
    private long max;

    void record(long durationMs) {
        long normalized = Math.max(0, durationMs);
        count++;
        sum += normalized;
        min = Math.min(min, normalized);
        max = Math.max(max, normalized);
        int bucket = bucketIndex(normalized);
        countsByBucket.merge(bucket, 1L, Long::sum);
    }

    void clear() {
        countsByBucket.clear();
        count = 0;
        sum = 0;
        min = Long.MAX_VALUE;
        max = 0;
    }

    long avg() {
        return count == 0 ? 0 : sum / count;
    }

    PerformanceStatsSnapshot.DurationStats snapshot() {
        if (count == 0) {
            return PerformanceStatsSnapshot.DurationStats.empty();
        }
        return new PerformanceStatsSnapshot.DurationStats(
                avg(),
                min,
                max,
                percentile(0.90),
                percentile(0.95),
                percentile(0.99)
        );
    }

    private long percentile(double percentile) {
        if (count == 0) {
            return 0;
        }
        long target = Math.max(1, (long) Math.ceil(count * percentile));
        long seen = 0;
        for (Map.Entry<Integer, Long> entry : countsByBucket.entrySet()) {
            seen += entry.getValue();
            if (seen >= target) {
                long upperBound = DURATION_BUCKET_UPPER_BOUNDS[entry.getKey()];
                return Math.min(upperBound, max);
            }
        }
        return max;
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
