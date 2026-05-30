package com.laker.postman.performance.master;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportApi;
import com.laker.postman.performance.core.report.PerformanceJsonReportProtocol;
import com.laker.postman.performance.core.report.PerformanceJsonReportSummary;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpoint;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunResultResponse;
import com.laker.postman.performance.core.worker.PerformanceWorkerRunStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class PerformanceWorkerReportCollector {
    private final PerformanceWorkerHttpClient workerClient;

    public PerformanceWorkerReportResult collect(PerformanceWorkerEndpoint endpoint,
                                                 String runId) throws IOException, InterruptedException {
        return collect(endpoint, runId, null);
    }

    public PerformanceWorkerReportResult collect(PerformanceWorkerEndpoint endpoint,
                                                 String runId,
                                                 Duration timeout) throws IOException, InterruptedException {
        PerformanceWorkerRunResultResponse result = timeout == null
                ? workerClient.result(endpoint, runId)
                : workerClient.result(endpoint, runId, timeout);
        PerformanceJsonReport report = result.getReport();
        String error = result.getError();

        if (!hasReportData(report)) {
            // worker 已到终态时，最终 result 理论上应包含 report；这里再查 status，避免控制面竞态导致 master 收到空最终报表。
            try {
                PerformanceWorkerRunStatusResponse status = timeout == null
                        ? workerClient.status(endpoint, runId, true)
                        : workerClient.status(endpoint, runId, true, timeout);
                if (hasReportData(status.getReport())) {
                    report = status.getReport();
                    log.debug("使用 worker status report 兜底最终结果: worker={}, runId={}",
                            endpointLabel(endpoint), runId);
                }
                if ((error == null || error.isBlank()) && status.getError() != null && !status.getError().isBlank()) {
                    error = status.getError();
                }
            } catch (IOException | InterruptedException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                log.warn("读取 worker status report 兜底失败: worker={}, runId={}",
                        endpointLabel(endpoint), runId, ex);
            }
        }

        return new PerformanceWorkerReportResult(result.getStatus(), report, error);
    }

    public static boolean hasAnyReportData(List<PerformanceJsonReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return false;
        }
        return reports.stream().anyMatch(PerformanceWorkerReportCollector::hasReportData);
    }

    public static boolean hasReportData(PerformanceJsonReport report) {
        if (report == null) {
            return false;
        }
        PerformanceJsonReportSummary summary = report.getSummary();
        if (summary != null && (summary.getTotalRequests() > 0
                || summary.getSuccessRequests() > 0
                || summary.getFailedRequests() > 0)) {
            return true;
        }
        if (report.getProtocols() == null) {
            return false;
        }
        return report.getProtocols().values().stream().anyMatch(PerformanceWorkerReportCollector::hasProtocolData);
    }

    private static boolean hasProtocolData(PerformanceJsonReportProtocol protocol) {
        if (protocol == null) {
            return false;
        }
        if (hasApiData(protocol.getTotal())) {
            return true;
        }
        return protocol.getApis() != null && protocol.getApis().stream().anyMatch(PerformanceWorkerReportCollector::hasApiData);
    }

    private static boolean hasApiData(PerformanceJsonReportApi api) {
        return api != null && (api.getTotal() > 0 || api.getSuccess() > 0 || api.getFailed() > 0);
    }

    private String endpointLabel(PerformanceWorkerEndpoint endpoint) {
        return endpoint == null ? "" : endpoint.getHost() + ":" + endpoint.getPort();
    }

    public record PerformanceWorkerReportResult(String status,
                                                PerformanceJsonReport report,
                                                String error) {
    }
}
