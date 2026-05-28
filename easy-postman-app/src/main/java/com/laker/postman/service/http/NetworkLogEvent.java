package com.laker.postman.service.http;

/**
 * HTTP 执行层向外发布的网络日志事件。
 *
 * @param stage     执行阶段
 * @param message   日志内容
 * @param elapsedMs 相对本次 callStart 的耗时；重定向这类外部事件可为空
 */
public record NetworkLogEvent(NetworkLogEventStage stage, String message, Long elapsedMs) {
}
