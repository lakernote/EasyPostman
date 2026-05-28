package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutor;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.panel.performance.plan.PerformanceTestPlanCompiler;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import com.laker.postman.panel.performance.runtime.PerformanceIterationContextFactory;
import com.laker.postman.panel.performance.runtime.PerformancePlanExecutor;
import com.laker.postman.panel.performance.runtime.PerformanceSamplerExecutor;
import com.laker.postman.performance.core.runtime.PerformanceVirtualUserCoordinator;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;
import com.laker.postman.service.variable.ExecutionVariableContext;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;

public class PerformancePlanExecutorTest {

    @Test
    public void shouldExecuteLoopControllerTimerAndRequestSamplerInOrder() {
        DefaultMutableTreeNode groupNode = threadGroupNode();
        DefaultMutableTreeNode loopNode = loopNode(2);
        loopNode.add(timerNode("loop timer", 11));
        loopNode.add(requestNode("loop request", RequestItemProtocolEnum.HTTP));
        groupNode.add(loopNode);

        List<String> events = new ArrayList<>();
        PerformancePlanExecutor executor = newExecutor(
                () -> true,
                events,
                false,
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(
                PerformanceTestPlanCompiler.compile(groupNode).getThreadGroups().get(0),
                new ExecutionVariableContext()
        );

        assertEquals(events, List.of(
                "timer:11",
                "request:loop request",
                "timer:11",
                "request:loop request"
        ));
    }

    @Test
    public void shouldApplyScopedTimersBeforeEachSamplerRegardlessOfSiblingOrder() {
        DefaultMutableTreeNode groupNode = threadGroupNode();
        groupNode.add(requestNode("first request", RequestItemProtocolEnum.HTTP));
        groupNode.add(timerNode("group scoped timer", 50));
        DefaultMutableTreeNode loopNode = loopNode(1);
        loopNode.add(requestNode("loop request", RequestItemProtocolEnum.HTTP));
        groupNode.add(loopNode);

        List<String> events = new ArrayList<>();
        PerformancePlanExecutor executor = newExecutor(
                () -> true,
                events,
                false,
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(
                PerformanceTestPlanCompiler.compile(groupNode).getThreadGroups().get(0),
                new ExecutionVariableContext()
        );

        assertEquals(events, List.of(
                "timer:50",
                "request:first request",
                "timer:50",
                "request:loop request"
        ));
    }

    @Test
    public void shouldApplyHttpRequestChildTimerBeforeSampler() {
        DefaultMutableTreeNode groupNode = threadGroupNode();
        DefaultMutableTreeNode httpRequest = requestNode("http request", RequestItemProtocolEnum.HTTP);
        httpRequest.add(timerNode("http child timer", 21));
        groupNode.add(httpRequest);

        List<String> events = new ArrayList<>();
        PerformancePlanExecutor executor = newExecutor(
                () -> true,
                events,
                false,
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(
                PerformanceTestPlanCompiler.compile(groupNode).getThreadGroups().get(0),
                new ExecutionVariableContext()
        );

        assertEquals(events, List.of(
                "timer:21",
                "request:http request"
        ));
    }

    @Test
    public void shouldRunHttpRequestChildTimersBeforeSamplerButSkipThemForWebSocket() {
        DefaultMutableTreeNode groupNode = threadGroupNode();
        DefaultMutableTreeNode httpRequest = requestNode("http request", RequestItemProtocolEnum.HTTP);
        httpRequest.add(timerNode("http child timer", 21));
        DefaultMutableTreeNode wsRequest = requestNode("ws request", RequestItemProtocolEnum.WEBSOCKET);
        wsRequest.add(timerNode("ws child timer", 31));
        groupNode.add(httpRequest);
        groupNode.add(wsRequest);

        List<String> events = new ArrayList<>();
        PerformancePlanExecutor executor = newExecutor(
                () -> true,
                events,
                true,
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(
                PerformanceTestPlanCompiler.compile(groupNode).getThreadGroups().get(0),
                new ExecutionVariableContext()
        );

        assertEquals(events, List.of(
                "timer:21",
                "request:http request",
                "request:ws request"
        ));
    }

    @Test
    public void shouldCreateIterationContextWithCsvRowForCurrentVirtualUser() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                new CsvDataSetData(
                        "users.csv",
                        List.of("name"),
                        List.of(Map.of("name", "alice"), Map.of("name", "bob"))
                ),
                List.of()
        );
        PerformanceVirtualUserCoordinator virtualUsers = new PerformanceVirtualUserCoordinator();
        PerformanceIterationContextFactory factory = new PerformanceIterationContextFactory(virtualUsers);
        List<ExecutionVariableContext> contexts = new ArrayList<>();

        virtualUsers.newThread("test-vu", (active, total) -> {
        }, 1, () -> contexts.add(factory.create(groupPlan, 3))).run();

        assertEquals(contexts.size(), 1);
        assertEquals(contexts.get(0).getIterationIndex(), 0);
        assertEquals(contexts.get(0).getIterationCount(), 3);
        assertEquals(contexts.get(0).getIterationData().get("name"), "alice");
    }

    private static PerformancePlanExecutor newExecutor(AtomicBoolean running,
                                                       List<String> events,
                                                       boolean webSocketByName,
                                                       PerformancePlanExecutor.TimerSleeper timerSleeper) {
        return newExecutor(running::get, events, webSocketByName, timerSleeper);
    }

    private static PerformancePlanExecutor newExecutor(java.util.function.BooleanSupplier running,
                                                       List<String> events,
                                                       boolean webSocketByName,
                                                       PerformancePlanExecutor.TimerSleeper timerSleeper) {
        PerformanceRequestExecutor requestExecutor = new PerformanceRequestExecutor(
                running,
                throwable -> false,
                ConcurrentHashMap.newKeySet(),
                ConcurrentHashMap.newKeySet()
        ) {
            @Override
            public PerformanceRequestExecutionResult execute(PerformanceRequestSampler requestSampler,
                                                             ExecutionVariableContext iterationContext) {
                HttpRequestItem requestItem = requestSampler.getHttpRequestItem();
                events.add("request:" + requestItem.getName());
                HttpResponse response = new HttpResponse();
                response.costMs = 1;
                response.endTime = System.currentTimeMillis();
                boolean webSocket = webSocketByName && requestItem.getProtocol().isWebSocketProtocol();
                return new PerformanceRequestExecutionResult(
                        requestItem.getId(),
                        requestItem.getName(),
                        null,
                        response,
                        "",
                        List.of(),
                        false,
                        false,
                        webSocket,
                        System.currentTimeMillis(),
                        1
                );
            }
        };
        PerformanceSamplerExecutor samplerExecutor = new PerformanceSamplerExecutor(
                running,
                () -> false,
                requestExecutor,
                new PerformanceResultCollector(List.of())
        );
        return new PerformancePlanExecutor(running, samplerExecutor, timerSleeper);
    }

    private static DefaultMutableTreeNode threadGroupNode() {
        ThreadGroupData data = new ThreadGroupData();
        data.threadMode = ThreadGroupData.ThreadMode.FIXED;
        data.numThreads = 1;
        data.useTime = false;
        data.loops = 1;
        return new DefaultMutableTreeNode(new JMeterTreeNode("group", NodeType.THREAD_GROUP, data));
    }

    private static DefaultMutableTreeNode loopNode(int iterations) {
        LoopData data = new LoopData();
        data.iterations = iterations;
        return new DefaultMutableTreeNode(new JMeterTreeNode("loop", NodeType.LOOP, data));
    }

    private static DefaultMutableTreeNode timerNode(String name, int delayMs) {
        TimerData data = new TimerData();
        data.delayMs = delayMs;
        return new DefaultMutableTreeNode(new JMeterTreeNode(name, NodeType.TIMER, data));
    }

    private static DefaultMutableTreeNode requestNode(String name, RequestItemProtocolEnum protocol) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(name + "-id");
        item.setName(name);
        item.setProtocol(protocol);
        return new DefaultMutableTreeNode(new JMeterTreeNode(name, NodeType.REQUEST, item));
    }
}
