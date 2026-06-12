package com.laker.postman.service.curl;

import com.laker.postman.request.model.HttpHeader;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
class CurlHeaderSupport {

    private static final List<String> RESTRICTED_WEBSOCKET_HEADERS = List.of(
            "Connection",
            "Host",
            "Upgrade",
            "Sec-WebSocket-Key",
            "Sec-WebSocket-Version",
            "Sec-WebSocket-Extensions"
    );

    static HttpHeader parseHeaderOption(String headerOption) {
        if (headerOption == null) {
            return null;
        }

        int keyEnd = headerOption.indexOf(':');
        if (keyEnd < 0 && headerOption.endsWith(";")) {
            keyEnd = headerOption.length() - 1;
        }
        if (keyEnd < 0) {
            return null;
        }

        String headerName = headerOption.substring(0, keyEnd).trim();
        if (headerName.isEmpty()) {
            return null;
        }
        String headerValue = keyEnd < headerOption.length() && headerOption.charAt(keyEnd) == ':'
                ? headerOption.substring(keyEnd + 1).trim()
                : "";
        return new HttpHeader(true, headerName, headerValue);
    }

    static void setHeader(List<HttpHeader> headers, String headerName, String headerValue) {
        for (HttpHeader header : headers) {
            if (header != null && header.getKey() != null && header.getKey().equalsIgnoreCase(headerName)) {
                header.setValue(headerValue);
                return;
            }
        }
        headers.add(new HttpHeader(true, headerName, headerValue));
    }

    static boolean hasEnabledHeader(List<HttpHeader> headers, String headerName) {
        return findEnabledHeaderValue(headers, headerName) != null;
    }

    static String findEnabledHeaderValue(List<HttpHeader> headers, String headerName) {
        if (headers == null) {
            return null;
        }
        for (HttpHeader header : headers) {
            if (header != null
                    && header.isEnabled()
                    && header.getKey() != null
                    && header.getKey().equalsIgnoreCase(headerName)) {
                return header.getValue() == null ? "" : header.getValue();
            }
        }
        return null;
    }

    static void filterRestrictedWebSocketHeaders(CurlRequest req) {
        if (req == null || req.headersList == null || !CurlUrlSupport.isWebSocketUrl(req.url)) {
            return;
        }
        req.headersList.removeIf(header -> header != null && isRestrictedWebSocketHeader(header.getKey()));
    }

    private static boolean isRestrictedWebSocketHeader(String headerName) {
        if (headerName == null) {
            return false;
        }
        for (String restrictedHeader : RESTRICTED_WEBSOCKET_HEADERS) {
            if (restrictedHeader.equalsIgnoreCase(headerName.trim())) {
                return true;
            }
        }
        return false;
    }
}
