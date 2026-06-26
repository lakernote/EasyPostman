package com.laker.postman.plugin.capture;

import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
class CaptureFilterExpressions {

    static String onlyHost(String host) {
        String normalizedHost = normalizeHost(host);
        return normalizedHost.isBlank() ? "" : "host:" + normalizedHost;
    }

    static String excludeHost(String currentFilter, String host) {
        String hostToken = onlyHost(host);
        if (hostToken.isBlank()) {
            return normalizeFilter(currentFilter);
        }
        return appendConjunctiveToken(currentFilter, "!" + hostToken);
    }

    static String onlyRequest(String method, String host, String path) {
        String methodToken = onlyMethod(method);
        String hostToken = onlyHost(host);
        String pathToken = onlyPath(path);
        return joinTokens(methodToken, hostToken, pathToken);
    }

    static String onlyMethod(String method) {
        String normalizedMethod = normalizeMethod(method);
        return normalizedMethod.isBlank() ? "" : "method:" + normalizedMethod;
    }

    static String onlyPid(String pid) {
        String normalizedPid = normalizeTokenValue(pid);
        return normalizedPid.isBlank() ? "" : "pid:" + normalizedPid;
    }

    static String excludePid(String currentFilter, String pid) {
        String pidToken = onlyPid(pid);
        if (pidToken.isBlank()) {
            return normalizeFilter(currentFilter);
        }
        return appendConjunctiveToken(currentFilter, "!" + pidToken);
    }

    static String onlyProcess(String processName) {
        String normalizedProcessName = normalizeTokenValue(processName);
        return normalizedProcessName.isBlank() ? "" : "process:" + quoteIfNeeded(normalizedProcessName);
    }

    static String excludeProcess(String currentFilter, String processName) {
        String processToken = onlyProcess(processName);
        if (processToken.isBlank()) {
            return normalizeFilter(currentFilter);
        }
        return appendConjunctiveToken(currentFilter, "!" + processToken);
    }

    static String onlyPath(String path) {
        String normalizedPath = normalizePath(path);
        return normalizedPath.isBlank() ? "" : "path:" + normalizedPath;
    }

    static String excludePath(String currentFilter, String path) {
        String pathToken = onlyPath(path);
        if (pathToken.isBlank()) {
            return normalizeFilter(currentFilter);
        }
        return appendConjunctiveToken(currentFilter, "!" + pathToken);
    }

    private static String joinTokens(String... tokens) {
        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (token == null || token.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(token);
        }
        return builder.toString();
    }

    private static String appendConjunctiveToken(String currentFilter, String token) {
        String normalizedFilter = normalizeFilter(currentFilter);
        if (normalizedFilter.isBlank()) {
            return token;
        }
        if (containsToken(normalizedFilter, token)) {
            return normalizedFilter;
        }
        String leftExpression = needsWrapping(normalizedFilter) ? "(" + normalizedFilter + ")" : normalizedFilter;
        return leftExpression + " " + token;
    }

    private static boolean containsToken(String filter, String token) {
        for (String existingToken : CaptureQuickFilterTokens.parse(filter)) {
            if (token.equalsIgnoreCase(existingToken)) {
                return true;
            }
        }
        return false;
    }

    private static boolean needsWrapping(String filter) {
        String normalized = filter.toLowerCase(Locale.ROOT);
        return containsOperator(normalized, "or")
                && !(filter.startsWith("(") && filter.endsWith(")"));
    }

    private static boolean containsOperator(String filter, String operator) {
        return (" " + filter + " ").contains(" " + operator + " ");
    }

    private static String normalizeFilter(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeTokenValue(String value) {
        return value == null ? "" : value.trim()
                .replace('"', ' ')
                .replace('\'', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String quoteIfNeeded(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.indexOf(' ') >= 0 || value.indexOf(',') >= 0 || value.indexOf(';') >= 0) {
            return "\"" + value + "\"";
        }
        return value;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (normalized.isBlank()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim();
        int schemeIndex = normalized.indexOf("://");
        if (schemeIndex >= 0) {
            normalized = normalized.substring(schemeIndex + 3);
        }
        int slashIndex = normalized.indexOf('/');
        if (slashIndex >= 0) {
            normalized = normalized.substring(0, slashIndex);
        }
        int colonIndex = normalized.lastIndexOf(':');
        if (colonIndex > 0 && normalized.indexOf(':') == colonIndex) {
            normalized = normalized.substring(0, colonIndex);
        }
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
