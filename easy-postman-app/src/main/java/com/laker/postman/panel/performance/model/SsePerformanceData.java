package com.laker.postman.panel.performance.model;

/**
 * SSE 压测配置，挂载在 REQUEST 节点上。
 */
public class SsePerformanceData {

    public enum CompletionMode {
        FIRST_MESSAGE,
        FIXED_DURATION,
        MESSAGE_COUNT
    }

    public int connectTimeoutMs = 10000;
    public CompletionMode completionMode = CompletionMode.FIRST_MESSAGE;
    public int firstMessageTimeoutMs = 10000;
    public int holdConnectionMs = 30000;
    public int targetMessageCount = 1;
    public String eventNameFilter = "";
}
