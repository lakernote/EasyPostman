package com.laker.postman.performance.worker;

import com.laker.postman.performance.core.worker.PerformanceWorkerAssignment;
import com.laker.postman.performance.core.worker.PerformanceWorkerThreadGroupAssignment;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class PerformanceWorkerCommandLineTest {

    @Test
    public void shouldParseWorkerOptionsWithDefaults() {
        PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(new String[]{
                "performance", "worker"
        });

        assertFalse(options.isHelp());
        assertEquals(options.getHost(), PerformanceWorkerOptions.DEFAULT_HOST);
        assertEquals(options.getPort(), PerformanceWorkerOptions.DEFAULT_PORT);
        assertEquals(options.getProgressIntervalMs(), PerformanceWorkerOptions.DEFAULT_PROGRESS_INTERVAL_MS);
    }

    @Test
    public void shouldParseWorkerHostPortAndProgressInterval() {
        PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(new String[]{
                "performance", "worker",
                "--host", "127.0.0.1",
                "--port", "19091",
                "--progress-interval", "2"
        });

        assertEquals(options.getHost(), "127.0.0.1");
        assertEquals(options.getPort(), 19091);
        assertEquals(options.getProgressIntervalMs(), 2000L);
    }

    @Test
    public void shouldDisableWorkerProgressOutput() {
        PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(new String[]{
                "performance", "worker",
                "--no-progress"
        });

        assertEquals(options.getProgressIntervalMs(), 0L);
    }

    @Test
    public void shouldFormatWorkerAssignmentSummaryForConsoleLogs() {
        PerformanceWorkerAssignment assignment = PerformanceWorkerAssignment.builder()
                .threadGroups(List.of(
                        new PerformanceWorkerThreadGroupAssignment("0", 0, 0, 50),
                        new PerformanceWorkerThreadGroupAssignment("1", 1, 50, 50)
                ))
                .build();

        assertEquals(PerformanceWorkerCommand.assignmentSummary(assignment),
                "[groupIndex=0,first=0,count=50;groupIndex=1,first=50,count=50]");
    }

    @Test
    public void shouldRejectUnusedPlanAndAssignmentOptions() {
        try {
            PerformanceWorkerCommandLine.parse(new String[]{
                    "performance", "worker",
                    "--plan", "/tmp/plan.json"
            });
            fail("Expected unknown option failure");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains("Unknown option: --plan"), ex.getMessage());
        }
    }

    @Test
    public void shouldRejectInvalidWorkerPortAndProgressIntervalValues() {
        assertInvalidPort("--port requires a value", "performance", "worker", "--port");
        assertInvalidPort("--port must be a number", "performance", "worker", "--port", "abc");
        assertInvalidPort("--port must be between 1 and 65535", "performance", "worker", "--port", "0");
        assertInvalidPort("--port must be between 1 and 65535", "performance", "worker", "--port", "65536");
        assertInvalidProgressInterval("--progress-interval requires a value",
                "performance", "worker", "--progress-interval");
        assertInvalidProgressInterval("--progress-interval must be a number",
                "performance", "worker", "--progress-interval", "abc");
        assertInvalidProgressInterval("--progress-interval must be >= 0",
                "performance", "worker", "--progress-interval", "-1");
    }

    @Test
    public void shouldExposeWorkerServerLifecycleWithoutNetworkProtocol() throws Exception {
        PerformanceWorkerServer server = new PerformanceWorkerServer(PerformanceWorkerOptions.builder()
                .host("127.0.0.1")
                .port(19092)
                .build());

        server.start();
        assertTrue(server.isRunning());

        server.stop();
        assertFalse(server.isRunning());
    }

    private static void assertInvalidPort(String expectedMessage, String... args) {
        try {
            PerformanceWorkerCommandLine.parse(args);
            fail("Expected invalid port failure");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(expectedMessage), ex.getMessage());
        }
    }

    private static void assertInvalidProgressInterval(String expectedMessage, String... args) {
        try {
            PerformanceWorkerCommandLine.parse(args);
            fail("Expected invalid progress interval failure");
        } catch (IllegalArgumentException ex) {
            assertTrue(ex.getMessage().contains(expectedMessage), ex.getMessage());
        }
    }
}
