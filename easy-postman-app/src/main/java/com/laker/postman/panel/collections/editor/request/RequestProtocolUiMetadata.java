package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.util.IconUtil;
import lombok.experimental.UtilityClass;

import javax.swing.*;

@UtilityClass
public class RequestProtocolUiMetadata {
    private static final int DEFAULT_ICON_SIZE = 24;

    public static Icon iconFor(RequestItemProtocolEnum protocol) {
        return iconFor(protocol, DEFAULT_ICON_SIZE);
    }

    public static Icon iconFor(RequestItemProtocolEnum protocol, int size) {
        if (protocol == null) {
            return null;
        }
        return switch (protocol) {
            case HTTP -> IconUtil.create("icons/http.svg", size, size);
            case WEBSOCKET -> IconUtil.create("icons/websocket.svg", size, size);
            case SSE -> IconUtil.create("icons/sse.svg", size, size);
            case SAVED_RESPONSE -> IconUtil.createThemed("icons/save-response.svg", size, size);
        };
    }
}
