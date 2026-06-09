package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestMethodUiMetadata {

    public static String methodColorHex(String method) {
        return switch (method == null ? "" : method.toUpperCase()) {
            case "GET" -> ModernColors.toHtmlColor(ModernColors.getSuccess());
            case "POST" -> ModernColors.toHtmlColor(ModernColors.getWarning());
            case "PUT" -> ModernColors.toHtmlColor(ModernColors.getPrimary());
            case "PATCH" -> ModernColors.toHtmlColor(ModernColors.getAccent());
            case "DELETE" -> ModernColors.toHtmlColor(ModernColors.getError());
            default -> ModernColors.toHtmlColor(ModernColors.getNeutral());
        };
    }
}
