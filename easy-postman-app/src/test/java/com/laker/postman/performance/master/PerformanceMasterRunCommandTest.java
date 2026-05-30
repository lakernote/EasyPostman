package com.laker.postman.performance.master;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.worker.PerformanceWorkerOptions;
import com.laker.postman.performance.worker.PerformanceWorkerServer;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceMasterRunCommandTest {

    @Test
    public void shouldRunPlanOnWorkersAndWriteAggregatedReport() throws Exception {
        AtomicInteger workerCalls = new AtomicInteger();
        try (PerformanceWorkerServer workerA = workerServer("worker-a", workerCalls);
             PerformanceWorkerServer workerB = workerServer("worker-b", workerCalls)) {
            workerA.start();
            workerB.start();
            Path tempDir = Files.createTempDirectory("ep-master-run");
            Path planPath = tempDir.resolve("plan.json");
            Path outPath = tempDir.resolve("master-result.json");
            new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            int exitCode = new PerformanceMasterRunCommand().run(new String[]{
                            "performance", "master", "run",
                            "--plan", planPath.toString(),
                            "--workers", "127.0.0.1:" + workerA.getPort() + ",127.0.0.1:" + workerB.getPort(),
                            "--out", outPath.toString()
                    },
                    new PrintStream(stdout, true, StandardCharsets.UTF_8),
                    new PrintStream(stderr, true, StandardCharsets.UTF_8));

            assertEquals(exitCode, 0, stderr.toString(StandardCharsets.UTF_8));
            assertEquals(workerCalls.get(), 2);
            String resultJson = Files.readString(outPath);
            assertTrue(resultJson.contains("\"source\": \"master\""));
            assertTrue(resultJson.contains("\"totalRequests\": 4"));
            assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("workers=2"));
        }
    }

    @Test
    public void shouldStopSubmittedWorkersWhenLaterSubmitFails() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch stopSeen = new CountDownLatch(1);
        try (PerformanceWorkerServer workerA = new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> {
                    started.countDown();
                    while (control.isRunning()) {
                        Thread.sleep(10);
                    }
                    stopSeen.countDown();
                    return workerReport("worker-a", request, new AtomicInteger());
                }
        )) {
            workerA.start();
            Path tempDir = Files.createTempDirectory("ep-master-run-failed-submit");
            Path planPath = tempDir.resolve("plan.json");
            new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            int exitCode = new PerformanceMasterRunCommand().run(new String[]{
                            "performance", "master", "run",
                            "--plan", planPath.toString(),
                            "--workers", "127.0.0.1:" + workerA.getPort() + ",127.0.0.1:1"
                    },
                    new PrintStream(stdout, true, StandardCharsets.UTF_8),
                    new PrintStream(stderr, true, StandardCharsets.UTF_8));

            String stderrText = stderr.toString(StandardCharsets.UTF_8);
            assertEquals(exitCode, 1, stdout.toString(StandardCharsets.UTF_8));
            assertTrue(stderrText.contains("ConnectException"), stderrText);
            assertTrue(started.await(1, TimeUnit.SECONDS));
            assertTrue(stopSeen.await(1, TimeUnit.SECONDS), stderrText);
        }
    }

    @Test
    public void shouldApplyMasterTimeoutToWorkerHttpRequests() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-timeout");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        RecordingWorkerHttpClient workerClient = new RecordingWorkerHttpClient();

        PerformanceJsonReport report = new PerformanceMasterRunExecutor(
                new com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner(),
                workerClient
        ).execute(PerformanceMasterOptions.builder()
                .planPath(planPath)
                .workers(List.of(new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19090)))
                .timeoutMs(1_000L)
                .pollIntervalMs(50L)
                .build());

        assertEquals(report.getMetadata().getStatus(), "SUCCESS");
        assertTrue(!workerClient.timeouts.isEmpty());
        assertTrue(workerClient.timeouts.stream().allMatch(timeout ->
                !timeout.isNegative() && !timeout.isZero() && timeout.toMillis() <= 1_000L));
    }

    @Test
    public void shouldCarryWorkerErrorIntoAggregatedReport() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-error");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        RecordingWorkerHttpClient workerClient = new RecordingWorkerHttpClient();
        workerClient.failResult = true;

        PerformanceJsonReport report = new PerformanceMasterRunExecutor(
                new com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner(),
                workerClient
        ).execute(PerformanceMasterOptions.builder()
                .planPath(planPath)
                .workers(List.of(new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19090)))
                .timeoutMs(1_000L)
                .pollIntervalMs(50L)
                .build());

        assertEquals(report.getMetadata().getStatus(), "FAILED");
        assertTrue(report.getMetadata().getError().contains("boom"));
    }


    private static PerformanceWorkerServer workerServer(String workerId, AtomicInteger calls) {
        return new PerformanceWorkerServer(
                PerformanceWorkerOptions.builder().host("127.0.0.1").port(0).build(),
                (request, control) -> workerReport(workerId, request, calls)
        );
    }

    private static PerformanceJsonReport workerReport(String workerId,
                                                      PerformanceWorkerRunRequest request,
                                                      AtomicInteger calls) {
        calls.incrementAndGet();
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(request.getRunId())
                        .source(workerId)
                        .status("SUCCESS")
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(2L)
                        .successRequests(2L)
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }

    private static final class RecordingWorkerHttpClient extends PerformanceWorkerHttpClient {
        private final List<Duration> timeouts = new ArrayList<>();
        private PerformanceWorkerRunRequest request;
        private boolean failResult;

        @Override
        public void submitRun(com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                              PerformanceWorkerRunRequest request,
                              Duration timeout) {
            this.request = request;
            timeouts.add(timeout);
        }

        @Override
        public com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse status(
                com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                String runId,
                Duration timeout) {
            timeouts.add(timeout);
            return com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse.builder()
                    .runId(runId)
                    .workerId("worker-a")
                    .status("SUCCESS")
                    .build();
        }

        @Override
        public com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse result(
                com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                String runId,
                Duration timeout) {
            timeouts.add(timeout);
            if (failResult) {
                return com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse.builder()
                        .runId(runId)
                        .workerId("worker-a")
                        .status("FAILED")
                        .error("boom")
                        .build();
            }
            return com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse.builder()
                    .runId(runId)
                    .workerId("worker-a")
                    .status("SUCCESS")
                    .report(workerReport("worker-a", request, new AtomicInteger()))
                    .build();
        }
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
