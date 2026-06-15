package com.laker.postman.http.runtime.observation;

/**
 * 网络日志输出端口。
 * <p>
 * HTTP 执行层只依赖这个端口发布事件，不直接访问 Swing 面板；GUI、Functional、Performance
 * 可以按场景传入不同实现，避免底层传输代码反向依赖 UI。
 */
@FunctionalInterface
public interface NetworkLogSink {
    NetworkLogSink NOOP = event -> {
    };

    void append(NetworkLogEvent event);

    static NetworkLogSink noop() {
        return NOOP;
    }

    default void append(NetworkLogEventStage stage, String message, Long elapsedMs) {
        append(new NetworkLogEvent(stage, message, elapsedMs));
    }

    default void append(NetworkLogEventStage stage, String message, Long elapsedMs, Long durationMs) {
        append(new NetworkLogEvent(stage, message, elapsedMs, durationMs));
    }
}
