package com.laker.postman.panel.performance.runtime;

import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.runtime.PerformanceRunListener;
import com.laker.postman.performance.core.runtime.PerformanceRunSummary;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.execution.PerformanceNetworkRuntime;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.panel.performance.result.PerformanceResultCollector;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

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
