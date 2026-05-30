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

    @Test
    public void shouldRecalculateDistributedQpsFromGlobalSampleWindow() {
        PerformanceJsonReportApi leftApi = httpApi("search", "Search", 60, 1_000L, 4_000L,
                20.0, 6_000L, 12_000L);
        PerformanceJsonReportApi rightApi = httpApi("search", "Search", 40, 2_000L, 6_000L,
                10.0, 4_000L, 8_000L);
        PerformanceJsonReport left = report("worker-a", leftApi);
        PerformanceJsonReport right = report("worker-b", rightApi);

        PerformanceJsonReport merged = PerformanceJsonReportSummaryMapper.merge(
                "run-1",
                "master",
                PerformanceRunStatus.SUCCESS,
                "plan.json",
                List.of(left, right)
        );

        PerformanceJsonReportApi mergedApi = merged.getProtocols().get("HTTP").getApis().get(0);
        PerformanceJsonReportApi mergedTotal = merged.getProtocols().get("HTTP").getTotal();

        assertEquals(mergedApi.getTotal(), 100L);
        assertEquals(mergedApi.getFirstSampleStartTimeMs(), 1_000L);
        assertEquals(mergedApi.getLastSampleEndTimeMs(), 6_000L);
        assertEquals(mergedApi.getSamplesPerSecond(), 20.0);
        assertEquals(mergedApi.getBytes().getSentBytes(), 10_000L);
        assertEquals(mergedApi.getBytes().getReceivedBytes(), 20_000L);
        assertEquals(mergedApi.getBytes().getSentBytesPerSecond(), 2_000.0);
        assertEquals(mergedApi.getBytes().getReceivedBytesPerSecond(), 4_000.0);
        assertEquals(mergedApi.getBytes().getAvgReceivedBytes(), 200L);
        assertEquals(mergedTotal.getSamplesPerSecond(), 20.0);
    }

    private static PerformanceJsonReport report(String source, PerformanceJsonReportApi api) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .source(source)
                        .status(PerformanceRunStatus.SUCCESS)
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(api.getTotal())
                        .successRequests(api.getSuccess())
                        .build())
                .protocols(java.util.Map.of("HTTP", PerformanceJsonReportProtocol.builder()
                        .protocol("HTTP")
                        .total(api)
                        .apis(List.of(api))
                        .build()))
                .build();
    }

    private static PerformanceJsonReportApi httpApi(String apiId,
                                                    String name,
                                                    long total,
                                                    long firstSampleStartTimeMs,
                                                    long lastSampleEndTimeMs,
                                                    double samplesPerSecond,
                                                    long sentBytes,
                                                    long receivedBytes) {
        return PerformanceJsonReportApi.builder()
                .apiId(apiId)
                .name(name)
                .protocol("HTTP")
                .total(total)
                .success(total)
                .samplesPerSecond(samplesPerSecond)
                .firstSampleStartTimeMs(firstSampleStartTimeMs)
                .lastSampleEndTimeMs(lastSampleEndTimeMs)
                .bytes(PerformanceJsonReportBytes.builder()
                        .sentBytes(sentBytes)
                        .receivedBytes(receivedBytes)
                        .build())
                .durationMs(PerformanceJsonReportDuration.builder().avg(10L).min(10L).max(10L).build())
                .build();
    }
}
