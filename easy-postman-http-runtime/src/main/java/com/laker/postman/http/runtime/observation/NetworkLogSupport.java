package com.laker.postman.http.runtime.observation;

import com.laker.postman.http.runtime.model.HttpCaptureProfiles;
import com.laker.postman.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;

/**
 * 网络日志发布保护层。
 * <p>
 * 真实请求不能被日志面板影响，所有协议统一通过这里判断开关、解析 sink 并吞掉日志异常。
 */
@UtilityClass
public class NetworkLogSupport {

    public static boolean isEnabled(PreparedRequest request) {
        return request != null && HttpCaptureProfiles.resolve(request).emitNetworkLog();
    }

    public static NetworkLogSink resolveSink(PreparedRequest request) {
        return request == null || request.networkLogSink == null
                ? NetworkLogSink.noop()
                : request.networkLogSink;
    }

    public static void append(PreparedRequest request, NetworkLogEventStage stage, String message) {
        append(request, stage, message, null);
    }

    public static void append(PreparedRequest request, NetworkLogEventStage stage, String message, Long elapsedMs) {
        append(request, stage, message, elapsedMs, null);
    }

    public static void append(PreparedRequest request,
                              NetworkLogEventStage stage,
                              String message,
                              Long elapsedMs,
                              Long durationMs) {
        if (!isEnabled(request)) {
            return;
        }
        try {
            resolveSink(request).append(stage, message, elapsedMs, durationMs);
        } catch (Throwable ignored) {
            // 网络日志是观察能力，异常不能影响真实请求、SSE 或 WebSocket。
        }
    }
}
