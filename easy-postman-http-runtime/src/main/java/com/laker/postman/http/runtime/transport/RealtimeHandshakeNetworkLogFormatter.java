package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;
import okhttp3.Response;

import java.util.Locale;

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
        boolean websocketRequest = isWebSocketRequest(request);
        boolean sseRequest = !websocketRequest && isSseRequest(request, response);
        if (websocketRequest) {
            sb.append("Result: ").append(webSocketUpgradeResult(response)).append("\n");
            sb.append("WebSocket URL: ").append(valueOrDash(request != null ? request.url : null)).append("\n");
        } else if (sseRequest) {
            sb.append("Result: ").append(sseStreamResult(response)).append("\n");
            sb.append("SSE URL: ").append(valueOrDash(request != null ? request.url : null)).append("\n");
        }
        sb.append("Thread: ").append(resolveThread(traceInfo)).append("\n");
        sb.append("Connection: ").append(resolveConnection(traceInfo)).append("\n");
        sb.append(protocolLabel(websocketRequest, sseRequest))
                .append(resolveProtocol(traceInfo, response))
                .append("\n");
        for (int i = 0; i < response.headers().size(); i++) {
            sb.append(response.headers().name(i))
                    .append(": ")
                    .append(response.headers().value(i))
                    .append("\n");
        }
        return sb.toString();
    }

    private static boolean isWebSocketRequest(PreparedRequest request) {
        if (request == null || request.url == null) {
            return false;
        }
        String lower = request.url.toLowerCase(Locale.ROOT);
        return lower.startsWith("ws://") || lower.startsWith("wss://");
    }

    private static boolean isSseRequest(PreparedRequest request, Response response) {
        if (isSseResponse(response)) {
            return true;
        }
        if (request == null || request.headersList == null) {
            return false;
        }
        for (HttpHeader header : request.headersList) {
            if (header == null || header.getKey() == null || header.getValue() == null) {
                continue;
            }
            if ("Accept".equalsIgnoreCase(header.getKey())
                    && header.getValue().toLowerCase(Locale.ROOT).contains("text/event-stream")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSseResponse(Response response) {
        if (response == null) {
            return false;
        }
        String contentType = response.header("Content-Type", "");
        return contentType.toLowerCase(Locale.ROOT).contains("text/event-stream");
    }

    private static String webSocketUpgradeResult(Response response) {
        return response.code() == 101 ? "WebSocket upgrade accepted" : "WebSocket upgrade response";
    }

    private static String sseStreamResult(Response response) {
        return response.code() == 200 && isSseResponse(response) ? "SSE stream accepted" : "SSE HTTP response";
    }

    private static String protocolLabel(boolean websocketRequest, boolean sseRequest) {
        if (websocketRequest) {
            return "HTTP Handshake Protocol: ";
        }
        if (sseRequest) {
            return "HTTP Stream Protocol: ";
        }
        return "Protocol: ";
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

    private static String valueOrDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
