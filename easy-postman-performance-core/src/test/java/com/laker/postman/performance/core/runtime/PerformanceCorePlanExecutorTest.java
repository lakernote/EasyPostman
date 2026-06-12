package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.controller.ConditionData;
import com.laker.postman.performance.core.controller.WhileData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceConditionController;
import com.laker.postman.performance.core.plan.PerformanceLoopController;
import com.laker.postman.performance.core.plan.PerformanceOnceOnlyController;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceWhileController;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.plan.PerformanceSimpleController;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.plan.PerformanceTimerElement;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    public void shouldExecuteSimpleControllerChildrenOncePerParentIteration() {
        PerformanceSimpleController simpleController = new PerformanceSimpleController(
                "simple",
                List.of(timer("simple timer", 6), sampler("inside simple", false, List.of()))
        );
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(new PerformanceLoopController("loop", loopData(2), List.of(simpleController)))
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName() + ":" + context),
                delayMs -> events.add("timer:" + delayMs)
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "timer:6",
                "sample:inside simple:ctx",
                "timer:6",
                "sample:inside simple:ctx"
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
    public void shouldExecuteConditionChildrenOnlyWhenEvaluatorReturnsTrue() {
        ConditionData conditionData = new ConditionData();
        conditionData.expression = "{{run}}";
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        new PerformanceConditionController("condition", conditionData, List.of(
                                timer("condition timer", 7),
                                sampler("conditional request", false, List.of())
                        ))
                )
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName() + ":" + context),
                delayMs -> events.add("timer:" + delayMs),
                (condition, context) -> "run".equals(context)
        );

        executor.executeIteration(groupPlan, "skip");
        executor.executeIteration(groupPlan, "run");

        assertEquals(events, List.of(
                "timer:7",
                "sample:conditional request:run"
        ));
    }

    @Test
    public void shouldExecuteWhileChildrenOnlyWhileEvaluatorReturnsTrue() {
        WhileData whileData = new WhileData();
        whileData.expression = "{{remaining}} > 0";
        whileData.intervalMs = 25;
        whileData.maxIterations = 5;
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(
                        new PerformanceWhileController("while", whileData, List.of(
                                sampler("retry request", false, List.of())
                        ))
                )
        );
        List<String> events = new ArrayList<>();
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(3);
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> {
                    events.add("sample:" + sampler.getName());
                    remaining.decrementAndGet();
                },
                delayMs -> events.add("sleep:" + delayMs),
                (condition, context) -> false,
                (controller, context) -> true,
                (whileController, context) -> remaining.get() > 0
        );

        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "sample:retry request",
                "sleep:25",
                "sample:retry request",
                "sleep:25",
                "sample:retry request"
        ));
    }

    @Test
    public void shouldExecuteOnceOnlyChildrenOnlyFirstTimePerContextState() {
        PerformanceOnceOnlyController onceOnlyController = new PerformanceOnceOnlyController(
                "once only",
                List.of(timer("once timer", 5), sampler("login", false, List.of()))
        );
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(new PerformanceLoopController("loop", loopData(3), List.of(onceOnlyController)))
        );
        List<String> events = new ArrayList<>();
        Set<String> seen = new java.util.HashSet<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName() + ":" + context),
                delayMs -> events.add("timer:" + delayMs),
                (condition, context) -> true,
                (controller, context) -> seen.add(context + ":" + System.identityHashCode(controller))
        );

        executor.executeIteration(groupPlan, "ctx-a");
        executor.executeIteration(groupPlan, "ctx-a");
        executor.executeIteration(groupPlan, "ctx-b");

        assertEquals(events, List.of(
                "timer:5",
                "sample:login:ctx-a",
                "timer:5",
                "sample:login:ctx-b"
        ));
    }

    @Test
    public void defaultOnceOnlyStateShouldResetBetweenTopLevelExecutions() {
        PerformanceOnceOnlyController onceOnlyController = new PerformanceOnceOnlyController(
                "once only",
                List.of(sampler("login", false, List.of()))
        );
        PerformanceThreadGroupPlan groupPlan = new PerformanceThreadGroupPlan(
                "group",
                new ThreadGroupData(),
                List.of(new PerformanceLoopController("loop", loopData(2), List.of(onceOnlyController)))
        );
        List<String> events = new ArrayList<>();
        PerformanceCorePlanExecutor<String> executor = new PerformanceCorePlanExecutor<>(
                () -> true,
                (sampler, context) -> events.add("sample:" + sampler.getName() + ":" + context)
        );

        executor.executeIteration(groupPlan, "ctx");
        executor.executeIteration(groupPlan, "ctx");

        assertEquals(events, List.of(
                "sample:login:ctx",
                "sample:login:ctx"
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

    private static LoopData loopData(int iterations) {
        LoopData data = new LoopData();
        data.iterations = iterations;
        return data;
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
