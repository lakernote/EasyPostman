package com.laker.postman.performance.core.runtime;

import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class PerformanceCoreExecutionEngineTest {

    @Test(timeOut = 3000)
    public void shouldRunPlanWithGenericIterationContextAndExposeCoreState() {
        AtomicBoolean running = new AtomicBoolean(true);
        RecordingNetworkControl networkControl = new RecordingNetworkControl();
        List<String> executions = new CopyOnWriteArrayList<>();
        PerformanceCoreExecutionEngine<String> engine = new PerformanceCoreExecutionEngine<>(
                running::get,
                networkControl,
                (groupPlan, iterationCount) -> "ctx:" + groupPlan.getName() + ":" + iterationCount,
                (groupPlan, iterationContext) -> executions.add(iterationContext),
                PerformanceRunListener.NOOP
        );
        PerformanceTestPlan plan = new PerformanceTestPlan(List.of(fixedGroupWithSampler("group", 2)));

        engine.beginRun(123L);
        engine.runTestPlan(plan, engine.getTotalThreads(plan));
        networkControl.activeWebSockets.set(3);
        networkControl.activeSseStreams.set(2);
        engine.cancelAllNetworkCalls();

        assertEquals(engine.getStartTime(), 123L);
        assertEquals(engine.getTotalThreads(plan), 1);
        assertEquals(engine.estimateTotalRequests(plan), 2L);
        assertEquals(executions, List.of("ctx:group:2", "ctx:group:2"));
        assertEquals(engine.getActiveThreads(), 0);
        assertEquals(engine.getActiveWebSockets(), 3);
        assertEquals(engine.getActiveSseStreams(), 2);
        assertEquals(networkControl.cancelAllCount.get(), 1);
        assertEquals(engine.liveRealtimeMetrics(123L).webSocket().activeSessions(), 0);
    }

    @Test
    public void shouldSkipPlanExecutionWhenNotRunning() {
        List<String> executions = new CopyOnWriteArrayList<>();
        PerformanceCoreExecutionEngine<String> engine = new PerformanceCoreExecutionEngine<>(
                () -> false,
                PerformanceNetworkControl.NOOP,
                (groupPlan, iterationCount) -> "ctx",
                (groupPlan, iterationContext) -> executions.add(iterationContext),
                PerformanceRunListener.NOOP
        );

        engine.runTestPlan(new PerformanceTestPlan(List.of(fixedGroupWithSampler("group", 1))), 1);

        assertFalse(engine.isRunning());
        assertEquals(executions.size(), 0);
    }

    private static PerformanceThreadGroupPlan fixedGroupWithSampler(String name, int loops) {
        ThreadGroupData data = new ThreadGroupData();
        data.threadMode = ThreadGroupData.ThreadMode.FIXED;
        data.numThreads = 1;
        data.useTime = false;
        data.loops = loops;
        return new PerformanceThreadGroupPlan(name, data, List.of(
                new PerformanceCoreRequestSampler("request", PerformanceRequestSnapshot.empty(), null, List.of())
        ));
    }

    private static final class RecordingNetworkControl implements PerformanceNetworkControl {
        private final AtomicInteger cancelAllCount = new AtomicInteger();
        private final AtomicInteger activeWebSockets = new AtomicInteger();
        private final AtomicInteger activeSseStreams = new AtomicInteger();

        @Override
        public int activeWebSocketCount() {
            return activeWebSockets.get();
        }

        @Override
        public int activeSseCount() {
            return activeSseStreams.get();
        }

        @Override
        public void cancelAll() {
            cancelAllCount.incrementAndGet();
        }
    }
}
