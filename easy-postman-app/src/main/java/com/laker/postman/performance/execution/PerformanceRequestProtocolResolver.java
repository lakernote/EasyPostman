package com.laker.postman.performance.execution;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.performance.model.PerformanceProtocolRules;
import com.laker.postman.http.request.HttpRequestProtocol;
import lombok.experimental.UtilityClass;

@UtilityClass
class PerformanceRequestProtocolResolver {

    boolean isSseRequest(HttpRequestItem item) {
        return PerformanceProtocolRules.isSsePerfRequest(item);
    }

    boolean isSseRequest(PerformanceRequestSnapshot snapshot) {
        return snapshot != null && snapshot.isSse();
    }

    boolean isSseRequest(HttpRequestItem item, PreparedRequest request) {
        RequestItemProtocolEnum protocol = resolveProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpRequestProtocol.isSse(request));
    }

    boolean isSseRequest(PerformanceRequestSnapshot snapshot, PreparedRequest request) {
        PerformanceProtocol protocol = snapshot == null ? PerformanceProtocol.HTTP : snapshot.getProtocol();
        return protocol == PerformanceProtocol.SSE
                || (protocol == PerformanceProtocol.HTTP && HttpRequestProtocol.isSse(request));
    }

    boolean isWebSocketRequest(HttpRequestItem item) {
        return PerformanceProtocolRules.isWebSocketPerfRequest(item);
    }

    boolean isWebSocketRequest(PerformanceRequestSnapshot snapshot) {
        return snapshot != null && snapshot.isWebSocket();
    }

    PerformanceProtocol resolvePerformanceProtocol(boolean webSocketRequest, boolean sseRequest) {
        if (webSocketRequest) {
            return PerformanceProtocol.WEBSOCKET;
        }
        if (sseRequest) {
            return PerformanceProtocol.SSE;
        }
        return PerformanceProtocol.HTTP;
    }

    private RequestItemProtocolEnum resolveProtocol(HttpRequestItem item) {
        return PerformanceProtocolRules.resolveRequestProtocol(item);
    }
}
