package com.laker.postman.performance.core.model;

import java.util.concurrent.atomic.LongAdder;

/**
 * 单调递增计数器，对应 Micrometer Counter 语义：只记录累计增量，速率由调用方按窗口派生。
 */
final class PerformanceCounter {
    private final LongAdder count = new LongAdder();

    void increment() {
        increment(1);
    }

    void increment(long amount) {
        if (amount > 0) {
            count.add(amount);
        }
    }

    long count() {
        return count.sum();
    }

    void clear() {
        count.reset();
    }
}
