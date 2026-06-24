package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.util.Locale;

@UtilityClass
public class HttpRequestDisplayMetadata {

    public static String methodLabel(String method) {
        String normalizedMethod = normalize(method);
        return switch (normalizedMethod) {
            case "DELETE" -> "DEL";
            case "OPTIONS" -> "OPT";
            case "PATCH" -> "PAT";
            case "TRACE" -> "TRC";
            case "" -> "";
            default -> normalizedMethod;
        };
    }

    public static Color methodColor(String method) {
        return switch (normalize(method)) {
            case "GET" -> ModernColors.getHttpMethodGet();
            case "POST" -> ModernColors.getHttpMethodPost();
            case "PUT" -> ModernColors.getHttpMethodPut();
            case "PATCH" -> ModernColors.getHttpMethodPatch();
            case "DELETE" -> ModernColors.getHttpMethodDelete();
            default -> ModernColors.getHttpMethodDefault();
        };
    }

    public static String methodColorHex(String method) {
        return ModernColors.toHtmlColor(methodColor(method));
    }

    public static Color protocolColor(String protocol) {
        return switch (normalize(protocol)) {
            case "WS", "WEBSOCKET" -> ModernColors.getHttpProtocolWs();
            case "SSE" -> ModernColors.getHttpProtocolSse();
            default -> ModernColors.getHttpMethodDefault();
        };
    }

    public static String protocolColorHex(String protocol) {
        return ModernColors.toHtmlColor(protocolColor(protocol));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
