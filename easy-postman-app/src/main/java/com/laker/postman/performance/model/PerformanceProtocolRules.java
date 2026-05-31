package com.laker.postman.performance.model;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;



import com.laker.postman.http.request.HttpRequestProtocol;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceProtocolRules {

    public RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    public boolean isSsePerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpRequestProtocol.isSse(item));
    }

    public boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return resolveRequestProtocol(item).isWebSocketProtocol();
    }
}
