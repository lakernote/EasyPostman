package com.laker.postman.performance.core.worker;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

public class PerformanceWorkerEndpointParserTest {

    @Test
    public void shouldParseCommaAndWhitespaceSeparatedEndpoints() {
        List<PerformanceWorkerEndpoint> endpoints = PerformanceWorkerEndpointParser.parse(
                "  127.0.0.1:19090, worker-a:19091\nworker-b:19092\tworker-c:19093,,  "
        );

        assertEquals(endpoints, List.of(
                new PerformanceWorkerEndpoint("127.0.0.1", 19090),
                new PerformanceWorkerEndpoint("worker-a", 19091),
                new PerformanceWorkerEndpoint("worker-b", 19092),
                new PerformanceWorkerEndpoint("worker-c", 19093)
        ));
    }

    @Test
    public void shouldIgnoreBlankInputAndEmptyFragments() {
        assertEquals(PerformanceWorkerEndpointParser.parse(null), List.of());
        assertEquals(PerformanceWorkerEndpointParser.parse("  , \n\t , "), List.of());
    }

    @Test
    public void shouldRejectInvalidEndpointAndIncludeOriginalFragment() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                () -> PerformanceWorkerEndpointParser.parse("worker-a:19090, bad-fragment"));

        assertTrue(exception.getMessage().contains("bad-fragment"));
    }

    @Test
    public void shouldRejectNonNumericPortAndIncludeOriginalFragment() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                () -> PerformanceWorkerEndpointParser.parse("worker-a:not-a-port"));

        assertTrue(exception.getMessage().contains("worker-a:not-a-port"));
    }

    @Test
    public void shouldRejectPortOutsideValidRangeAndIncludeOriginalFragment() {
        IllegalArgumentException exception = expectThrows(IllegalArgumentException.class,
                () -> PerformanceWorkerEndpointParser.parse("worker-a:65536"));

        assertTrue(exception.getMessage().contains("worker-a:65536"));
    }

    @Test
    public void shouldFormatEndpointsAsCommaSeparatedHostPorts() {
        String text = PerformanceWorkerEndpointParser.formatList(List.of(
                new PerformanceWorkerEndpoint("127.0.0.1", 19090),
                new PerformanceWorkerEndpoint("worker-a", 19091)
        ));

        assertEquals(text, "127.0.0.1:19090,worker-a:19091");
    }
}
