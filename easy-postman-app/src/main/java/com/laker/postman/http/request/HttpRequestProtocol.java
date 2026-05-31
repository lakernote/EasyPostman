package com.laker.postman.http.request;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpRequestItem;
import lombok.experimental.UtilityClass;

@UtilityClass
public class HttpRequestProtocol {
    private static final String ACCEPT_HEADER = "Accept";
    private static final String SSE_MEDIA_TYPE = "text/event-stream";

    public static boolean isSse(PreparedRequest request) {
        if (request == null || request.headersList == null) {
            return false;
        }
        return request.headersList.stream().anyMatch(HttpRequestProtocol::isSseAcceptHeader);
    }

    public static boolean isSse(HttpRequestItem item) {
        if (item == null || item.getHeadersList() == null || item.getHeadersList().isEmpty()) {
            return false;
        }
        return item.getHeadersList().stream().anyMatch(HttpRequestProtocol::isSseAcceptHeader);
    }

    public static boolean isWebSocketUrl(String url) {
        return url != null && (url.startsWith("ws://") || url.startsWith("wss://"));
    }

    private static boolean isSseAcceptHeader(HttpHeader header) {
        return header.isEnabled()
                && ACCEPT_HEADER.equalsIgnoreCase(header.getKey())
                && header.getValue() != null
                && header.getValue().toLowerCase().contains(SSE_MEDIA_TYPE);
    }
}
