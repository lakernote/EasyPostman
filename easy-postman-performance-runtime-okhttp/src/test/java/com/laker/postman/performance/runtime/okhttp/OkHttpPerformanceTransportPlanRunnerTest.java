package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.plan.PerformanceCoreRequestSampler;
import com.laker.postman.performance.core.plan.PerformanceTestPlan;
import com.laker.postman.performance.core.plan.PerformanceThreadGroupPlan;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;
import com.laker.postman.performance.core.runtime.PerformanceCoreRunSession;
import com.laker.postman.performance.core.runtime.PerformanceRunHandle;
import com.laker.postman.performance.core.runtime.PerformanceRunSummary;
import com.laker.postman.performance.core.runtime.PerformanceTransportRuntime;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class OkHttpPerformanceTransportPlanRunnerTest {

    @Test(timeOut = 3000)
    public void shouldRunCorePlanThroughOkHttpTransport() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            AtomicBoolean running = new AtomicBoolean(true);
            List<PerformanceSampleRecord> records = new CopyOnWriteArrayList<>();
            OkHttpPerformanceTransportPlanRunner engine = new OkHttpPerformanceTransportPlanRunner(
                    running::get,
                    new OkHttpPerformanceTransportRuntime()
            );
            PerformanceTestPlan plan = new PerformanceTestPlan(List.of(fixedGroup(
                    PerformanceRequestSnapshot.builder()
                            .id("api")
                            .name("API")
                            .protocol(PerformanceProtocol.HTTP)
                            .method("GET")
                            .url(server.url("/run").toString())
                            .build()
            )));

            engine.beginRun(System.currentTimeMillis(), new PerformanceCoreResultSink() {
                @Override
                public boolean acceptsSamples() {
                    return true;
                }

                @Override
                public void onSample(PerformanceSampleRecord record) {
                    records.add(record);
                }
            });
            engine.runTestPlan(plan, engine.getTotalThreads(plan));

            assertEquals(server.getRequestCount(), 1);
            assertEquals(records.size(), 1);
            assertEquals(records.get(0).getApiId(), "api");
            assertEquals(records.get(0).getResponseCode(), 200);
            assertTrue(records.get(0).isSuccessful());
            assertFalse(records.get(0).isExecutionFailed());
        }
    }

    @Test(timeOut = 3000)
    public void shouldRunThroughCoreRunSession() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            AtomicBoolean running = new AtomicBoolean(false);
            List<PerformanceSampleRecord> records = new CopyOnWriteArrayList<>();
            List<PerformanceRunSummary> summaries = new CopyOnWriteArrayList<>();
            OkHttpPerformanceTransportPlanRunner engine = new OkHttpPerformanceTransportPlanRunner(
                    running::get,
                    new OkHttpPerformanceTransportRuntime()
            );
            PerformanceCoreRunSession session = new PerformanceCoreRunSession(running::get, running::set, engine);
            PerformanceTestPlan plan = new PerformanceTestPlan(List.of(fixedGroup(
                    PerformanceRequestSnapshot.builder()
                            .id("api")
                            .name("API")
                            .protocol(PerformanceProtocol.HTTP)
                            .method("GET")
                            .url(server.url("/session").toString())
                            .build()
            )));

            PerformanceRunHandle handle = session.start(plan, new PerformanceCoreResultSink() {
                @Override
                public boolean acceptsSamples() {
                    return true;
                }

                @Override
                public void onSample(PerformanceSampleRecord record) {
                    records.add(record);
                }

                @Override
                public void onComplete(PerformanceRunSummary summary) {
                    summaries.add(summary);
                }
            });
            handle.join(1000);

            assertFalse(handle.isAlive());
            assertFalse(session.isRunning());
            assertEquals(records.size(), 1);
            assertEquals(summaries.size(), 1);
            assertFalse(summaries.get(0).isStopped());
        }
    }

    @Test
    public void shouldDelegateCancellationToTransportNetworkControl() {
        CancellableTransportRuntime runtime = new CancellableTransportRuntime();
        OkHttpPerformanceTransportPlanRunner engine = new OkHttpPerformanceTransportPlanRunner(
                () -> true,
                runtime
        );

        engine.cancelAllNetworkCalls();

        assertTrue(runtime.cancelled.get());
    }

    private static PerformanceThreadGroupPlan fixedGroup(PerformanceRequestSnapshot snapshot) {
        ThreadGroupData data = new ThreadGroupData();
        data.threadMode = ThreadGroupData.ThreadMode.FIXED;
        data.numThreads = 1;
        data.useTime = false;
        data.loops = 1;
        return new PerformanceThreadGroupPlan("group", data, List.of(
                new PerformanceCoreRequestSampler(snapshot.getName(), snapshot, null, List.of())
        ));
    }

    private static final class CancellableTransportRuntime implements PerformanceTransportRuntime {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public PerformanceSampleRecord execute(com.laker.postman.performance.core.request.PerformanceOutboundRequest request) {
            return PerformanceSampleRecord.builder().build();
        }

        @Override
        public void cancelAll() {
            cancelled.set(true);
        }
    }
}
