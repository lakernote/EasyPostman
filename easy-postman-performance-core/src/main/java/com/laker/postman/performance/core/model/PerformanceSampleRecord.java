package com.laker.postman.performance.core.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class PerformanceSampleRecord {
    String apiId;
    String apiName;
    String errorMsg;
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

    public PerformanceSampleRecord(String apiId,
                                   String apiName,
                                   String errorMsg,
                                   boolean executionFailed,
                                   boolean interrupted,
                                   PerformanceProtocol protocol,
                                   long startTimeMs,
                                   long endTimeMs,
                                   long elapsedTimeMs,
                                   int responseCode,
                                   long bodySize,
                                   long headersSize,
                                   int sentMessages,
                                   int receivedMessages,
                                   int matchedMessages,
                                   long firstMessageLatencyMs,
                                   boolean successful) {
        this.apiId = apiId == null ? "" : apiId;
        this.apiName = apiName == null ? "" : apiName;
        this.errorMsg = errorMsg == null ? "" : errorMsg;
        this.executionFailed = executionFailed;
        this.interrupted = interrupted;
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
        this.startTimeMs = Math.max(0, startTimeMs);
        long resolvedEndTimeMs = endTimeMs > 0 ? endTimeMs : this.startTimeMs + Math.max(0, elapsedTimeMs);
        this.endTimeMs = Math.max(this.startTimeMs, resolvedEndTimeMs);
        this.elapsedTimeMs = elapsedTimeMs > 0 ? elapsedTimeMs : Math.max(0, this.endTimeMs - this.startTimeMs);
        this.responseCode = Math.max(0, responseCode);
        this.bodySize = Math.max(0, bodySize);
        this.headersSize = Math.max(0, headersSize);
        this.sentMessages = Math.max(0, sentMessages);
        this.receivedMessages = Math.max(0, receivedMessages);
        this.matchedMessages = Math.max(0, matchedMessages);
        this.firstMessageLatencyMs = firstMessageLatencyMs;
        this.successful = successful;
    }

    public RequestResult toRequestResult() {
        RequestResult result = new RequestResult(startTimeMs, endTimeMs, successful, apiId, apiName, protocol);
        result.endTime = endTimeMs;
        result.sentMessages = sentMessages;
        result.receivedMessages = receivedMessages;
        result.matchedMessages = matchedMessages;
        result.firstMessageLatencyMs = firstMessageLatencyMs;
        return result;
    }
}
