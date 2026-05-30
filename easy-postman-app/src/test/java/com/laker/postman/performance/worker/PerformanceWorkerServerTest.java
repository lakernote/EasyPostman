package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerProtocolJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PerformanceWorkerServerTest {

    @Test
    public void shouldAcceptRunAndExposeResultOverHttp() throws Exception {
        AtomicReference<PerformanceWorkerRunRequest> captured = new AtomicReference<>();
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();
        try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> {
                    captured.set(request);
                    return PerformanceJsonReport.builder()
                            .metadata(PerformanceJsonReportMetadata.builder()
                                    .runId(request.getRunId())
                                    .source("worker")
                                    .status("SUCCESS")
                                    .build())
                            .summary(PerformanceJsonReportSummary.builder()
                                    .totalRequests(2L)
                                    .successRequests(2L)
                                    .build())
                            .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                            .build();
                }
        )) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            PerformanceWorkerRunRequest runRequest = PerformanceWorkerRunRequest.builder()
                    .runId("run-http")
                    .plan(emptyPlan())
                    .assignment(PerformanceWorkerAssignment.builder().runId("run-http").workerId("worker-a").build())
                    .build();

            HttpResponse<String> accepted = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(storage.toJson(runRequest)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(accepted.statusCode(), 202, accepted.body());
            assertNotNull(captured.get());
            assertEquals(captured.get().getRunId(), "run-http");

            HttpResponse<String> status = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs/run-http"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            PerformanceWorkerRunStatusResponse statusResponse = storage.statusResponseFromJson(status.body());

            assertEquals(status.statusCode(), 200, status.body());
            assertEquals(statusResponse.getStatus(), "SUCCESS");

            HttpResponse<String> result = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs/run-http/result"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            PerformanceWorkerRunResultResponse resultResponse = storage.resultResponseFromJson(result.body());

            assertEquals(result.statusCode(), 200, result.body());
            assertEquals(resultResponse.getReport().getSummary().getTotalRequests(), 2L);
        }
    }

    @Test
    public void shouldKeepStoppedRunNonTerminalUntilReportIsReady() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopSeen = new CountDownLatch(1);
        CountDownLatch releaseReport = new CountDownLatch(1);
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();
        try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> {
                    started.countDown();
                    while (control.isRunning()) {
                        Thread.sleep(10);
                    }
                    stopSeen.countDown();
                    assertTrue(releaseReport.await(2, TimeUnit.SECONDS));
                    return PerformanceJsonReport.builder()
                            .metadata(PerformanceJsonReportMetadata.builder()
                                    .runId(request.getRunId())
                                    .source("worker")
                                    .status("STOPPED")
                                    .stopped(true)
                                    .build())
                            .summary(PerformanceJsonReportSummary.builder()
                                    .totalRequests(1L)
                                    .successRequests(1L)
                                    .build())
                            .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                            .build();
                }
        )) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            PerformanceWorkerRunRequest runRequest = PerformanceWorkerRunRequest.builder()
                    .runId("run-stop")
                    .plan(emptyPlan())
                    .assignment(PerformanceWorkerAssignment.builder().runId("run-stop").workerId("worker-a").build())
                    .build();

            client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(storage.toJson(runRequest)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(started.await(1, TimeUnit.SECONDS));

            HttpResponse<String> stop = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs/run-stop/stop"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            PerformanceWorkerRunStatusResponse stopResponse = storage.statusResponseFromJson(stop.body());

            assertEquals(stop.statusCode(), 200, stop.body());
            assertEquals(stopResponse.getStatus(), "STOPPING");
            assertTrue(stopSeen.await(1, TimeUnit.SECONDS));

            HttpResponse<String> stoppingStatus = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs/run-stop"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(storage.statusResponseFromJson(stoppingStatus.body()).getStatus(), "STOPPING");

            releaseReport.countDown();
            PerformanceWorkerRunResultResponse resultResponse = awaitStoppedResult(client, storage, server.getPort());
            assertEquals(resultResponse.getStatus(), "STOPPED");
            assertNotNull(resultResponse.getReport());
            assertEquals(resultResponse.getReport().getSummary().getTotalRequests(), 1L);
        }
    }

    @Test
    public void shouldPruneCompletedRunsAfterRetentionWindow() throws Exception {
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();
        try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder()
                        .host("127.0.0.1")
                        .port(0)
                        .completedRunRetentionMs(100L)
                        .build(),
                (request, control) -> PerformanceJsonReport.builder()
                        .metadata(PerformanceJsonReportMetadata.builder()
                                .runId(request.getRunId())
                                .source("worker")
                                .status("SUCCESS")
                                .build())
                        .summary(PerformanceJsonReportSummary.builder()
                                .totalRequests(1L)
                                .successRequests(1L)
                                .build())
                        .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                        .build()
        )) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            submitRun(client, storage, server.getPort(), "run-old");
            awaitStatus(client, storage, server.getPort(), "run-old", "SUCCESS");
            Thread.sleep(150);

            submitRun(client, storage, server.getPort(), "run-new");

            HttpResponse<String> oldResult = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort()
                                    + "/api/performance/v1/runs/run-old/result"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(oldResult.statusCode(), 404, oldResult.body());
        }
    }

    @Test
    public void shouldRejectSecondRunWhileActive() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();
        try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> {
                    started.countDown();
                    assertTrue(release.await(2, TimeUnit.SECONDS));
                    return PerformanceJsonReport.builder()
                            .metadata(PerformanceJsonReportMetadata.builder()
                                    .runId(request.getRunId())
                                    .source("worker")
                                    .status("SUCCESS")
                                    .build())
                            .summary(PerformanceJsonReportSummary.builder()
                                    .totalRequests(1L)
                                    .successRequests(1L)
                                    .build())
                            .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                            .build();
                }
        )) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            submitRun(client, storage, server.getPort(), "run-active");
            assertTrue(started.await(1, TimeUnit.SECONDS));

            PerformanceWorkerRunRequest secondRun = PerformanceWorkerRunRequest.builder()
                    .runId("run-second")
                    .plan(emptyPlan())
                    .build();
            HttpResponse<String> second = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort() + "/api/performance/v1/runs"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(storage.toJson(secondRun)))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(second.statusCode(), 409, second.body());
            release.countDown();
            awaitStatus(client, storage, server.getPort(), "run-active", "SUCCESS");
        }
    }

    @Test
    public void shouldNotOverwriteFailedRunStatusWhenStopArrivesLate() throws Exception {
        PerformanceWorkerProtocolJsonStorage storage = new PerformanceWorkerProtocolJsonStorage();
        try (PerformanceWorkerServer server = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> {
                    throw new IllegalStateException("worker failed");
                }
        )) {
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            submitRun(client, storage, server.getPort(), "run-failed");
            awaitStatus(client, storage, server.getPort(), "run-failed", "FAILED");

            HttpResponse<String> stop = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + server.getPort()
                                    + "/api/performance/v1/runs/run-failed/stop"))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            PerformanceWorkerRunStatusResponse stopResponse = storage.statusResponseFromJson(stop.body());

            assertEquals(stop.statusCode(), 200, stop.body());
            assertEquals(stopResponse.getStatus(), "FAILED");
            assertEquals(stopResponse.getError(), "worker failed");
        }
    }

    private static PerformanceWorkerRunResultResponse awaitStoppedResult(HttpClient client,
                                                                         PerformanceWorkerProtocolJsonStorage storage,
                                                                         int port) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        PerformanceWorkerRunResultResponse response;
        do {
            HttpResponse<String> result = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + port + "/api/performance/v1/runs/run-stop/result"))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            response = storage.resultResponseFromJson(result.body());
            if ("STOPPED".equals(response.getStatus())) {
                return response;
            }
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        return response;
    }

    private static void submitRun(HttpClient client,
                                  PerformanceWorkerProtocolJsonStorage storage,
                                  int port,
                                  String runId) throws Exception {
        PerformanceWorkerRunRequest runRequest = PerformanceWorkerRunRequest.builder()
                .runId(runId)
                .plan(emptyPlan())
                .assignment(PerformanceWorkerAssignment.builder().runId(runId).workerId("worker-a").build())
                .build();
        HttpResponse<String> accepted = client.send(HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/api/performance/v1/runs"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(storage.toJson(runRequest)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(accepted.statusCode(), 202, accepted.body());
    }

    private static PerformanceWorkerRunStatusResponse awaitStatus(HttpClient client,
                                                                  PerformanceWorkerProtocolJsonStorage storage,
                                                                  int port,
                                                                  String runId,
                                                                  String expectedStatus) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        PerformanceWorkerRunStatusResponse response;
        do {
            HttpResponse<String> status = client.send(HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + port + "/api/performance/v1/runs/" + runId))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(status.statusCode(), 200, status.body());
            response = storage.statusResponseFromJson(status.body());
            if (expectedStatus.equals(response.getStatus())) {
                return response;
            }
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        return response;
    }

    private static PerformanceRunPlan emptyPlan() {
        return PerformanceRunPlan.builder()
                .testPlan(new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("run plan")
                        .type(NodeType.ROOT)
                        .build()))
                .build();
    }
}
