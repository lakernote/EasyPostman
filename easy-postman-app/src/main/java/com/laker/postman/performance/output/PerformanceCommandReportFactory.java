package com.laker.postman.performance.output;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportMetadata;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummaryMapper;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceCommandReportFactory {

    public PerformanceJsonReport snapshot(String runId,
                                          String source,
                                          String status,
                                          String planPath,
                                          long startTimeMs,
                                          long endTimeMs,
                                          String error,
                                          long totalRequests,
                                          long successRequests,
                                          long failedRequests) {
        return PerformanceJsonReport.builder()
                .metadata(PerformanceJsonReportMetadata.builder()
                        .runId(runId)
                        .source(source)
                        .status(status)
                        .planPath(planPath)
                        .startTimeMs(startTimeMs)
                        .endTimeMs(endTimeMs)
                        .elapsedTimeMs(Math.max(0L, endTimeMs - startTimeMs))
                        .error(error)
                        .build())
                .summary(PerformanceJsonReportSummary.builder()
                        .totalRequests(totalRequests)
                        .successRequests(successRequests)
                        .failedRequests(failedRequests)
                        .build())
                .protocols(PerformanceJsonReportSummaryMapper.emptyProtocols())
                .build();
    }
}
