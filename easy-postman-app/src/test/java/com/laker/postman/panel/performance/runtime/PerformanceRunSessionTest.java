package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.core.runtime.PerformanceRunSummary;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.execution.PerformanceNetworkRuntime;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class PerformanceRunSessionTest {

    @Test(timeOut = 3000)
    public void startShouldRunPlanWithoutSwingComponents() throws Exception {
        AtomicBoolean running = new AtomicBoolean(false);
        List<PerformanceRunSummary> completed = new ArrayList<>();
        PerformanceResultSink sink = new PerformanceResultSink() {
            @Override
            public void onComplete(PerformanceRunSummary summary) {
                completed.add(summary);
            }
        };
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                running::get,
                () -> true,
                () -> 64,
                new PerformanceResultCollector(PerformanceResultSink.NOOP)
        );
        PerformanceRunSession session = new PerformanceRunSession(
                running::get,
                running::set,
                engine
        );

        PerformanceRunHandle handle = session.start(PerformanceRunRequest.builder()
                .plan(new PerformanceTestPlan(List.of()))
                .resultSink(sink)
                .build());
        handle.join(1000);

        assertFalse(handle.isAlive());
        assertFalse(session.isRunning());
        assertEquals(completed.size(), 1);
        assertFalse(completed.get(0).isStopped());
    }

    @Test(timeOut = 3000)
    public void startShouldCleanNetworkRuntimeWhenRunCompletesNormally() throws Exception {
        AtomicBoolean running = new AtomicBoolean(false);
        TrackingNetworkRuntime networkRuntime = new TrackingNetworkRuntime();
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                running::get,
                () -> true,
                () -> 64,
                new PerformanceResultCollector(PerformanceResultSink.NOOP),
                PerformanceRunListener.NOOP,
                () -> false,
                networkRuntime
        );
        PerformanceRunSession session = new PerformanceRunSession(
                running::get,
                running::set,
                engine
        );

        PerformanceRunHandle handle = session.start(PerformanceRunRequest.builder()
                .plan(new PerformanceTestPlan(List.of()))
                .resultSink(PerformanceResultSink.NOOP)
                .build());
        handle.join(1000);

        assertFalse(handle.isAlive());
        assertEquals(networkRuntime.cancelAllCount, 1);
    }

    @Test
    public void beginRunShouldSkipScriptExecutorForPlansWithoutScripts() throws Exception {
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                () -> true,
                () -> true,
                () -> 64,
                new PerformanceResultCollector(PerformanceResultSink.NOOP)
        );

        engine.prepareRun(plan(false));
        engine.beginRun(System.currentTimeMillis());
        try {
            assertNull(runScriptExecutor(engine));
        } finally {
            engine.endRun();
        }
    }

    @Test
    public void beginRunShouldStartScriptExecutorForPlansWithScripts() throws Exception {
        PerformanceExecutionEngine engine = new PerformanceExecutionEngine(
                () -> true,
                () -> true,
                () -> 64,
                new PerformanceResultCollector(PerformanceResultSink.NOOP)
        );

        engine.prepareRun(plan(true));
        engine.beginRun(System.currentTimeMillis());
        try {
            assertNotNull(runScriptExecutor(engine));
        } finally {
            engine.endRun();
        }
    }

    private static Object runScriptExecutor(PerformanceExecutionEngine engine) throws Exception {
        Field field = PerformanceExecutionEngine.class.getDeclaredField("runScriptExecutor");
        field.setAccessible(true);
        return field.get(engine);
    }

    private static PerformanceTestPlan plan(boolean withScript) {
        ThreadGroupData threadGroupData = new ThreadGroupData();
        threadGroupData.numThreads = 1;
        PerformanceRequestSnapshot request = PerformanceRequestSnapshot.builder()
                .url("http://localhost")
                .prescript(withScript ? "pm.variables.set('a', '1')" : "")
                .build();
        return new PerformanceTestPlan(List.of(
                new PerformanceThreadGroupPlan(
                        "thread group",
                        threadGroupData,
                        List.of(new PerformanceCoreRequestSampler("request", request, null, List.of()))
                )
        ));
    }

    private static final class TrackingNetworkRuntime implements PerformanceNetworkRuntime {
        private int cancelAllCount;

        @Override
        public Set<EventSource> activeSseSources() {
            return Set.of();
        }

        @Override
        public Set<WebSocket> activeWebSockets() {
            return Set.of();
        }

        @Override
        public int activeHttpCallCount() {
            return 0;
        }

        @Override
        public int activeSseCount() {
            return 0;
        }

        @Override
        public int activeWebSocketCount() {
            return 0;
        }

        @Override
        public void cancelAll() {
            cancelAllCount++;
        }

        @Override
        public void onCallStarted(Call call) {
        }

        @Override
        public void onCallFinished(Call call) {
        }

        @Override
        public OkHttpClient getBaseClient(PreparedRequest request) {
            return new OkHttpClient();
        }
    }
}
