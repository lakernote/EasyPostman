package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.ResultNodeInfo;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class PerformanceResultRecorder {

    private final List<RequestResult> allRequestResults;
    private final Map<String, List<Long>> apiCostMap;
    private final Map<String, Integer> apiSuccessMap;
    private final Map<String, Integer> apiFailMap;
    private final Object statsLock;
    private final PerformanceResultTablePanel resultTablePanel;
    private final IntSupplier slowRequestThresholdSupplier;

    public PerformanceResultRecorder(List<RequestResult> allRequestResults,
                                     Map<String, List<Long>> apiCostMap,
                                     Map<String, Integer> apiSuccessMap,
                                     Map<String, Integer> apiFailMap,
                                     Object statsLock,
                                     PerformanceResultTablePanel resultTablePanel,
                                     IntSupplier slowRequestThresholdSupplier) {
        this.allRequestResults = allRequestResults;
        this.apiCostMap = apiCostMap;
        this.apiSuccessMap = apiSuccessMap;
        this.apiFailMap = apiFailMap;
        this.statsLock = statsLock;
        this.resultTablePanel = resultTablePanel;
        this.slowRequestThresholdSupplier = slowRequestThresholdSupplier;
    }

    public void record(PerformanceRequestExecutionResult executionResult, boolean efficientMode) {
        if (executionResult == null || executionResult.interrupted) {
            return;
        }

        long cost = resolveCostMs(executionResult);
        long endTime = resolveEndTime(executionResult, cost);
        boolean actualSuccess = ResultNodeInfo.isActuallySuccessful(
                executionResult.executionFailed,
                executionResult.response,
                executionResult.testResults
        );
        int slowRequestThresholdMs = slowRequestThresholdSupplier.getAsInt();

        updateStatistics(executionResult, cost, endTime, actualSuccess);

        if (!shouldRecordResult(efficientMode, actualSuccess, cost, slowRequestThresholdMs)) {
            return;
        }

        resultTablePanel.addResult(PerformanceResultNodeInfoMapper.toDisplayNodeInfo(executionResult));
    }

    static boolean shouldRecordResult(boolean efficientMode,
                                      boolean actualSuccess,
                                      long costMs,
                                      int slowRequestThresholdMs) {
        if (!efficientMode) {
            return true;
        }
        if (!actualSuccess) {
            return true;
        }
        return slowRequestThresholdMs > 0 && costMs >= slowRequestThresholdMs;
    }

    private long resolveCostMs(PerformanceRequestExecutionResult executionResult) {
        return executionResult.response == null ? executionResult.fallbackCostMs : executionResult.response.costMs;
    }

    private long resolveEndTime(PerformanceRequestExecutionResult executionResult, long costMs) {
        if (executionResult.response == null) {
            return executionResult.requestStartTime + costMs;
        }
        return executionResult.response.endTime > 0
                ? executionResult.response.endTime
                : executionResult.requestStartTime + costMs;
    }

    private void updateStatistics(PerformanceRequestExecutionResult executionResult,
                                  long costMs,
                                  long endTime,
                                  boolean actualSuccess) {
        synchronized (statsLock) {
            allRequestResults.add(toRequestResult(executionResult, costMs, endTime, actualSuccess));
            apiCostMap
                    .computeIfAbsent(executionResult.apiId, key -> Collections.synchronizedList(new ArrayList<>()))
                    .add(costMs);
            if (actualSuccess) {
                apiSuccessMap.merge(executionResult.apiId, 1, Integer::sum);
            } else {
                apiFailMap.merge(executionResult.apiId, 1, Integer::sum);
            }
        }
    }

    static RequestResult toRequestResult(PerformanceRequestExecutionResult executionResult,
                                         long costMs,
                                         long endTime,
                                         boolean actualSuccess) {
        RequestResult result = new RequestResult(
                executionResult.requestStartTime,
                endTime,
                actualSuccess,
                executionResult.apiId,
                executionResult.protocol
        );
        result.endTime = endTime;
        if (executionResult.response == null) {
            result.endTime = executionResult.requestStartTime + costMs;
            return result;
        }
        if (executionResult.protocol == PerformanceProtocol.WEBSOCKET) {
            result.sentMessages = headerInt(executionResult.response.headers, "X-Easy-WS-Sent-Count");
            result.receivedMessages = headerInt(executionResult.response.headers, "X-Easy-WS-Received-Count");
            result.matchedMessages = headerInt(executionResult.response.headers, "X-Easy-WS-Message-Count");
            result.firstMessageLatencyMs = headerLong(executionResult.response.headers, "X-Easy-WS-First-Message-Latency-Ms", -1);
            result.completionReason = headerValue(executionResult.response.headers, "X-Easy-WS-Completion-Reason");
        } else if (executionResult.protocol == PerformanceProtocol.SSE) {
            result.receivedMessages = headerInt(executionResult.response.headers, "X-Easy-SSE-Event-Count");
            result.matchedMessages = headerInt(executionResult.response.headers, "X-Easy-SSE-Message-Count");
            result.firstMessageLatencyMs = headerLong(executionResult.response.headers, "X-Easy-SSE-First-Message-Latency-Ms", -1);
            result.completionReason = headerValue(executionResult.response.headers, "X-Easy-SSE-Completion-Reason");
        }
        return result;
    }

    private static int headerInt(Map<String, List<String>> headers, String name) {
        return (int) headerLong(headers, name, 0);
    }

    private static long headerLong(Map<String, List<String>> headers, String name, long defaultValue) {
        String value = headerValue(headers, name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String headerValue(Map<String, List<String>> headers, String name) {
        if (headers == null || name == null) {
            return "";
        }
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!name.equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            List<String> values = entry.getValue();
            if (values == null || values.isEmpty()) {
                return "";
            }
            return values.get(0) == null ? "" : values.get(0);
        }
        return "";
    }
}
