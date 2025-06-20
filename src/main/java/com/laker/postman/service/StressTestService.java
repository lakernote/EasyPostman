package com.laker.postman.service;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.StressResult;
import com.laker.postman.util.HttpRequestExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压力测试服务类：用于对指定的 HTTP 接口进行并发压力测试。
 */
public class StressTestService {

    /**
     * 新增支持HttpRequestItem参数的stressTest方法，内部统一用HttpRequestExecutor构建和发送请求。
     */
    public static StressResult stressTest(HttpRequestItem item, int concurrency, int requestCount) throws InterruptedException {
        List<Long> times = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(requestCount);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        AtomicInteger errorCount = new AtomicInteger(0);
        ConcurrentHashMap<Integer, Long> timeData = new ConcurrentHashMap<>();
        for (int i = 0; i < requestCount; i++) {
            int reqNum = i + 1;
            pool.execute(() -> {
                long start = System.currentTimeMillis();
                try {
                    HttpRequestExecutor.PreparedRequest req = HttpRequestExecutor.buildPreparedRequest(item);
                    HttpService.HttpResponse resp = HttpRequestExecutor.execute(req);
                    if (resp.code < 200 || resp.code > 299) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                } finally {
                    long duration = System.currentTimeMillis() - start;
                    times.add(duration);
                    timeData.put(reqNum, duration);
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        return new StressResult(times, errorCount.get(), timeData);
    }

    /**
     * 支持进度回调和总耗时统计的重载方法
     */
    public static StressResult stressTest(HttpRequestItem item, int concurrency, int requestCount, java.util.function.IntConsumer progressCallback) throws InterruptedException {
        List<Long> times = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(requestCount);
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        AtomicInteger errorCount = new AtomicInteger(0);
        ConcurrentHashMap<Integer, Long> timeData = new ConcurrentHashMap<>();
        AtomicInteger completed = new AtomicInteger(0);
        long start = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            int reqNum = i + 1;
            pool.execute(() -> {
                long reqStart = System.currentTimeMillis();
                try {
                    HttpRequestExecutor.PreparedRequest req = HttpRequestExecutor.buildPreparedRequest(item);
                    HttpService.HttpResponse resp = HttpRequestExecutor.execute(req);
                    if (resp.code < 200 || resp.code > 299) {
                        errorCount.incrementAndGet();
                    }
                } catch (Exception ex) {
                    errorCount.incrementAndGet();
                } finally {
                    long duration = System.currentTimeMillis() - reqStart;
                    times.add(duration);
                    timeData.put(reqNum, duration);
                    int done = completed.incrementAndGet();
                    if (progressCallback != null) progressCallback.accept(done);
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        long totalDuration = System.currentTimeMillis() - start;
        return new StressResult(times, errorCount.get(), timeData, totalDuration);
    }
}