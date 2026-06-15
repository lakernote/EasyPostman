package com.laker.postman.http.runtime.observation;

/**
 * HTTP 执行层向外发布的网络日志事件。
 *
 * @param stage      执行阶段
 * @param message    日志内容
 * @param elapsedMs  相对本次 callStart 的耗时；重定向这类外部事件可为空
 * @param durationMs 当前阶段自身耗时；start 事件、外部事件或不可计算时为空
 */
public record NetworkLogEvent(NetworkLogEventStage stage, String message, Long elapsedMs, Long durationMs) {

    public NetworkLogEvent(NetworkLogEventStage stage, String message, Long elapsedMs) {
        this(stage, message, elapsedMs, null);
    }
}
