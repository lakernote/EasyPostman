package com.laker.postman.performance.core.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class PerformanceStatsSnapshot {

    private final List<ApiSummary> summaries;
    private final Map<PerformanceProtocol, ApiSummary> totalsByProtocol;
    private final long totalRequests;
    private final long successRequests;
    private final long retainedRequestResultCount;

    PerformanceStatsSnapshot(List<ApiSummary> summaries,
                             Map<PerformanceProtocol, ApiSummary> totalsByProtocol,
                             long totalRequests,
                             long successRequests,
                             long retainedRequestResultCount) {
        this.summaries = List.copyOf(summaries);
        this.totalsByProtocol = new EnumMap<>(PerformanceProtocol.class);
        this.totalsByProtocol.putAll(totalsByProtocol);
        this.totalRequests = totalRequests;
        this.successRequests = successRequests;
        this.retainedRequestResultCount = retainedRequestResultCount;
    }

    public List<ApiSummary> summaries() {
        return summaries;
    }

    public long totalRequests() {
        return totalRequests;
    }

    public long successRequests() {
        return successRequests;
    }

    public long retainedRequestResultCount() {
        return retainedRequestResultCount;
    }

    public ApiSummary totalFor(PerformanceProtocol protocol, String name) {
        ApiSummary summary = totalsByProtocol.get(protocol);
        return summary == null ? null : summary.withName(name);
    }

    public record ApiSummary(
            String apiId,
            String name,
            PerformanceProtocol protocol,
            long total,
            long success,
            long fail,
            double successRate,
            // QPS：请求数 /（最后一个样本结束时间 - 第一个样本开始时间）
            double samplesPerSecond,
            // 第一个样本开始时间，用于 master 合并 worker 时按全局窗口重算 QPS
            long firstSampleStartTimeMs,
            // 最后一个样本结束时间，用于 master 合并 worker 时按全局窗口重算 QPS
            long lastSampleEndTimeMs,
            // 请求耗时统计：只统计 sampler/request 本身，不包含前置脚本、后置脚本、断言和 UI 渲染
            DurationStats durationStats,
            long sentMessages,
            long receivedMessages,
            long matchedMessages,
            double sendRate,
            double receiveRate,
            double matchedRate,
            // 发送字节数：请求头 + 请求体
            long sentBytes,
            // 接收字节数：响应头 + 响应体
            long receivedBytes,
            // 发送字节速率：sentBytes / sample 时间窗口，UI 可按 KB/s 展示
            double sentBytesPerSecond,
            // 接收字节速率：receivedBytes / sample 时间窗口，UI 可按 KB/s 展示
            double receivedBytesPerSecond,
            // 平均接收字节数：receivedBytes / total，用于对齐 JMeter Avg. Bytes 口径
            long avgReceivedBytes,
            long avgFirstMessageLatencyMs,
            DurationStats firstMessageLatencyStats
    ) {
        ApiSummary withName(String newName) {
            return new ApiSummary(
                    apiId,
                    newName,
                    protocol,
                    total,
                    success,
                    fail,
                    successRate,
                    samplesPerSecond,
                    firstSampleStartTimeMs,
                    lastSampleEndTimeMs,
                    durationStats,
                    sentMessages,
                    receivedMessages,
                    matchedMessages,
                    sendRate,
                    receiveRate,
                    matchedRate,
                    sentBytes,
                    receivedBytes,
                    sentBytesPerSecond,
                    receivedBytesPerSecond,
                    avgReceivedBytes,
                    avgFirstMessageLatencyMs,
                    firstMessageLatencyStats
            );
        }
    }

    public record DurationStats(
            long avg,
            long min,
            long max,
            long p90,
            long p95,
            long p99
    ) {
        public static DurationStats empty() {
            return new DurationStats(0, 0, 0, 0, 0, 0);
        }
    }
}
