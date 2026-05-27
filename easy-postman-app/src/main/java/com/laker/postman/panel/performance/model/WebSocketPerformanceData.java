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

    public enum SendContentSource {
        REQUEST_BODY,
        CUSTOM_TEXT
    }

    public enum CompletionMode {
        SINGLE_MESSAGE,
        UNTIL_MATCH,
        FIXED_DURATION,
        MESSAGE_COUNT
    }

    public int connectTimeoutMs = 10000;
    public SendMode sendMode = SendMode.REQUEST_BODY_ON_CONNECT;
    public SendContentSource sendContentSource = SendContentSource.REQUEST_BODY;
    public String customSendBody = "";
    public String sendPreScript = "";
    public int sendCount = 1;
    public int sendIntervalMs = 1000;
    public CompletionMode completionMode = CompletionMode.SINGLE_MESSAGE;
    public int firstMessageTimeoutMs = 10000;
    public int holdConnectionMs = 30000;
    public int targetMessageCount = 1;
    public String messageFilter = "";

    public static boolean usesMessageFilter(CompletionMode mode) {
        return mode == CompletionMode.UNTIL_MATCH || mode == CompletionMode.MESSAGE_COUNT;
    }

}
