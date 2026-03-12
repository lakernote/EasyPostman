package com.laker.postman.panel.performance.model;

/**
 * WebSocket 压测配置，挂载在 REQUEST 节点上。
 */
public class WebSocketPerformanceData {

    public enum SendMode {
        NONE,
        REQUEST_BODY_ON_CONNECT,
        REQUEST_BODY_REPEAT
    }

    public enum CompletionMode {
        FIRST_MESSAGE,
        MATCHED_MESSAGE,
        FIXED_DURATION,
        MESSAGE_COUNT
    }

    public int connectTimeoutMs = 10000;
    public SendMode sendMode = SendMode.REQUEST_BODY_ON_CONNECT;
    public int sendCount = 1;
    public int sendIntervalMs = 1000;
    public CompletionMode completionMode = CompletionMode.FIRST_MESSAGE;
    public int firstMessageTimeoutMs = 10000;
    public int holdConnectionMs = 30000;
    public int targetMessageCount = 1;
    public String messageFilter = "";
}
