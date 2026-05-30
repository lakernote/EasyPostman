package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

@Value
public class PerformanceJsonReportStream {
    long sentMessages;
    long receivedMessages;
    long matchedMessages;
    double sendRate;
    double receiveRate;
    double matchedRate;

    @Builder
    public PerformanceJsonReportStream(Long sentMessages,
                                       Long receivedMessages,
                                       Long matchedMessages,
                                       Double sendRate,
                                       Double receiveRate,
                                       Double matchedRate) {
        this.sentMessages = Math.max(0L, sentMessages == null ? 0L : sentMessages);
        this.receivedMessages = Math.max(0L, receivedMessages == null ? 0L : receivedMessages);
        this.matchedMessages = Math.max(0L, matchedMessages == null ? 0L : matchedMessages);
        this.sendRate = finite(sendRate);
        this.receiveRate = finite(receiveRate);
        this.matchedRate = finite(matchedRate);
    }

    private static double finite(Double value) {
        return value == null || !Double.isFinite(value) ? 0D : value;
    }
}
