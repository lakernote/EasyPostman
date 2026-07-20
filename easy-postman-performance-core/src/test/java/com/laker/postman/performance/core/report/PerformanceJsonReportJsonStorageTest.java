package com.laker.postman.performance.core.report;

import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PerformanceJsonReportJsonStorageTest {

    @Test
    public void shouldAtomicallyReplaceExistingReportAndLoadIt() throws Exception {
        Path tempDir = Files.createTempDirectory("performance-report-storage");
        Path reportPath = tempDir.resolve("result.json");
        Files.writeString(reportPath, "stale-result", StandardCharsets.UTF_8);
        PerformanceJsonReport report = PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .source("local")
                        .status("RUNNING")
                        .planPath("plan.json")
                        .startTimeMs(10L)
                        .endTimeMs(20L)
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(3L)
                        .successRequests(2L)
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
        PerformanceJsonReportJsonStorage storage = new PerformanceJsonReportJsonStorage();

        storage.save(reportPath, report);

        PerformanceJsonReport loaded = storage.load(reportPath);
        assertNotNull(loaded);
        assertEquals(loaded.getMetadata().getStatus(), "RUNNING");
        assertEquals(loaded.getSummary().getTotalRequests(), 3L);
        assertEquals(loaded.getSummary().getFailedRequests(), 1L);
        assertTrue(Files.readString(reportPath).startsWith("{"));
        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
