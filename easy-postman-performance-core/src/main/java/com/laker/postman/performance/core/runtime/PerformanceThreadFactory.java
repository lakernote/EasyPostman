package com.laker.postman.performance.core.runtime;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class PerformanceThreadFactory implements ThreadFactory {

    private static final ConcurrentMap<String, AtomicInteger> DIRECT_THREAD_COUNTERS = new ConcurrentHashMap<>();

    private final String namePrefix;
    private final boolean daemon;
    private final AtomicInteger counter = new AtomicInteger(1);

    private PerformanceThreadFactory(String namePrefix, boolean daemon) {
        this.namePrefix = namePrefix;
        this.daemon = daemon;
    }

    public static ThreadFactory daemonFactory(String namePrefix) {
        return new PerformanceThreadFactory(namePrefix, true);
    }

    public static Thread newDaemonThread(String namePrefix, Runnable task) {
        int threadNumber = DIRECT_THREAD_COUNTERS
                .computeIfAbsent(namePrefix, ignored -> new AtomicInteger(1))
                .getAndIncrement();
        return configureThread(new Thread(task, namePrefix + "-" + threadNumber), true);
    }

    @Override
    public Thread newThread(Runnable task) {
        return configureThread(new Thread(task, namePrefix + "-" + counter.getAndIncrement()), daemon);
    }

    private static Thread configureThread(Thread thread, boolean daemon) {
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler((failedThread, error) ->
                log.error("性能测试后台线程异常: {}", failedThread.getName(), error));
        return thread;
    }
}
