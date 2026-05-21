package com.laker.postman.panel.performance.model;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

public enum PerformanceProtocol {
    HTTP(MessageKeys.PERFORMANCE_PROTOCOL_HTTP),
    WEBSOCKET(MessageKeys.PERFORMANCE_PROTOCOL_WEBSOCKET),
    SSE(MessageKeys.PERFORMANCE_PROTOCOL_SSE);

    private final String displayKey;

    PerformanceProtocol(String displayKey) {
        this.displayKey = displayKey;
    }

    public String getDisplayName() {
        return I18nUtil.getMessage(displayKey);
    }
}
