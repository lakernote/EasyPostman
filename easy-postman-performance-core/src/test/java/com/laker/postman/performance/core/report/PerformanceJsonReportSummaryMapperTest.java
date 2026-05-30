package com.laker.postman.performance.core.report;

import com.laker.postman.performance.core.run.PerformanceRunStatus;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PerformanceJsonReportSummaryMapperTest {

    @Test
    public void shouldPromoteMergedReportToFailedWhenAnyWorkerHasRequestFailures() {
        PerformanceJsonReport report = PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .source("127.0.0.1:19091")
                        .status(PerformanceRunStatus.SUCCESS)
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(10L)
                        .successRequests(9L)
                        .failedRequests(1L)
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();

        PerformanceJsonReport merged = PerformanceJsonReportSummaryMapper.merge(
                "run-1",
                "master",
                PerformanceRunStatus.SUCCESS,
                "plan.json",
                List.of(report)
        );

        assertEquals(merged.getMetadata().getStatus(), PerformanceRunStatus.FAILED);
        assertEquals(merged.getSummary().getFailedRequests(), 1L);
        assertTrue(merged.getMetadata().getError().contains("127.0.0.1:19091: Request failures: 1"));
    }
}
