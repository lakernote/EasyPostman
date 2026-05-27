package com.laker.postman.panel.performance.model;

/**
 * SSE 压测配置，挂载在 REQUEST 节点上。
 */
public class SsePerformanceData {

    public enum CompletionMode {
        SINGLE_MESSAGE,
        UNTIL_MATCH,
        FIXED_DURATION,
        MESSAGE_COUNT,
        STREAM_CLOSED
    }

    public int connectTimeoutMs = 10000;
    public CompletionMode completionMode = CompletionMode.SINGLE_MESSAGE;
    public int firstMessageTimeoutMs = 10000;
    public int holdConnectionMs = 30000;
    public int targetMessageCount = 1;
    public String eventNameFilter = "";
    public String messageFilter = "";

    public static boolean usesEventNameFilter(CompletionMode mode) {
        return mode == CompletionMode.UNTIL_MATCH || mode == CompletionMode.MESSAGE_COUNT;
    }
}
