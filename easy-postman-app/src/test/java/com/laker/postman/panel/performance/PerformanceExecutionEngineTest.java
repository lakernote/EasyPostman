package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.assertion.AssertionData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.PerformanceStatsCollector;
import com.laker.postman.panel.performance.model.PerformanceStatsCollectorListener;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.runtime.PerformanceExecutionEngine;
import com.laker.postman.panel.performance.runtime.PerformanceIterationContextFactory;
import com.laker.postman.panel.performance.runtime.PerformancePlanExecutor;
import com.laker.postman.panel.performance.runtime.PerformanceThreadGroupRunner;
import com.laker.postman.panel.performance.runtime.PerformanceVirtualUserCoordinator;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
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

    @Test
    public void executionApisShouldNotExposeSwingTreeCompatibilityMethods() {
        assertFalse(hasDefaultMutableTreeNodeParameter(PerformanceExecutionEngine.class));
    }

    @Test
    public void executionEngineShouldNotExposeSwingResultTablePanelParameters() {
        assertFalse(hasParameterType(PerformanceExecutionEngine.class, PerformanceResultTablePanel.class));
    }

    @Test
    public void executionEngineShouldNotExposeRawResultListenerCollections() {
        assertFalse(hasConstructorParameter(PerformanceExecutionEngine.class, List.class));
    }

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
                emptyResultCollector()
        );

        assertEquals(engine.getTotalThreads(PerformanceTestPlanCompiler.compile(root)), 1);
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
                emptyResultCollector()
        );

        assertEquals(engine.estimateTotalRequests(PerformanceTestPlanCompiler.compile(root)), 400L);
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
                emptyResultCollector()
        );

        assertEquals(engine.estimateTotalRequests(PerformanceTestPlanCompiler.compile(root)), 10_000_000_000L);
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
                    statsResultCollector(statsCollector)
            );

            engine.runTestPlanWithProgress(PerformanceTestPlanCompiler.compile(group), 1, (active, total) -> {
            });

            assertEquals(server.getRequestCount(), 2);
            assertEquals(statsCollector.snapshot().totalRequests(), 2);
        }
    }

    @Test
    public void compiledPlanExecutionShouldRunHttpAssertions() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("http-assertion-request");
            item.setName("HTTP Assertion Request");
            item.setProtocol(RequestItemProtocolEnum.HTTP);
            item.setMethod("GET");
            item.setUrl(server.url("/assertion").toString());

            DefaultMutableTreeNode request = new DefaultMutableTreeNode(
                    new JMeterTreeNode(item.getName(), NodeType.REQUEST, item)
            );
            request.add(responseCodeAssertion("201"));
            DefaultMutableTreeNode group = fixedThreadGroup(1);
            group.add(request);

            PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
            new PerformanceExecutionEngine(
                    null,
                    () -> true,
                    () -> false,
                    () -> 4,
                    null,
                    statsResultCollector(statsCollector)
            ).runTestPlanWithProgress(PerformanceTestPlanCompiler.compile(group), 1, (active, total) -> {
            });

            assertEquals(server.getRequestCount(), 1);
            assertEquals(statsCollector.snapshot().totalRequests(), 1);
            assertEquals(statsCollector.snapshot().successRequests(), 0);
        }
    }

    @Test
    public void compiledPlanExecutionShouldValidateWebSocketStagesBeforeNetwork() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("unexpected"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("compiled-ws-missing-connect");
            item.setName("Compiled WS Missing Connect");
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            item.setMethod("GET");
            item.setUrl(server.url("/socket").toString().replaceFirst("^http", "ws"));

            JMeterTreeNode requestData = new JMeterTreeNode(item.getName(), NodeType.REQUEST, item);
            requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            DefaultMutableTreeNode group = fixedThreadGroup(1);
            group.add(new DefaultMutableTreeNode(requestData));

            PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
            new PerformanceExecutionEngine(
                    null,
                    () -> true,
                    () -> false,
                    () -> 4,
                    null,
                    statsResultCollector(statsCollector)
            ).runTestPlanWithProgress(PerformanceTestPlanCompiler.compile(group), 1, (active, total) -> {
            });

            assertEquals(server.getRequestCount(), 0);
            assertEquals(statsCollector.snapshot().totalRequests(), 1);
            assertEquals(statsCollector.snapshot().successRequests(), 0);
        }
    }

    @Test
    public void compiledPlanExecutionShouldValidateSseStagesBeforeNetwork() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("data: unexpected\n\n"));
            server.start();

            HttpRequestItem item = new HttpRequestItem();
            item.setId("compiled-sse-missing-stages");
            item.setName("Compiled SSE Missing Stages");
            item.setProtocol(RequestItemProtocolEnum.HTTP);
            item.setMethod("GET");
            item.setUrl(server.url("/events").toString());
            item.setHeadersList(List.of(new HttpHeader(true, "Accept", "text/event-stream")));

            JMeterTreeNode requestData = new JMeterTreeNode(item.getName(), NodeType.REQUEST, item);
            requestData.ssePerformanceData = new SsePerformanceData();
            DefaultMutableTreeNode group = fixedThreadGroup(1);
            group.add(new DefaultMutableTreeNode(requestData));

            PerformanceStatsCollector statsCollector = new PerformanceStatsCollector();
            new PerformanceExecutionEngine(
                    null,
                    () -> true,
                    () -> false,
                    () -> 4,
                    null,
                    statsResultCollector(statsCollector)
            ).runTestPlanWithProgress(PerformanceTestPlanCompiler.compile(group), 1, (active, total) -> {
            });

            assertEquals(server.getRequestCount(), 0);
            assertEquals(statsCollector.snapshot().totalRequests(), 1);
            assertEquals(statsCollector.snapshot().successRequests(), 0);
        }
    }

    @Test
    public void stairsStepCountShouldRoundUpWhenStepDoesNotDivideThreadRange() {
        assertEquals(PerformanceExecutionEngine.calculateStairsTotalSteps(1, 10, 4), 3);
    }

    @Test(timeOut = 3000)
    public void adjustSpikeThreadCountShouldIgnoreStaleThreadEndEntries() throws Exception {
        PerformanceThreadGroupRunner runner = new PerformanceThreadGroupRunner(
                null,
                () -> true,
                () -> 0L,
                () -> {
                },
                new PerformanceVirtualUserCoordinator(),
                null,
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
            runner.adjustSpikeThreadCount(
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

    private static DefaultMutableTreeNode fixedThreadGroup(int loops) {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.threadMode = ThreadGroupData.ThreadMode.FIXED;
        threadGroupData.numThreads = 1;
        threadGroupData.useTime = false;
        threadGroupData.loops = loops;
        return new DefaultMutableTreeNode(new JMeterTreeNode("thread group", NodeType.THREAD_GROUP, threadGroupData));
    }

    private static DefaultMutableTreeNode responseCodeAssertion(String expectedStatus) {
        AssertionData assertionData = new AssertionData();
        assertionData.type = "Response Code";
        assertionData.operator = "=";
        assertionData.value = expectedStatus;
        return new DefaultMutableTreeNode(new JMeterTreeNode("status assertion", NodeType.ASSERTION, assertionData));
    }

    private static boolean hasDefaultMutableTreeNodeParameter(Class<?> type) {
        return hasParameterType(type, DefaultMutableTreeNode.class);
    }

    private static PerformanceResultCollector emptyResultCollector() {
        return new PerformanceResultCollector(List.of());
    }

    private static PerformanceResultCollector statsResultCollector(PerformanceStatsCollector statsCollector) {
        return new PerformanceResultCollector(List.of(new PerformanceStatsCollectorListener(statsCollector)));
    }

    private static boolean hasParameterType(Class<?> type, Class<?> parameterType) {
        boolean methodParameter = java.util.Arrays.stream(type.getMethods())
                .flatMap(method -> java.util.Arrays.stream(method.getParameterTypes()))
                .anyMatch(parameterType::equals);
        boolean constructorParameter = java.util.Arrays.stream(type.getConstructors())
                .flatMap(constructor -> java.util.Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(parameterType::equals);
        return methodParameter || constructorParameter;
    }

    private static boolean hasConstructorParameter(Class<?> type, Class<?> parameterType) {
        return java.util.Arrays.stream(type.getConstructors())
                .flatMap(constructor -> java.util.Arrays.stream(constructor.getParameterTypes()))
                .anyMatch(parameterType::equals);
    }
}
