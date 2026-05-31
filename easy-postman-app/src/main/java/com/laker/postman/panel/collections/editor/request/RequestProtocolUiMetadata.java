package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.IconUtil;

import javax.swing.*;

public final class RequestProtocolUiMetadata {
    private static final int DEFAULT_ICON_SIZE = 24;

    private RequestProtocolUiMetadata() {
    }

    public static Icon iconFor(RequestItemProtocolEnum protocol) {
        return iconFor(protocol, DEFAULT_ICON_SIZE);
    }

    public static Icon iconFor(RequestItemProtocolEnum protocol, int size) {
        if (protocol == null) {
            return null;
        }
        return switch (protocol) {
            case HTTP -> new FlatSVGIcon("icons/http.svg", size, size);
            case WEBSOCKET -> new FlatSVGIcon("icons/websocket.svg", size, size);
            case SSE -> new FlatSVGIcon("icons/sse.svg", size, size);
            case SAVED_RESPONSE -> IconUtil.createThemed("icons/save-response.svg", size, size);
        };
    }
}
