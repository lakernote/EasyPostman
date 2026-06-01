package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.sun.net.httpserver.HttpServer;
import org.jfree.data.time.RegularTimePeriod;
import org.testng.annotations.Test;

import com.laker.postman.panel.performance.result.PerformanceTrendView;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class PerformanceRemoteRunControlSupportTest {

    @Test
    public void shouldRequestWorkerTrendSnapshotWithoutFullReportWhenOnlyTrendIsEnabled() {
        PerformanceRemoteRunControlSupport support = support(true, false);

        assertFalse(support.shouldIncludeStatusReport());
        assertTrue(support.shouldIncludeStatusTrend());
    }

    @Test
    public void shouldRequestWorkerReportWhenRealtimeReportIsEnabled() {
        PerformanceRemoteRunControlSupport support = support(false, true);

        assertTrue(support.shouldIncludeStatusReport());
        assertFalse(support.shouldIncludeStatusTrend());
    }

    @Test
    public void shouldSkipWorkerReportWhenTrendAndRealtimeReportAreDisabled() {
        PerformanceRemoteRunControlSupport support = support(false, false);

        assertFalse(support.shouldIncludeStatusReport());
        assertFalse(support.shouldIncludeStatusTrend());
    }

    @Test
    public void shouldRequestRemoteTrendSnapshotOnlyAtConfiguredSamplingInterval() {
        PerformanceRemoteRunControlSupport support = support(true, false, 2_000L, new NoopTrendView());

        assertFalse(support.shouldRequestStatusTrendSnapshot(10_000L, false));
        assertFalse(support.shouldRequestStatusTrendSnapshot(11_000L, false));
        assertTrue(support.shouldRequestStatusTrendSnapshot(12_000L, false));
        assertFalse(support.shouldRequestStatusTrendSnapshot(13_000L, false));
        assertTrue(support.shouldRequestStatusTrendSnapshot(14_000L, false));
    }

    @Test
    public void shouldRejectLegacyRemoteWorkerWithoutProtocolVersionBeforeSubmit() throws Exception {
        try (TestServer server = TestServer.start("""
                {"status":"UP","workerId":"legacy-worker","host":"127.0.0.1","port":19090}
                """)) {
            PerformanceRemoteRunControlSupport support = support(false, false);
            Method submitRun = PerformanceRemoteRunControlSupport.class.getDeclaredMethod(
                    "submitRun",
                    PerformanceRunPlan.class,
                    List.class,
                    String.class
            );
            submitRun.setAccessible(true);

            try {
                submitRun.invoke(support, emptyPlan(), List.of(server.endpoint()), "run-legacy");
                fail("Expected legacy worker protocol rejection");
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                assertTrue(cause instanceof IllegalStateException, String.valueOf(cause));
                assertTrue(cause.getMessage().contains("127.0.0.1"), cause.getMessage());
                assertTrue(cause.getMessage().contains("protocol"), cause.getMessage());
            }

            assertEquals(server.requestCount().get(), 1, "旧 worker 应在 health 阶段被拦截，不能继续 POST /runs");
        }
    }

    private static PerformanceRemoteRunControlSupport support(boolean trendEnabled,
                                                             boolean reportRealtimeEnabled) {
        return support(trendEnabled, reportRealtimeEnabled, 1_000L, null);
    }

    private static PerformanceRemoteRunControlSupport support(boolean trendEnabled,
                                                             boolean reportRealtimeEnabled,
                                                             long samplingIntervalMs,
                                                             PerformanceTrendView trendView) {
        return new PerformanceRemoteRunControlSupport(
                () -> false,
                ignored -> {
                },
                null,
                null,
                null,
                trendView,
                () -> {
                },
                () -> {
                },
                () -> trendEnabled,
                () -> reportRealtimeEnabled,
                () -> samplingIntervalMs
        );
    }

    private static PerformanceRunPlan emptyPlan() {
        return PerformanceRunPlan.builder()
                .testPlan(new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("run plan")
                        .type(NodeType.ROOT)
                        .build()))
                .build();
    }

    private record TestServer(HttpServer server,
                              java.util.concurrent.atomic.AtomicInteger requestCount) implements AutoCloseable {

        static TestServer start(String body) throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            java.util.concurrent.atomic.AtomicInteger requestCount = new java.util.concurrent.atomic.AtomicInteger();
            server.createContext("/", exchange -> {
                requestCount.incrementAndGet();
                byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBody.length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(responseBody);
                }
            });
            server.start();
            return new TestServer(server, requestCount);
        }

        PerformanceWorkerEndpoint endpoint() {
            return new PerformanceWorkerEndpoint("127.0.0.1", server.getAddress().getPort());
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class NoopTrendView implements PerformanceTrendView {
        @Override
        public void clearTrendDataset() {
        }

        @Override
        public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
        }
    }
}
