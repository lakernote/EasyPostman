package com.laker.postman.common.component;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestMethodUiMetadata {

    public static String methodColorHex(String method) {
        return switch (method == null ? "" : method.toUpperCase()) {
            case "GET" -> "#4CAF50";
            case "POST" -> "#FF9800";
            case "PUT" -> "#2196F3";
            case "PATCH" -> "#9C27B0";
            case "DELETE" -> "#F44336";
            default -> "#7f8c8d";
        };
    }
}
