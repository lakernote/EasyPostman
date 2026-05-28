package com.laker.postman.panel.performance.model;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.service.http.HttpUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceProtocolRules {

    public RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    public boolean isSsePerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item));
    }

    public boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return resolveRequestProtocol(item).isWebSocketProtocol();
    }
}
