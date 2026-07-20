package com.laker.postman.performance.master;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportDuration;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.worker.PerformanceWorkerHealthResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerProtocol;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunRequest;
import com.laker.postman.performance.worker.PerformanceWorkerOptions;
import com.laker.postman.performance.worker.PerformanceWorkerServer;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

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
            assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("Performance master progress"));
        }
    }

    @Test
    public void shouldReplaceStaleOutputWithFailedReportWhenPlanDoesNotExist() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-missing");
        Path outPath = tempDir.resolve("result.json");
        Path missingPlanPath = tempDir.resolve("missing-plan.json");
        Files.writeString(outPath, "stale-success", StandardCharsets.UTF_8);

        int exitCode = new PerformanceMasterRunCommand().run(new String[]{
                        "performance", "master", "run",
                        "--plan", missingPlanPath.toString(),
                        "--workers", "127.0.0.1:19090",
                        "--out", outPath.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(new ByteArrayOutputStream()));

        assertEquals(exitCode, 2);
        PerformanceJsonReport report = new com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage()
                .load(outPath);
        assertEquals(report.getMetadata().getStatus(), "FAILED");
        assertTrue(report.getMetadata().getError().contains("does not exist"));
        assertTrue(!Files.readString(outPath).contains("stale-success"));
    }

    @Test
    public void shouldRejectOutputThatPointsToPlanWithoutModifyingPlan() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-conflicting-output");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        String originalPlan = Files.readString(planPath);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PerformanceMasterRunExecutor executor = new PerformanceMasterRunExecutor() {
            @Override
            public PerformanceJsonReport execute(PerformanceMasterOptions options,
                                                 PerformanceMasterRunListener listener) {
                throw new AssertionError("master executor must not run for conflicting paths");
            }
        };

        int exitCode = new PerformanceMasterRunCommand(executor).run(new String[]{
                        "performance", "master", "run",
                        "--plan", planPath.toString(),
                        "--workers", "127.0.0.1:19090",
                        "--out", planPath.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("--out must not point to the plan file"));
        assertEquals(Files.readString(planPath), originalPlan);
    }

    @Test
    public void shouldUpdateMasterOutputWhileWorkersAreRunning() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-live");
        Path planPath = tempDir.resolve("plan.json");
        Path outPath = tempDir.resolve("result.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        CountDownLatch liveReportWritten = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        PerformanceJsonReport runningReport = report("RUNNING", 3L, 2L);
        PerformanceJsonReport finalReport = report("SUCCESS", 3L, 3L);
        PerformanceMasterRunExecutor executor = new PerformanceMasterRunExecutor() {
            @Override
            public PerformanceJsonReport execute(PerformanceMasterOptions options,
                                                 PerformanceMasterRunListener listener) throws Exception {
                listener.onProgress(new PerformanceMasterRunProgress(
                        runningReport,
                        2,
                        4,
                        0,
                        1,
                        12.5
                ));
                liveReportWritten.countDown();
                assertTrue(allowCompletion.await(5, TimeUnit.SECONDS));
                return finalReport;
            }
        };
        PerformanceMasterRunCommand command = new PerformanceMasterRunCommand(executor);
        ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> exitCode = commandExecutor.submit(() -> command.run(new String[]{
                            "performance", "master", "run",
                            "--plan", planPath.toString(),
                            "--workers", "127.0.0.1:19090",
                            "--out", outPath.toString()
                    },
                    new PrintStream(new ByteArrayOutputStream()),
                    new PrintStream(new ByteArrayOutputStream())));

            assertTrue(liveReportWritten.await(5, TimeUnit.SECONDS));
            PerformanceJsonReport running = new com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage()
                    .load(outPath);
            assertEquals(running.getMetadata().getStatus(), "RUNNING");
            assertEquals(running.getSummary().getTotalRequests(), 3L);

            allowCompletion.countDown();
            assertEquals(exitCode.get(5, TimeUnit.SECONDS).intValue(), 0);
            PerformanceJsonReport completed = new com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage()
                    .load(outPath);
            assertEquals(completed.getMetadata().getStatus(), "SUCCESS");
            assertEquals(completed.getSummary().getSuccessRequests(), 3L);
        } finally {
            allowCompletion.countDown();
            commandExecutor.shutdownNow();
        }
    }

    @Test
    public void shouldStopSubmittedWorkersWhenLaterSubmitFails() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-failed-submit");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        RecordingWorkerHttpClient workerClient = new RecordingWorkerHttpClient();
        workerClient.failSubmitAt = 2;

        try {
            new PerformanceMasterRunExecutor(
                    new com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner(),
                    workerClient
            ).execute(PerformanceMasterOptions.builder()
                    .planPath(planPath)
                    .workers(List.of(
                            new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19090),
                            new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19091)
                    ))
                    .timeoutMs(1_000L)
                    .pollIntervalMs(50L)
                    .build());
            fail("Expected submit failure");
        } catch (IOException ex) {
            assertTrue(ex.getMessage().contains("submit boom"), ex.getMessage());
        }

        assertEquals(workerClient.healthRequests.get(), 2);
        assertEquals(workerClient.submitRequests.get(), 2);
        assertEquals(workerClient.stopRequests.get(), 1);
        assertEquals(workerClient.stoppedEndpoints.get(0).getPort(), 19090);
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

    @Test
    public void shouldFallbackToStatusReportWhenResultReportIsMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-status-report");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        RecordingWorkerHttpClient workerClient = new RecordingWorkerHttpClient();
        workerClient.omitResultReport = true;

        PerformanceJsonReport report = new PerformanceMasterRunExecutor(
                new com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner(),
                workerClient
        ).execute(PerformanceMasterOptions.builder()
                .planPath(planPath)
                .workers(List.of(new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19090)))
                .timeoutMs(1_000L)
                .pollIntervalMs(50L)
                .build());

        assertEquals(report.getSummary().getTotalRequests(), 2L);
        assertTrue(report.getProtocols().get(PerformanceProtocol.HTTP.name()).getTotal().getTotal() > 0);
        assertTrue(workerClient.statusReportRequests.get() > 0);
    }

    @Test
    public void shouldRejectIncompatibleWorkerProtocolBeforeSubmit() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-master-run-protocol");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyPlan());
        RecordingWorkerHttpClient workerClient = new RecordingWorkerHttpClient();
        workerClient.workerProtocolVersion = "legacy";

        try {
            new PerformanceMasterRunExecutor(
                    new com.laker.postman.performance.core.worker.PerformanceWorkerAssignmentPlanner(),
                    workerClient
            ).execute(PerformanceMasterOptions.builder()
                    .planPath(planPath)
                    .workers(List.of(new com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint("127.0.0.1", 19090)))
                    .timeoutMs(1_000L)
                    .pollIntervalMs(50L)
                    .build());
            fail("Expected incompatible worker protocol rejection");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage().contains("127.0.0.1:19090"), ex.getMessage());
            assertTrue(ex.getMessage().contains(PerformanceWorkerProtocol.CURRENT_VERSION), ex.getMessage());
            assertTrue(ex.getMessage().contains("legacy"), ex.getMessage());
        }

        assertEquals(workerClient.healthRequests.get(), 1);
        assertEquals(workerClient.submitRequests.get(), 0);
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
        PerformanceJsonReportApi api = PerformanceJsonReportApi.builder()
                .apiId("http")
                .name("普通 HTTP")
                .protocol(PerformanceProtocol.HTTP.name())
                .total(2L)
                .success(2L)
                .samplesPerSecond(2.0)
                .durationMs(PerformanceJsonReportDuration.builder()
                        .avg(10L)
                        .min(8L)
                        .max(12L)
                        .p90(11L)
                        .p95(12L)
                        .p99(12L)
                        .build())
                .build();
        Map<String, PerformanceJsonReportProtocol> protocols = PerformanceJsonReportSummaryMapper.emptyProtocols();
        protocols.put(PerformanceProtocol.HTTP.name(), PerformanceJsonReportProtocol.builder()
                .protocol(PerformanceProtocol.HTTP.name())
                .total(api)
                .apis(List.of(api))
                .build());
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
                .protocols(protocols)
                .build();
    }

    private static final class RecordingWorkerHttpClient extends PerformanceWorkerHttpClient {
        private final List<Duration> timeouts = new ArrayList<>();
        private PerformanceWorkerRunRequest request;
        private boolean failResult;
        private boolean omitResultReport;
        private String workerProtocolVersion = PerformanceWorkerProtocol.CURRENT_VERSION;
        private int failSubmitAt = -1;
        private final List<com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint> stoppedEndpoints = new ArrayList<>();
        private final AtomicInteger healthRequests = new AtomicInteger();
        private final AtomicInteger submitRequests = new AtomicInteger();
        private final AtomicInteger stopRequests = new AtomicInteger();
        private final AtomicInteger statusReportRequests = new AtomicInteger();

        @Override
        public PerformanceWorkerHealthResponse health(com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                                                       Duration timeout) {
            healthRequests.incrementAndGet();
            timeouts.add(timeout);
            return PerformanceWorkerHealthResponse.builder()
                    .status("UP")
                    .workerId("worker-a")
                    .host(endpoint.getHost())
                    .port(endpoint.getPort())
                    .workerProtocolVersion(workerProtocolVersion)
                    .build();
        }

        @Override
        public void submitRun(com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                              PerformanceWorkerRunRequest request,
                              Duration timeout) throws IOException {
            submitRequests.incrementAndGet();
            if (failSubmitAt > 0 && submitRequests.get() == failSubmitAt) {
                throw new IOException("submit boom");
            }
            this.request = request;
            timeouts.add(timeout);
        }

        @Override
        public void stop(com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                         String runId,
                         Duration timeout) {
            stopRequests.incrementAndGet();
            stoppedEndpoints.add(endpoint);
            timeouts.add(timeout);
        }

        @Override
        public com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse status(
                com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                String runId,
                Duration timeout) {
            return status(endpoint, runId, true, timeout);
        }

        @Override
        public com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse status(
                com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint endpoint,
                String runId,
                boolean includeReport,
                Duration timeout) {
            timeouts.add(timeout);
            PerformanceJsonReport report = null;
            if (includeReport && omitResultReport) {
                statusReportRequests.incrementAndGet();
                report = workerReport("worker-a", request, new AtomicInteger());
            }
            return com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse.builder()
                    .runId(runId)
                    .workerId("worker-a")
                    .status("SUCCESS")
                    .report(report)
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
            if (omitResultReport) {
                return com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse.builder()
                        .runId(runId)
                        .workerId("worker-a")
                        .status("SUCCESS")
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

    private static PerformanceJsonReport report(String status, long total, long success) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .source("master")
                        .status(status)
                        .planPath("plan.json")
                        .startTimeMs(10L)
                        .endTimeMs(20L)
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }
}
