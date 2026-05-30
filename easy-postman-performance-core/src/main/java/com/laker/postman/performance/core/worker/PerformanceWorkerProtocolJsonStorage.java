package com.laker.postman.performance.core.worker;

import com.laker.postman.performance.core.report.PerformanceJsonReport;
import com.laker.postman.performance.core.report.PerformanceJsonReportJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunPlan;
import com.laker.postman.performance.core.run.PerformanceRunPlanJsonStorage;
import com.laker.postman.performance.core.run.PerformanceRunStatus;
import com.laker.postman.util.JsonUtil;

import java.util.LinkedHashMap;
import java.util.Map;

public class PerformanceWorkerProtocolJsonStorage {
    private final PerformanceRunPlanJsonStorage planStorage = new PerformanceRunPlanJsonStorage();
    private final PerformanceWorkerAssignmentJsonStorage assignmentStorage = new PerformanceWorkerAssignmentJsonStorage();
    private final PerformanceJsonReportJsonStorage reportStorage = new PerformanceJsonReportJsonStorage();

    public String toJson(Object value) {
        return JsonUtil.toJsonPrettyStr(toMap(value));
    }

    public PerformanceWorkerRunRequest runRequestFromJson(String json) {
        Map<String, Object> root = root(json);
        PerformanceRunPlan plan = root.get("plan") == null
                ? null
                : planStorage.fromJson(JsonUtil.toJsonStr(root.get("plan")));
        PerformanceWorkerAssignment assignment = root.get("assignment") == null
                ? null
                : assignmentStorage.fromJson(JsonUtil.toJsonStr(root.get("assignment")));
        return PerformanceWorkerRunRequest.builder()
                .runId(stringValue(root, "runId", ""))
                .plan(plan)
                .assignment(assignment)
                .build();
    }

    public PerformanceWorkerRunStatusResponse statusResponseFromJson(String json) {
        Map<String, Object> root = root(json);
        PerformanceJsonReport report = root.get("report") == null
                ? null
                : reportStorage.fromJson(JsonUtil.toJsonStr(root.get("report")));
        return PerformanceWorkerRunStatusResponse.builder()
                .runId(stringValue(root, "runId", ""))
                .workerId(stringValue(root, "workerId", ""))
                .status(stringValue(root, "status", PerformanceRunStatus.UNKNOWN))
                .activeUsers(intValue(root, "activeUsers", 0))
                .totalUsers(intValue(root, "totalUsers", 0))
                .totalRequests(longValue(root, "totalRequests", 0))
                .successRequests(longValue(root, "successRequests", 0))
                .failedRequests(longValue(root, "failedRequests", 0))
                .qps(doubleValue(root, "qps", 0))
                .report(report)
                .error(stringValue(root, "error", ""))
                .build();
    }

    public PerformanceWorkerRunAcceptedResponse acceptedResponseFromJson(String json) {
        Map<String, Object> root = root(json);
        return PerformanceWorkerRunAcceptedResponse.builder()
                .runId(stringValue(root, "runId", ""))
                .workerId(stringValue(root, "workerId", ""))
                .status(stringValue(root, "status", PerformanceRunStatus.ACCEPTED))
                .message(stringValue(root, "message", ""))
                .build();
    }

    public PerformanceWorkerRunResultResponse resultResponseFromJson(String json) {
        Map<String, Object> root = root(json);
        PerformanceJsonReport report = root.get("report") == null
                ? null
                : reportStorage.fromJson(JsonUtil.toJsonStr(root.get("report")));
        return PerformanceWorkerRunResultResponse.builder()
                .runId(stringValue(root, "runId", ""))
                .workerId(stringValue(root, "workerId", ""))
                .status(stringValue(root, "status", PerformanceRunStatus.UNKNOWN))
                .report(report)
                .error(stringValue(root, "error", ""))
                .build();
    }

    private Map<String, Object> toMap(Object value) {
        if (value instanceof PerformanceWorkerRunRequest request) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("runId", request.getRunId());
            json.put("plan", request.getPlan() == null
                    ? null
                    : JsonUtil.convertValue(JsonUtil.readTree(planStorage.toJson(request.getPlan())), Map.class));
            json.put("assignment", request.getAssignment() == null
                    ? null
                    : JsonUtil.convertValue(JsonUtil.readTree(assignmentStorage.toJson(request.getAssignment())), Map.class));
            return json;
        }
        if (value instanceof PerformanceWorkerRunAcceptedResponse response) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("runId", response.getRunId());
            json.put("workerId", response.getWorkerId());
            json.put("status", response.getStatus());
            json.put("message", response.getMessage());
            return json;
        }
        if (value instanceof PerformanceWorkerRunStatusResponse response) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("runId", response.getRunId());
            json.put("workerId", response.getWorkerId());
            json.put("status", response.getStatus());
            json.put("activeUsers", response.getActiveUsers());
            json.put("totalUsers", response.getTotalUsers());
            json.put("totalRequests", response.getTotalRequests());
            json.put("successRequests", response.getSuccessRequests());
            json.put("failedRequests", response.getFailedRequests());
            json.put("qps", response.getQps());
            json.put("report", response.getReport() == null ? null : reportStorage.toMap(response.getReport()));
            json.put("error", response.getError());
            return json;
        }
        if (value instanceof PerformanceWorkerRunResultResponse response) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("runId", response.getRunId());
            json.put("workerId", response.getWorkerId());
            json.put("status", response.getStatus());
            json.put("report", response.getReport() == null ? null : reportStorage.toMap(response.getReport()));
            json.put("error", response.getError());
            return json;
        }
        if (value instanceof PerformanceWorkerErrorResponse response) {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("error", response.getError());
            return json;
        }
        return JsonUtil.convertValue(value, Map.class);
    }

    private Map<String, Object> root(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        return objectMap(JsonUtil.convertValue(JsonUtil.readTree(json), Map.class));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private String stringValue(Map<String, Object> json, String key, String defaultValue) {
        Object value = json.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private long longValue(Map<String, Object> json, String key, long defaultValue) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private int intValue(Map<String, Object> json, String key, int defaultValue) {
        long value = longValue(json, key, defaultValue);
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private double doubleValue(Map<String, Object> json, String key, double defaultValue) {
        Object value = json.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}
