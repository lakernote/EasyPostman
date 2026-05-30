package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportDuration;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

public class PerformanceRemoteTrendWindowSamplerTest {

    @Test
    public void shouldCalculateQpsFromRemoteRequestDeltas() {
        PerformanceRemoteTrendWindowSampler sampler = new PerformanceRemoteTrendWindowSampler();
        sampler.reset(1_000L);

        PerformanceTrendSnapshot first = sampler.sample(
                70,
                100,
                5,
                report(100, 95, 5, 4),
                2_000L
        );
        PerformanceTrendSnapshot second = sampler.sample(
                70,
                150,
                8,
                report(150, 142, 8, 5),
                3_000L
        );

        assertEquals(first.activeUsers(), 70);
        assertEquals(first.http().samples(), 100);
        assertEquals(first.http().sampleRate(), 100.0);
        assertEquals(second.http().samples(), 50);
        assertEquals(second.http().failures(), 3);
        assertEquals(second.http().sampleRate(), 50.0);
        assertEquals(second.http().avgDurationMs(), 5.0);
    }

    private static PerformanceJsonReport report(long total, long success, long failed, long avgDuration) {
        PerformanceJsonReportApi httpTotal = PerformanceJsonReportApi.builder()
                .name("HTTP Total")
                .protocol("HTTP")
                .total(total)
                .success(success)
                .failed(failed)
                .durationMs(PerformanceJsonReportDuration.builder().avg(avgDuration).build())
                .build();
        return PerformanceJsonReport.builder()
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(total)
                        .successRequests(success)
                        .failedRequests(failed)
                        .build())
                .protocols(Map.of("HTTP", PerformanceJsonReportProtocol.builder()
                        .protocol("HTTP")
                        .total(httpTotal)
                        .build()))
                .build();
    }
}
