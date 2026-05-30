package com.laker.postman.performance.worker;

import org.testng.annotations.Test;

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
    }

    @Test
    public void shouldParseWorkerHostAndPort() {
        PerformanceWorkerOptions options = PerformanceWorkerCommandLine.parse(new String[]{
                "performance", "worker",
                "--host", "127.0.0.1",
                "--port", "19091"
        });

        assertEquals(options.getHost(), "127.0.0.1");
        assertEquals(options.getPort(), 19091);
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
    public void shouldRejectInvalidWorkerPortValues() {
        assertInvalidPort("--port requires a value", "performance", "worker", "--port");
        assertInvalidPort("--port must be a number", "performance", "worker", "--port", "abc");
        assertInvalidPort("--port must be between 1 and 65535", "performance", "worker", "--port", "0");
        assertInvalidPort("--port must be between 1 and 65535", "performance", "worker", "--port", "65536");
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
}
