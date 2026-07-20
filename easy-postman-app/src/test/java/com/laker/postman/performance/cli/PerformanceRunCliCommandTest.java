package com.laker.postman.performance.cli;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import com.laker.postman.performance.core.run.PerformanceRunEnvironment;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.performance.core.run.PerformanceRunVariable;
import com.laker.postman.performance.core.run.PerformanceRunVariableSet;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.performance.runtime.PerformanceRunPlanExecutor;
import com.laker.postman.performance.runtime.PerformanceRunReportListener;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class PerformanceRunCliCommandTest {

    @Test
    public void shouldParseRunPlanAndOutputPath() {
        PerformanceRunCliOptions options = PerformanceRunCliOptions.parse(new String[]{
                "performance", "run", "--plan", "/tmp/plan.json", "--out", "/tmp/result.json"
        });

        assertFalse(options.isHelp());
        assertEquals(options.getPlanPath(), Path.of("/tmp/plan.json"));
        assertEquals(options.getOutPath(), Path.of("/tmp/result.json"));
    }

    @Test
    public void shouldExecuteEmptyPlanHeadlesslyAndWriteResultJson() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-headless-run");
        Path planPath = tempDir.resolve("plan.json");
        Path outPath = tempDir.resolve("result.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyRunPlan());
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PerformanceRunCliCommand command = new PerformanceRunCliCommand(() -> {
        });

        int exitCode = command.run(new String[]{
                "performance", "run", "--plan", planPath.toString(), "--out", outPath.toString()
        }, new PrintStream(stdout, true, StandardCharsets.UTF_8), new PrintStream(stderr, true, StandardCharsets.UTF_8));

        assertEquals(exitCode, 0, stderr.toString(StandardCharsets.UTF_8));
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("total=0"));
        assertTrue(stdout.toString(StandardCharsets.UTF_8).contains("Performance run progress"));
        String resultJson = Files.readString(outPath);
        assertTrue(resultJson.contains("\"schemaVersion\": \"1.1\""));
        assertTrue(resultJson.contains("\"status\": \"SUCCESS\""));
        assertTrue(resultJson.contains("\"totalRequests\": 0"));
        assertFalse(resultJson.contains("\"report\":"));
    }

    @Test
    public void shouldReplaceStaleOutputWithFailedReportWhenPlanDoesNotExist() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-headless-run-missing");
        Path outPath = tempDir.resolve("result.json");
        Files.writeString(outPath, "stale-success", StandardCharsets.UTF_8);
        Path missingPlan = tempDir.resolve("missing-plan.json");
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = new PerformanceRunCliCommand(() -> {
            throw new AssertionError("runtime bootstrap must not run for a missing plan");
        }).run(new String[]{
                        "performance", "run",
                        "--plan", missingPlan.toString(),
                        "--out", outPath.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        PerformanceJsonReport report = new PerformanceJsonReportJsonStorage().load(outPath);
        assertEquals(report.getMetadata().getStatus(), PerformanceRunStatus.FAILED);
        assertTrue(report.getMetadata().getError().contains("does not exist"));
        assertFalse(Files.readString(outPath).contains("stale-success"));
    }

    @Test
    public void shouldRejectOutputThatPointsToPlanWithoutModifyingPlan() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-headless-run-conflicting-output");
        Path planPath = tempDir.resolve("plan.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyRunPlan());
        String originalPlan = Files.readString(planPath);
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        int exitCode = new PerformanceRunCliCommand(() -> {
            throw new AssertionError("runtime bootstrap must not run for conflicting paths");
        }).run(new String[]{
                        "performance", "run",
                        "--plan", planPath.toString(),
                        "--out", planPath.toString()
                },
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("--out must not point to the plan file"));
        assertEquals(Files.readString(planPath), originalPlan);
    }

    @Test
    public void shouldUpdateOutputWhileRunIsStillInProgress() throws Exception {
        Path tempDir = Files.createTempDirectory("ep-headless-run-live");
        Path planPath = tempDir.resolve("plan.json");
        Path outPath = tempDir.resolve("result.json");
        new PerformanceRunPlanJsonStorage().save(planPath, emptyRunPlan());
        CountDownLatch liveReportWritten = new CountDownLatch(1);
        CountDownLatch allowCompletion = new CountDownLatch(1);
        PerformanceRunPlanExecutor executor = new PerformanceRunPlanExecutor() {
            @Override
            public PerformanceRunExecutionResult execute(Path ignoredPlanPath,
                                                         PrintStream scriptOutput,
                                                         PerformanceRunReportListener listener) throws Exception {
                listener.onReport(report(PerformanceRunStatus.RUNNING, 2L, 1L));
                liveReportWritten.countDown();
                assertTrue(allowCompletion.await(5, TimeUnit.SECONDS));
                PerformanceJsonReport finalReport = report(PerformanceRunStatus.SUCCESS, 2L, 2L);
                return PerformanceRunExecutionResult.builder()
                        .status(PerformanceRunStatus.SUCCESS)
                        .totalRequests(2L)
                        .successRequests(2L)
                        .report(finalReport)
                        .build();
            }
        };
        PerformanceRunCliCommand command = new PerformanceRunCliCommand(() -> {
        }, executor);
        ExecutorService commandExecutor = Executors.newSingleThreadExecutor();
        try {
            Future<Integer> exitCode = commandExecutor.submit(() -> command.run(new String[]{
                            "performance", "run",
                            "--plan", planPath.toString(),
                            "--out", outPath.toString()
                    },
                    new PrintStream(new ByteArrayOutputStream()),
                    new PrintStream(new ByteArrayOutputStream())));

            assertTrue(liveReportWritten.await(5, TimeUnit.SECONDS));
            PerformanceJsonReport running = new PerformanceJsonReportJsonStorage().load(outPath);
            assertEquals(running.getMetadata().getStatus(), PerformanceRunStatus.RUNNING);
            assertEquals(running.getSummary().getTotalRequests(), 2L);

            allowCompletion.countDown();
            assertEquals(exitCode.get(5, TimeUnit.SECONDS).intValue(), 0);
            PerformanceJsonReport completed = new PerformanceJsonReportJsonStorage().load(outPath);
            assertEquals(completed.getMetadata().getStatus(), PerformanceRunStatus.SUCCESS);
            assertEquals(completed.getSummary().getSuccessRequests(), 2L);
        } finally {
            allowCompletion.countDown();
            commandExecutor.shutdownNow();
        }
    }

    @Test
    public void shouldReturnUsageErrorWhenPlanIsMissing() {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PerformanceRunCliCommand command = new PerformanceRunCliCommand(() -> {
        });

        int exitCode = command.run(new String[]{"performance", "run"},
                new PrintStream(new ByteArrayOutputStream()),
                new PrintStream(stderr));

        assertEquals(exitCode, 2);
        assertTrue(stderr.toString().contains("--plan is required"));
    }

    private static PerformanceRunPlan emptyRunPlan() {
        return PerformanceRunPlan.builder()
                .generatedBy("EasyPostman Test")
                .environment(new PerformanceRunEnvironment(
                        "env-cli",
                        "CLI",
                        List.of(new PerformanceRunVariable(true, "baseUrl", "http://127.0.0.1"))
                ))
                .globals(new PerformanceRunVariableSet(
                        List.of(new PerformanceRunVariable(true, "token", "abc123"))
                ))
                .testPlan(new PerformanceCorePlanDocument(PerformanceCorePlanNode.builder()
                        .name("run plan")
                        .type(NodeType.ROOT)
                        .build()))
                .build();
    }

    private static PerformanceJsonReport report(String status, long total, long success) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .source("local")
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
