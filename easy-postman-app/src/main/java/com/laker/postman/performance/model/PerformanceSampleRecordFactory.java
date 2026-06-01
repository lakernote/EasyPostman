package com.laker.postman.performance.model;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.execution.PerformanceRequestExecutionResult;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Map;

@UtilityClass
public class PerformanceSampleRecordFactory {

    public PerformanceSampleRecord fromExecutionResult(PerformanceRequestExecutionResult executionResult) {
        if (executionResult == null) {
            return null;
        }
        HttpResponse response = executionResult.response;
        long elapsedTimeMs = response == null ? executionResult.fallbackCostMs : response.costMs;
        long endTimeMs = response != null && response.endTime > 0
                ? response.endTime
                : executionResult.requestStartTime + elapsedTimeMs;
        PerformanceProtocol protocol = executionResult.protocol == null
                ? PerformanceProtocol.HTTP
                : executionResult.protocol;
        // 热路径只生成轻量统计记录，UI 明细对象由结果表/详情面板按需懒加载。
        return PerformanceSampleRecord.builder()
                .apiId(executionResult.apiId)
                .apiName(executionResult.apiName)
                .errorMsg(executionResult.errorMsg)
                .executionFailed(executionResult.executionFailed)
                .interrupted(executionResult.interrupted)
                .protocol(protocol)
                .startTimeMs(executionResult.requestStartTime)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(elapsedTimeMs)
                .responseCode(response == null ? 0 : response.code)
                .bodySize(response == null ? 0 : response.bodySize)
                .headersSize(response == null ? 0 : response.headersSize)
                .sentMessages(streamMetric(response, protocol, "X-Easy-WS-Sent-Count", 0))
                .receivedMessages(receivedMessages(response, protocol))
                .matchedMessages(matchedMessages(response, protocol))
                .sentBytes(sentBytes(response))
                .receivedBytes(receivedBytes(response))
                .firstMessageLatencyMs(firstMessageLatency(response, protocol))
                .successful(!executionResult.interrupted && ResultNodeInfo.isActuallySuccessful(
                        executionResult.executionFailed,
                        response,
                        executionResult.testResults
                ))
                .build();
    }

    private long sentBytes(HttpResponse response) {
        if (response == null || response.httpEventInfo == null) {
            return 0L;
        }
        return Math.max(0L, response.httpEventInfo.getHeaderBytesSent())
                + Math.max(0L, response.httpEventInfo.getBodyBytesSent());
    }

    private long receivedBytes(HttpResponse response) {
        if (response == null) {
            return 0L;
        }
        if (response.httpEventInfo != null) {
            long eventBytes = Math.max(0L, response.httpEventInfo.getHeaderBytesReceived())
                    + Math.max(0L, response.httpEventInfo.getBodyBytesReceived());
            if (eventBytes > 0) {
                return eventBytes;
            }
        }
        return Math.max(0L, response.headersSize) + Math.max(0L, response.bodySize);
    }

    private int receivedMessages(HttpResponse response, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET) {
            return streamMetric(response, protocol, "X-Easy-WS-Received-Count", 0);
        }
        if (protocol == PerformanceProtocol.SSE) {
            return streamMetric(response, protocol, "X-Easy-SSE-Event-Count", 0);
        }
        return 0;
    }

    private int matchedMessages(HttpResponse response, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET) {
            return streamMetric(response, protocol, "X-Easy-WS-Message-Count", 0);
        }
        if (protocol == PerformanceProtocol.SSE) {
            return streamMetric(response, protocol, "X-Easy-SSE-Message-Count", 0);
        }
        return 0;
    }

    private long firstMessageLatency(HttpResponse response, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET) {
            return headerLong(response == null ? null : response.headers,
                    "X-Easy-WS-First-Message-Latency-Ms", -1);
        }
        if (protocol == PerformanceProtocol.SSE) {
            return headerLong(response == null ? null : response.headers,
                    "X-Easy-SSE-First-Event-Latency-Ms", -1);
        }
        return -1;
    }

    private int streamMetric(HttpResponse response,
                             PerformanceProtocol protocol,
                             String header,
                             int defaultValue) {
        if (response == null || protocol == PerformanceProtocol.HTTP) {
            return defaultValue;
        }
        return (int) headerLong(response.headers, header, defaultValue);
    }

    private long headerLong(Map<String, List<String>> headers, String name, long defaultValue) {
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

    private String headerValue(Map<String, List<String>> headers, String name) {
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
