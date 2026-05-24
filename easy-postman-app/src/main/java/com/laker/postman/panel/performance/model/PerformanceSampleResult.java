package com.laker.postman.panel.performance.model;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class PerformanceSampleResult {
    String apiId;
    String apiName;
    PreparedRequest request;
    HttpResponse response;
    String errorMsg;
    List<TestResult> testResults;
    List<PerformanceAssertionResult> assertionResults;
    boolean executionFailed;
    boolean interrupted;
    PerformanceProtocol protocol;
    long startTimeMs;
    long endTimeMs;
    long elapsedTimeMs;
    int responseCode;
    long bodySize;
    long headersSize;
    int sentMessages;
    int receivedMessages;
    int matchedMessages;
    long firstMessageLatencyMs;
    boolean successful;

    public static PerformanceSampleResult fromExecutionResult(PerformanceRequestExecutionResult executionResult) {
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
        List<TestResult> testResults = executionResult.testResults == null
                ? List.of()
                : List.copyOf(executionResult.testResults);
        return PerformanceSampleResult.builder()
                .apiId(executionResult.apiId)
                .apiName(executionResult.apiName)
                .request(executionResult.request)
                .response(response)
                .errorMsg(executionResult.errorMsg)
                .testResults(testResults)
                .assertionResults(testResults.stream()
                        .map(PerformanceAssertionResult::fromTestResult)
                        .toList())
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
                .firstMessageLatencyMs(firstMessageLatency(response, protocol))
                .successful(ResultNodeInfo.isActuallySuccessful(
                        executionResult.executionFailed,
                        response,
                        testResults
                ))
                .build();
    }

    public RequestResult toRequestResult() {
        RequestResult result = new RequestResult(startTimeMs, endTimeMs, successful, apiId, protocol);
        result.endTime = endTimeMs;
        result.sentMessages = sentMessages;
        result.receivedMessages = receivedMessages;
        result.matchedMessages = matchedMessages;
        result.firstMessageLatencyMs = firstMessageLatencyMs;
        return result;
    }

    private static int receivedMessages(HttpResponse response, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET) {
            return streamMetric(response, protocol, "X-Easy-WS-Received-Count", 0);
        }
        if (protocol == PerformanceProtocol.SSE) {
            return streamMetric(response, protocol, "X-Easy-SSE-Event-Count", 0);
        }
        return 0;
    }

    private static int matchedMessages(HttpResponse response, PerformanceProtocol protocol) {
        if (protocol == PerformanceProtocol.WEBSOCKET) {
            return streamMetric(response, protocol, "X-Easy-WS-Message-Count", 0);
        }
        if (protocol == PerformanceProtocol.SSE) {
            return streamMetric(response, protocol, "X-Easy-SSE-Message-Count", 0);
        }
        return 0;
    }

    private static long firstMessageLatency(HttpResponse response, PerformanceProtocol protocol) {
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

    private static int streamMetric(HttpResponse response,
                                    PerformanceProtocol protocol,
                                    String header,
                                    int defaultValue) {
        if (response == null || protocol == PerformanceProtocol.HTTP) {
            return defaultValue;
        }
        return (int) headerLong(response.headers, header, defaultValue);
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
