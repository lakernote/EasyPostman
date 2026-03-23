package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CaptureQuickFilterTokens {
    private CaptureQuickFilterTokens() {
    }

    static List<String> parse(String rawValue) {
        List<String> tokens = new ArrayList<>();
        if (rawValue == null || rawValue.isBlank()) {
            return tokens;
        }
        for (String token : rawValue.trim().split("[,;\\s\\r\\n]+")) {
            if (token != null && !token.isBlank()) {
                tokens.add(token.trim());
            }
        }
        return tokens;
    }

    static boolean hasIncludedToken(List<String> tokens, String canonicalToken) {
        if (tokens == null || canonicalToken == null || canonicalToken.isBlank()) {
            return false;
        }
        for (String token : tokens) {
            if (!isNegated(token) && canonicalToken.equals(canonicalize(token))) {
                return true;
            }
        }
        return false;
    }

    static void removeToken(List<String> tokens, String canonicalToken, boolean includeNegated) {
        if (tokens == null || canonicalToken == null || canonicalToken.isBlank()) {
            return;
        }
        tokens.removeIf(token -> canonicalToken.equals(canonicalize(token))
                && (includeNegated || !isNegated(token)));
    }

    private static boolean isNegated(String token) {
        return token != null && token.trim().startsWith("!");
    }

    private static String canonicalize(String token) {
        if (token == null) {
            return "";
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("!")) {
            normalized = normalized.substring(1).trim();
        }
        return switch (normalized) {
            case "http", "scheme:http" -> "http";
            case "https", "scheme:https" -> "https";
            case "json", "type:json" -> "json";
            case "image", "img", "type:image", "type:img" -> "image";
            case "js", "type:js" -> "js";
            case "css", "type:css" -> "css";
            case "api", "type:api" -> "api";
            default -> normalized;
        };
    }
}
