package com.laker.postman.service.collections;

import com.laker.postman.model.RequestGroup;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;

/**
 * 继承缓存并发测试
 * <p>
 * 验证 computeIfAbsent 的原子性，确保多线程场景下不会重复计算
 *
 * @author laker
 * @since 4.3.22
 */
public class InheritanceCacheConcurrencyTest {

    /**
     * 测试场景：多个线程同时请求同一个 requestId
     * <p>
     * 预期结果：
     * - 计算函数只执行一次（不会重复计算）
     * - 所有线程获取到相同的对象
     */
    @Test
    public void testComputeIfAbsent_ConcurrentAccess_ShouldComputeOnlyOnce() throws InterruptedException {
        InheritanceCache cache = new InheritanceCache();
        String requestId = "test-request-id";

        // 计数器：记录 mappingFunction 执行次数
        AtomicInteger computeCount = new AtomicInteger(0);

        // 模拟耗时计算
        RequestGroup group = new RequestGroup();
        group.setName("Test Group");
        List<RequestGroup> expectedGroupChain = List.of(group);

        // 启动 10 个线程同时请求
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);  // 用于同步启动
        CountDownLatch doneLatch = new CountDownLatch(threadCount);  // 用于等待完成

        @SuppressWarnings("unchecked")
        List<RequestGroup>[] results = new List[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    // 等待所有线程就绪
                    startLatch.await();

                    // 同时访问缓存
                    results[index] = cache.computeIfAbsent(requestId, id -> {
                        computeCount.incrementAndGet();

                        // 模拟耗时计算
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        return expectedGroupChain;
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        // 启动所有线程
        startLatch.countDown();

        // 等待所有线程完成
        doneLatch.await();

        // 验证结果
        assertEquals(computeCount.get(), 1, "计算函数应该只执行一次");

        // 验证所有线程获取到相同的对象
        for (List<RequestGroup> result : results) {
            assertEquals(result, expectedGroupChain, "所有线程应该获取到相同的对象");
        }

    }

    /**
     * 测试场景：不同线程请求不同的 requestId
     * <p>
     * 预期结果：
     * - 每个 requestId 都会执行计算
     * - 缓存中有多个条目
     */
    @Test
    public void testComputeIfAbsent_DifferentKeys_ShouldComputeEach() throws InterruptedException {
        InheritanceCache cache = new InheritanceCache();

        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger computeCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final String requestId = "request-" + i;
            new Thread(() -> {
                try {
                    startLatch.await();

                    cache.computeIfAbsent(requestId, id -> {
                        computeCount.incrementAndGet();
                        RequestGroup g = new RequestGroup();
                        g.setName(id);
                        return List.of(g);
                    });

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown();
        doneLatch.await();

        // 每个 key 都应该计算一次
        assertEquals(computeCount.get(), threadCount, "每个不同的 key 都应该计算一次");
    }
}
