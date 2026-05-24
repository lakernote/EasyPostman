package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.PerformanceTreeRules;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.service.http.HttpUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
class PerformanceRequestProtocolResolver {

    boolean isSseRequest(HttpRequestItem item) {
        return PerformanceTreeRules.isSsePerfRequest(item);
    }

    boolean isSseRequest(HttpRequestItem item, PreparedRequest request) {
        RequestItemProtocolEnum protocol = resolveProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(request));
    }

    boolean isWebSocketRequest(HttpRequestItem item) {
        return PerformanceTreeRules.isWebSocketPerfRequest(item);
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
        return PerformanceTreeRules.resolveRequestProtocol(item);
    }
}
