package com.laker.postman.performance.cli;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.plan.PerformanceCorePlanDocument;
import com.laker.postman.performance.core.plan.PerformanceCorePlanNode;
import com.laker.postman.performance.core.run.PerformanceRunEnvironment;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunVariable;
import com.laker.postman.performance.core.run.PerformanceRunVariableSet;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        String resultJson = Files.readString(outPath);
        assertTrue(resultJson.contains("\"status\": \"SUCCESS\""));
        assertTrue(resultJson.contains("\"totalRequests\": 0"));
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
}
