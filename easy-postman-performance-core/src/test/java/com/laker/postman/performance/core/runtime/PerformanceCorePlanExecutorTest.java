package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;

public class PerformanceCorePlanExecutorTest {

    @Test
    public void shouldExecuteLoopControllerTimerAndSamplerInOrderWithPureContext() {
        LoopData loopData = new LoopData();
        loopData.iterations = 2;
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        new PerformanceLoopController("loop", loopData, List.of(
                                timer("loop timer", 11),
                                sampler("loop request", false, List.of())
                        ))
                )
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName() + ":" + context),
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "timer:11",
                "sample:loop request:ctx",
                "timer:11",
                "sample:loop request:ctx"
        ));
    }

    @Test
    public void shouldApplyScopedTimersBeforeEachSamplerRegardlessOfSiblingOrder() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        sampler("first request", false, List.of()),
                        timer("group scoped timer", 50),
                        sampler("second request", false, List.of())
                )
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName()),
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "timer:50",
                "sample:first request",
                "timer:50",
                "sample:second request"
        ));
    }

    @Test
    public void shouldApplySamplerChildTimersOnlyWhenSamplerDoesNotExecuteChildrenInOwnOrder() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        sampler("http request", false, List.of(timer("http child timer", 21))),
                        sampler("websocket request", true, List.of(timer("websocket child timer", 31)))
                )
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName()),
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "timer:21",
                "sample:http request",
                "sample:websocket request"
        ));
    }

    @Test
    public void shouldSkipNullPlanAndStopBeforeSamplerWhenTimerIsInterrupted() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        timer("timer", 10),
                        sampler("request", false, List.of())
                )
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName()),
                delayMs -> {
                    events.add("timer:" + delayMs);
                    throw new InterruptedException("stop");
                }
        );

        executor.executeIteration(null, "ctx");
        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of("timer:10"));
        Thread.interrupted();
    }

    @Test
    public void shouldStopBeforeLaterElementsWhenRunningSupplierTurnsFalse() {
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        sampler("first request", false, List.of()),
                        sampler("second request", false, List.of())
                )
        );
        AtomicBoolean running = new AtomicBoolean(true);
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                running::get,
                (sampler, context) -> {
                    events.add("sample:" + sampler.getName());
                    running.set(false);
                }
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of("sample:first request"));
    }

    private static PerformanceTimerElement timer(String name, int delayMs) {
        TimerData timerData = new TimerData();
        timerData.delayMs = delayMs;
        return new PerformanceTimerElement(name, timerData);
    }

    private static PerformanceSampler sampler(String name,
                                              boolean executesChildrenInSamplerOrder,
                                              List<PerformancePlanElement> children) {
        return new RecordingSampler(name, executesChildrenInSamplerOrder, children);
    }

    private record RecordingSampler(String name,
                                    boolean executesChildrenInSamplerOrder,
                                    List<PerformancePlanElement> children) implements PerformanceSampler {

        @Override
        public String getName() {
            return name;
        }

        @Override
        public NodeType getType() {
            return NodeType.REQUEST;
        }

        @Override
        public List<PerformancePlanElement> getChildren() {
            return children == null ? List.of() : List.copyOf(children);
        }
    }
}
