package com.laker.postman.performance.core.report;

import lombok.Builder;
import lombok.Value;

/**
 * 报告中的 HTTP 字节指标。
 */
@Value
public class PerformanceJsonReportBytes {
    // 发送总字节数：请求头 + 请求体
    long sentBytes;
    // 接收总字节数：响应头 + 响应体
    long receivedBytes;
    // 发送速率：sentBytes / sample 时间窗口，展示时可换算为 KB/s
    double sentBytesPerSecond;
    // 接收速率：receivedBytes / sample 时间窗口，展示时可换算为 KB/s
    double receivedBytesPerSecond;
    // 平均接收字节数：receivedBytes / total，对齐 JMeter Avg. Bytes
    long avgReceivedBytes;

    @Builder
    public PerformanceJsonReportBytes(Long sentBytes,
                                      Long receivedBytes,
                                      Double sentBytesPerSecond,
                                      Double receivedBytesPerSecond,
                                      Long avgReceivedBytes) {
        this.sentBytes = Math.max(0L, sentBytes == null ? 0L : sentBytes);
        this.receivedBytes = Math.max(0L, receivedBytes == null ? 0L : receivedBytes);
        this.sentBytesPerSecond = finite(sentBytesPerSecond);
        this.receivedBytesPerSecond = finite(receivedBytesPerSecond);
        this.avgReceivedBytes = Math.max(0L, avgReceivedBytes == null ? 0L : avgReceivedBytes);
    }

    private static double finite(Double value) {
        return value == null || !Double.isFinite(value) ? 0D : value;
    }
}
