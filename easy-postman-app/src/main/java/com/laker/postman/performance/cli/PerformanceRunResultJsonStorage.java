package com.laker.postman.performance.cli;

import com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage;
import com.laker.postman.performance.runtime.PerformanceRunExecutionResult;
import com.laker.postman.util.JsonUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class PerformanceRunResultJsonStorage {
    private final PerformanceJsonReportJsonStorage reportStorage = new PerformanceJsonReportJsonStorage();

    public void save(Path path, PerformanceRunExecutionResult result) throws IOException {
        if (path == null) {
            return;
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(result), StandardCharsets.UTF_8);
    }

    public String toJson(PerformanceRunExecutionResult result) {
        return JsonUtil.toJsonPrettyStr(toMap(result));
    }

    private Map<String, Object> toMap(PerformanceRunExecutionResult result) {
        PerformanceRunExecutionResult safeResult = result == null
                ? PerformanceRunExecutionResult.builder().status(PerformanceRunExecutionResult.STATUS_FAILED).build()
                : result;
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("status", safeResult.getStatus());
        json.put("planPath", safeResult.getPlanPath());
        json.put("startTimeMs", safeResult.getStartTimeMs());
        json.put("endTimeMs", safeResult.getEndTimeMs());
        json.put("elapsedTimeMs", safeResult.getElapsedTimeMs());
        json.put("stopped", safeResult.isStopped());
        json.put("totalRequests", safeResult.getTotalRequests());
        json.put("successRequests", safeResult.getSuccessRequests());
        json.put("failedRequests", safeResult.getFailedRequests());
        json.put("error", safeResult.getError());
        json.put("report", safeResult.getReport() == null ? null : reportStorage.toMap(safeResult.getReport()));
        return json;
    }
}
