package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceSampleRecord;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.List;

final class OkHttpSampleRecords {
    private OkHttpSampleRecords() {
    }

    static PerformanceSampleRecord httpRecord(PerformanceOutboundRequest request,
                                              Response response,
                                              long startTimeMs) throws IOException {
        ResponseBody responseBody = response.body();
        byte[] bodyBytes = responseBody == null ? new byte[0] : responseBody.bytes();
        long endTimeMs = System.currentTimeMillis();
        int statusCode = response.code();
        return PerformanceSampleRecord.builder()
                .apiId(request.getId())
                .apiName(request.getName())
                .protocol(resolveProtocol(request))
                .startTimeMs(startTimeMs)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(endTimeMs - startTimeMs)
                .responseCode(statusCode)
                .bodySize(bodyBytes.length)
                .headersSize(headersSize(response.headers().toMultimap()))
                .executionFailed(false)
                .interrupted(false)
                .successful(statusCode >= 200 && statusCode < 400)
                .build();
    }

    static PerformanceSampleRecord streamRecord(PerformanceOutboundRequest request,
                                                PerformanceProtocol protocol,
                                                long startTimeMs,
                                                int responseCode,
                                                int receivedMessages,
                                                long firstMessageLatencyMs,
                                                String errorMessage,
                                                boolean executionFailed,
                                                boolean successful) {
        long endTimeMs = System.currentTimeMillis();
        return PerformanceSampleRecord.builder()
                .apiId(request.getId())
                .apiName(request.getName())
                .protocol(protocol)
                .startTimeMs(startTimeMs)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(endTimeMs - startTimeMs)
                .responseCode(responseCode)
                .receivedMessages(receivedMessages)
                .matchedMessages(0)
                .firstMessageLatencyMs(firstMessageLatencyMs)
                .errorMsg(errorMessage)
                .executionFailed(executionFailed)
                .interrupted(false)
                .successful(successful)
                .build();
    }

    static PerformanceSampleRecord failedRecord(PerformanceOutboundRequest request,
                                                long startTimeMs,
                                                Exception exception) {
        long endTimeMs = System.currentTimeMillis();
        return PerformanceSampleRecord.builder()
                .apiId(request.getId())
                .apiName(request.getName())
                .protocol(resolveProtocol(request))
                .startTimeMs(startTimeMs)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(endTimeMs - startTimeMs)
                .errorMsg(exception == null ? "" : exception.getMessage())
                .executionFailed(true)
                .interrupted(exception instanceof InterruptedException)
                .successful(false)
                .build();
    }

    static PerformanceSampleRecord interruptedRecord(PerformanceOutboundRequest request,
                                                     long startTimeMs,
                                                     InterruptedException exception) {
        long endTimeMs = System.currentTimeMillis();
        return PerformanceSampleRecord.builder()
                .apiId(request.getId())
                .apiName(request.getName())
                .protocol(resolveProtocol(request))
                .startTimeMs(startTimeMs)
                .endTimeMs(endTimeMs)
                .elapsedTimeMs(endTimeMs - startTimeMs)
                .errorMsg(exception == null ? "" : exception.getMessage())
                .executionFailed(true)
                .interrupted(true)
                .successful(false)
                .build();
    }

    private static PerformanceProtocol resolveProtocol(PerformanceOutboundRequest request) {
        return request.getProtocol() == null ? PerformanceProtocol.HTTP : request.getProtocol();
    }

    private static long headersSize(java.util.Map<String, List<String>> headers) {
        long total = 0;
        if (headers == null) {
            return 0;
        }
        for (java.util.Map.Entry<String, List<String>> entry : headers.entrySet()) {
            total += entry.getKey() == null ? 0 : entry.getKey().length();
            for (String value : entry.getValue()) {
                total += value == null ? 0 : value.length();
            }
        }
        return total;
    }
}
