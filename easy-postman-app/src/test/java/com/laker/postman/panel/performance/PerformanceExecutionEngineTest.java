package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceExecutionEngineTest {

    @Test(timeOut = 3000)
    public void joinThreadGroupThreadsShouldInterruptChildrenAndWaitWhenInterrupted() throws Exception {
        CountDownLatch childStarted = new CountDownLatch(1);
        AtomicBoolean childInterrupted = new AtomicBoolean(false);
        AtomicBoolean cancellationCalled = new AtomicBoolean(false);

        Thread child = new Thread(() -> {
            childStarted.countDown();
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                childInterrupted.set(true);
                Thread.currentThread().interrupt();
            }
        });
        child.start();
        assertTrue(childStarted.await(500, TimeUnit.MILLISECONDS));

        Thread joiner = new Thread(() ->
                PerformanceExecutionEngine.joinThreadGroupThreads(List.of(child), () -> cancellationCalled.set(true))
        );
        joiner.start();

        Thread.sleep(100);
        joiner.interrupt();
        joiner.join(1000);

        assertFalse(joiner.isAlive());
        assertFalse(child.isAlive());
        assertTrue(childInterrupted.get());
        assertTrue(cancellationCalled.get());
    }

    @Test
    public void totalThreadsShouldClampInvalidFixedThreadCountFromPersistedConfig() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 0;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("root", NodeType.ROOT));
        root.add(new DefaultMutableTreeNode(new JMeterTreeNode("thread group", NodeType.THREAD_GROUP, threadGroupData)));

        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                null,
                () -> true,
                () -> true,
                () -> 4,
                null,
                new PerformanceStatsCollector(),
                null
        );

        assertEquals(engine.getTotalThreads(root), 1);
    }

    @Test
    public void estimateTotalRequestsShouldUseSpikeTestDuration() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.SPIKE;
        threadGroupData.spikeMinThreads = 1;
        threadGroupData.spikeMaxThreads = 1;
        threadGroupData.spikeRampUpTime = 10;
        threadGroupData.spikeHoldTime = 5;
        threadGroupData.spikeRampDownTime = 10;
        threadGroupData.spikeDuration = 120;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("root", NodeType.ROOT));
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new JMeterTreeNode("thread group", NodeType.THREAD_GROUP, threadGroupData)
        );
        group.add(new DefaultMutableTreeNode(new JMeterTreeNode("request", NodeType.REQUEST)));
        root.add(group);

        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                null,
                () -> true,
                () -> true,
                () -> 4,
                null,
                new PerformanceStatsCollector(),
                null
        );

        assertEquals(engine.estimateTotalRequests(root), 400L);
    }

    @Test
    public void estimateTotalRequestsShouldUseLongMathForNestedLoopControllers() {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = 1;

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JMeterTreeNode("root", NodeType.ROOT));
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new JMeterTreeNode("thread group", NodeType.THREAD_GROUP, threadGroupData)
        );
        JMeterTreeNode outerLoopData = new JMeterTreeNode("outer loop", NodeType.LOOP);
        outerLoopData.loopData = new LoopData();
        outerLoopData.loopData.iterations = LoopData.MAX_ITERATIONS;
        DefaultMutableTreeNode outerLoop = new DefaultMutableTreeNode(outerLoopData);
        JMeterTreeNode innerLoopData = new JMeterTreeNode("inner loop", NodeType.LOOP);
        innerLoopData.loopData = new LoopData();
        innerLoopData.loopData.iterations = LoopData.MAX_ITERATIONS;
        DefaultMutableTreeNode innerLoop = new DefaultMutableTreeNode(innerLoopData);
        innerLoop.add(new DefaultMutableTreeNode(new JMeterTreeNode("request", NodeType.REQUEST)));
        outerLoop.add(innerLoop);
        group.add(outerLoop);
        root.add(group);

        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                null,
                () -> true,
                () -> true,
                () -> 4,
                null,
                new PerformanceStatsCollector(),
                null
        );

        assertEquals(engine.estimateTotalRequests(root), 10_000_000_000L);
    }

    @Test
    public void fixedThreadGroupShouldExecuteHttpRequestsInsideLoopController() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("ok-1"));
            server.enqueue(new MockResponse().setBody("ok-2"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("http-loop-request");
            item.setName("HTTP Loop Request");
            item.setProtocol(RequestItemProtocolEnum.HTTP);
            item.setMethod("GET");
            item.setUrl(server.url("/loop").toString());

            ThreadGroupData threadGroupData = new ThreadGroupData();
            threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
            threadGroupData.numThreads = 1;
            threadGroupData.useTime = false;
            threadGroupData.loops = 1;

            DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                    new JMeterTreeNode("thread group", NodeType.THREAD_GROUP, threadGroupData)
            );
            JMeterTreeNode loopData = new JMeterTreeNode("Loop", NodeType.LOOP);
            loopData.loopData = new LoopData();
            loopData.loopData.iterations = 2;
            DefaultMutableTreeNode loopNode = new DefaultMutableTreeNode(loopData);
            loopNode.add(new DefaultMutableTreeNode(new JMeterTreeNode(item.getName(), NodeType.REQUEST, item)));
            group.add(loopNode);

            PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
            PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                    null,
                    () -> true,
                    () -> false,
                    () -> 4,
                    null,
                    statsCollector,
                    null
            );

            engine.runJMeterTreeWithProgress(group, 1, (active, total) -> {
            });

            assertEquals(server.getRequestCount(), 2);
            assertEquals(statsCollector.snapshot().totalRequests(), 2);
        }
    }

    @Test
    public void stairsStepCountShouldRoundUpWhenStepDoesNotDivideThreadRange() {
        assertEquals(PerformanceExecutionEngine.calculateStairsTotalSteps(1, 10, 4), 3);
    }

    @Test(timeOut = 3000)
    public void adjustSpikeThreadCountShouldIgnoreStaleThreadEndEntries() throws Exception {
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                null,
                () -> true,
                () -> true,
                () -> 4,
                null,
                new PerformanceStatsCollector(),
                null
        );
        ThreadGroupData threadGroupData = new ThreadGroupData();
        Thread worker = new Thread(() -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        worker.start();

        ConcurrentHashMap<Thread, Long> staleThreadEndTimes = new ConcurrentHashMap<>() {
            @Override
            public Long get(Object key) {
                if (key == worker) {
                    return null;
                }
                return super.get(key);
            }
        };
        staleThreadEndTimes.put(worker, Long.MAX_VALUE);

        try {
            Method method = PerformanceExecutionEngine.class.getDeclaredMethod(
                    "adjustSpikeThreadCount",
                    DefaultMutableTreeNode.class,
                    ThreadGroupData.class,
                    AtomicInteger.class,
                    int.class,
                    int.class,
                    BiConsumer.class,
                    int.class,
                    ConcurrentHashMap.class
            );
            method.setAccessible(true);

            method.invoke(
                    engine,
                    null,
                    threadGroupData,
                    new AtomicInteger(1),
                    0,
                    1,
                    (BiConsumer<Integer, Integer>) (active, total) -> {
                    },
                    1,
                    staleThreadEndTimes
            );
        } finally {
            worker.interrupt();
            worker.join(1_000);
        }
    }
}
