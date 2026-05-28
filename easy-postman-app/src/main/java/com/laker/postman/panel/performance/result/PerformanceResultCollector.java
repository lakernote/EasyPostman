package com.laker.postman.panel.performance.result;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.runtime.PerformanceCoreResultSink;


import com.laker.postman.panel.performance.execution.PerformanceRequestExecutionResult;
import com.laker.postman.panel.performance.model.PerformanceResultListener;
import com.laker.postman.panel.performance.model.PerformanceSampleEvent;
import com.laker.postman.panel.performance.model.PerformanceSampleResult;
import com.laker.postman.panel.performance.runtime.PerformanceResultSink;
import com.laker.postman.panel.performance.runtime.PerformanceResultSinkListenerAdapter;

import java.util.List;
import java.util.Map;

public final class PerformanceResultCollector {

    private final PerformanceResultSink resultSink;

    public PerformanceResultCollector(List<PerformanceResultListener> listeners) {
        this(new PerformanceResultSinkListenerAdapter(listeners));
    }

    public PerformanceResultCollector(PerformanceResultSink resultSink) {
        this.resultSink = resultSink == null ? PerformanceResultSink.NOOP : resultSink;
    }

    public void collect(PerformanceRequestExecutionResult executionResult, boolean efficientMode) {
        collect(executionResult, efficientMode, PerformanceCoreResultSink.NOOP);
    }

    public void collect(PerformanceRequestExecutionResult executionResult,
                        boolean efficientMode,
                        PerformanceCoreResultSink runResultSink) {
        if (executionResult == null) {
            return;
        }
        boolean recordableInterruptedResult = isRecordableInterruptedResult(executionResult);
        if (executionResult.interrupted && !recordableInterruptedResult) {
            return;
        }

        PerformanceSampleResult sampleResult = PerformanceSampleResult.fromExecutionResult(executionResult);
        if (sampleResult == null) {
            return;
        }
        PerformanceSampleEvent event = new PerformanceSampleEvent(sampleResult, executionResult, efficientMode);
        resultSink.onSample(event);
        PerformanceCoreResultSink resolvedRunSink = runResultSink == null
                ? PerformanceCoreResultSink.NOOP
                : runResultSink;
        if (resolvedRunSink.acceptsSamples()) {
            resolvedRunSink.onSample(event.sampleRecord());
        }
    }

    private boolean isRecordableInterruptedResult(PerformanceRequestExecutionResult executionResult) {
        if (!executionResult.interrupted || executionResult.response == null) {
            return false;
        }
        if (executionResult.response.code > 0) {
            return true;
        }
        if (executionResult.response.bodySize > 0
                || (executionResult.response.body != null && !executionResult.response.body.isBlank())) {
            return true;
        }
        if (executionResult.protocol == PerformanceProtocol.WEBSOCKET) {
            return hasAnyPositiveHeader(
                    executionResult.response.headers,
                    "X-Easy-WS-Sent-Count",
                    "X-Easy-WS-Received-Count",
                    "X-Easy-WS-Message-Count"
            );
        }
        if (executionResult.protocol == PerformanceProtocol.SSE) {
            return hasAnyPositiveHeader(
                    executionResult.response.headers,
                    "X-Easy-SSE-Event-Count",
                    "X-Easy-SSE-Message-Count"
            );
        }
        return false;
    }

    private static boolean hasAnyPositiveHeader(Map<String, List<String>> headers, String... names) {
        for (String name : names) {
            if (headerLong(headers, name, 0) > 0) {
                return true;
            }
        }
        return false;
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
