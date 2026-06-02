package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;
import okhttp3.Response;

/**
 * 统一渲染 SSE/WebSocket 握手响应日志，避免两个实时协议显示字段不一致。
 */
@UtilityClass
class RealtimeHandshakeNetworkLogFormatter {

    static String formatResponseSnapshot(PreparedRequest request, Response response) {
        if (response == null) {
            return "";
        }
        HttpEventInfo traceInfo = HttpExchangeTraceSupport.resolveFromRequest(request);
        StringBuilder sb = new StringBuilder("\n");
        sb.append("Status: ").append(response.code());
        if (!isBlank(response.message())) {
            sb.append(" ").append(response.message());
        }
        sb.append("\n");
        sb.append("Thread: ").append(resolveThread(traceInfo)).append("\n");
        sb.append("Connection: ").append(resolveConnection(traceInfo)).append("\n");
        sb.append("Protocol: ").append(resolveProtocol(traceInfo, response)).append("\n");
        for (int i = 0; i < response.headers().size(); i++) {
            sb.append(response.headers().name(i))
                    .append(": ")
                    .append(response.headers().value(i))
                    .append("\n");
        }
        return sb.toString();
    }

    private static String resolveThread(HttpEventInfo traceInfo) {
        if (traceInfo != null && !isBlank(traceInfo.getThreadName())) {
            return traceInfo.getThreadName();
        }
        return Thread.currentThread().getName();
    }

    private static String resolveConnection(HttpEventInfo traceInfo) {
        if (traceInfo == null) {
            return "-";
        }
        String local = traceInfo.getLocalAddress();
        String remote = traceInfo.getRemoteAddress();
        if (isBlank(local) && isBlank(remote)) {
            return "-";
        }
        return nullToDash(local) + " → " + nullToDash(remote);
    }

    private static String resolveProtocol(HttpEventInfo traceInfo, Response response) {
        if (response != null && response.protocol() != null) {
            return response.protocol().toString();
        }
        if (traceInfo != null && !isBlank(traceInfo.getProtocol())) {
            return traceInfo.getProtocol();
        }
        return "-";
    }

    private static String nullToDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
