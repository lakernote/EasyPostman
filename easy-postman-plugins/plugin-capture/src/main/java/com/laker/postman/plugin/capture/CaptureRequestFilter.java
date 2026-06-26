package com.laker.postman.plugin.capture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

final class CaptureRequestFilter {
    private final Expression expression;
    private final List<Rule> includeHostRules;
    private final List<Rule> excludeHostRules;
    private final List<Rule> includeMethodRules;
    private final List<Rule> excludeMethodRules;
    private final List<Rule> includePathRules;
    private final List<Rule> excludePathRules;
    private final List<Rule> includeQueryRules;
    private final List<Rule> excludeQueryRules;
    private final List<Rule> includeUrlRules;
    private final List<Rule> excludeUrlRules;
    private final List<Rule> includeSchemeRules;
    private final List<Rule> excludeSchemeRules;
    private final List<Rule> includeTypeRules;
    private final List<Rule> excludeTypeRules;
    private final List<Rule> includeRegexRules;
    private final List<Rule> excludeRegexRules;
    private final List<Rule> includeSourceRules;
    private final List<Rule> excludeSourceRules;
    private final List<Rule> includeClientRules;
    private final List<Rule> excludeClientRules;
    private final List<Rule> includeSourcePortRules;
    private final List<Rule> excludeSourcePortRules;
    private final List<Rule> includePidRules;
    private final List<Rule> excludePidRules;
    private final List<Rule> includeProcessRules;
    private final List<Rule> excludeProcessRules;
    private final List<Rule> includeSourcePathRules;
    private final List<Rule> excludeSourcePathRules;
    private final List<String> summaryTokens;

    private CaptureRequestFilter(Expression expression,
                                 List<Rule> includeHostRules,
                                 List<Rule> excludeHostRules,
                                 List<Rule> includeMethodRules,
                                 List<Rule> excludeMethodRules,
                                 List<Rule> includePathRules,
                                 List<Rule> excludePathRules,
                                 List<Rule> includeQueryRules,
                                 List<Rule> excludeQueryRules,
                                 List<Rule> includeUrlRules,
                                 List<Rule> excludeUrlRules,
                                 List<Rule> includeSchemeRules,
                                 List<Rule> excludeSchemeRules,
                                 List<Rule> includeTypeRules,
                                 List<Rule> excludeTypeRules,
                                 List<Rule> includeRegexRules,
                                 List<Rule> excludeRegexRules,
                                 List<Rule> includeSourceRules,
                                 List<Rule> excludeSourceRules,
                                 List<Rule> includeClientRules,
                                 List<Rule> excludeClientRules,
                                 List<Rule> includeSourcePortRules,
                                 List<Rule> excludeSourcePortRules,
                                 List<Rule> includePidRules,
                                 List<Rule> excludePidRules,
                                 List<Rule> includeProcessRules,
                                 List<Rule> excludeProcessRules,
                                 List<Rule> includeSourcePathRules,
                                 List<Rule> excludeSourcePathRules,
                                 List<String> summaryTokens) {
        this.expression = expression;
        this.includeHostRules = List.copyOf(includeHostRules);
        this.excludeHostRules = List.copyOf(excludeHostRules);
        this.includeMethodRules = List.copyOf(includeMethodRules);
        this.excludeMethodRules = List.copyOf(excludeMethodRules);
        this.includePathRules = List.copyOf(includePathRules);
        this.excludePathRules = List.copyOf(excludePathRules);
        this.includeQueryRules = List.copyOf(includeQueryRules);
        this.excludeQueryRules = List.copyOf(excludeQueryRules);
        this.includeUrlRules = List.copyOf(includeUrlRules);
        this.excludeUrlRules = List.copyOf(excludeUrlRules);
        this.includeSchemeRules = List.copyOf(includeSchemeRules);
        this.excludeSchemeRules = List.copyOf(excludeSchemeRules);
        this.includeTypeRules = List.copyOf(includeTypeRules);
        this.excludeTypeRules = List.copyOf(excludeTypeRules);
        this.includeRegexRules = List.copyOf(includeRegexRules);
        this.excludeRegexRules = List.copyOf(excludeRegexRules);
        this.includeSourceRules = List.copyOf(includeSourceRules);
        this.excludeSourceRules = List.copyOf(excludeSourceRules);
        this.includeClientRules = List.copyOf(includeClientRules);
        this.excludeClientRules = List.copyOf(excludeClientRules);
        this.includeSourcePortRules = List.copyOf(includeSourcePortRules);
        this.excludeSourcePortRules = List.copyOf(excludeSourcePortRules);
        this.includePidRules = List.copyOf(includePidRules);
        this.excludePidRules = List.copyOf(excludePidRules);
        this.includeProcessRules = List.copyOf(includeProcessRules);
        this.excludeProcessRules = List.copyOf(excludeProcessRules);
        this.includeSourcePathRules = List.copyOf(includeSourcePathRules);
        this.excludeSourcePathRules = List.copyOf(excludeSourcePathRules);
        this.summaryTokens = List.copyOf(summaryTokens);
    }

    static CaptureRequestFilter parse(String rawValue) {
        String normalizedRaw = rawValue == null ? "" : rawValue.trim();
        Expression expression = null;
        List<Rule> includeHostRules = new ArrayList<>();
        List<Rule> excludeHostRules = new ArrayList<>();
        List<Rule> includeMethodRules = new ArrayList<>();
        List<Rule> excludeMethodRules = new ArrayList<>();
        List<Rule> includePathRules = new ArrayList<>();
        List<Rule> excludePathRules = new ArrayList<>();
        List<Rule> includeQueryRules = new ArrayList<>();
        List<Rule> excludeQueryRules = new ArrayList<>();
        List<Rule> includeUrlRules = new ArrayList<>();
        List<Rule> excludeUrlRules = new ArrayList<>();
        List<Rule> includeSchemeRules = new ArrayList<>();
        List<Rule> excludeSchemeRules = new ArrayList<>();
        List<Rule> includeTypeRules = new ArrayList<>();
        List<Rule> excludeTypeRules = new ArrayList<>();
        List<Rule> includeRegexRules = new ArrayList<>();
        List<Rule> excludeRegexRules = new ArrayList<>();
        List<Rule> includeSourceRules = new ArrayList<>();
        List<Rule> excludeSourceRules = new ArrayList<>();
        List<Rule> includeClientRules = new ArrayList<>();
        List<Rule> excludeClientRules = new ArrayList<>();
        List<Rule> includeSourcePortRules = new ArrayList<>();
        List<Rule> excludeSourcePortRules = new ArrayList<>();
        List<Rule> includePidRules = new ArrayList<>();
        List<Rule> excludePidRules = new ArrayList<>();
        List<Rule> includeProcessRules = new ArrayList<>();
        List<Rule> excludeProcessRules = new ArrayList<>();
        List<Rule> includeSourcePathRules = new ArrayList<>();
        List<Rule> excludeSourcePathRules = new ArrayList<>();
        List<String> summaryTokens = new ArrayList<>();

        if (!normalizedRaw.isEmpty()) {
            List<String> tokens = tokenize(normalizedRaw);
            if (containsExpressionSyntax(tokens)) {
                expression = new ExpressionParser(tokens).parse();
                summaryTokens.add(normalizedRaw);
            } else {
                for (String token : tokens) {
                String trimmed = token == null ? "" : token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Rule rule = Rule.parse(trimmed);
                if (rule == null) {
                    continue;
                }
                summaryTokens.add(rule.summaryToken());
                switch (rule.type()) {
                    case HOST -> (rule.exclude() ? excludeHostRules : includeHostRules).add(rule);
                    case METHOD -> (rule.exclude() ? excludeMethodRules : includeMethodRules).add(rule);
                    case PATH -> (rule.exclude() ? excludePathRules : includePathRules).add(rule);
                    case QUERY -> (rule.exclude() ? excludeQueryRules : includeQueryRules).add(rule);
                    case URL -> (rule.exclude() ? excludeUrlRules : includeUrlRules).add(rule);
                    case SCHEME -> (rule.exclude() ? excludeSchemeRules : includeSchemeRules).add(rule);
                    case TYPE -> (rule.exclude() ? excludeTypeRules : includeTypeRules).add(rule);
                    case REGEX -> (rule.exclude() ? excludeRegexRules : includeRegexRules).add(rule);
                    case SOURCE -> (rule.exclude() ? excludeSourceRules : includeSourceRules).add(rule);
                    case CLIENT -> (rule.exclude() ? excludeClientRules : includeClientRules).add(rule);
                    case SOURCE_PORT -> (rule.exclude() ? excludeSourcePortRules : includeSourcePortRules).add(rule);
                    case PID -> (rule.exclude() ? excludePidRules : includePidRules).add(rule);
                    case PROCESS -> (rule.exclude() ? excludeProcessRules : includeProcessRules).add(rule);
                    case SOURCE_PATH -> (rule.exclude() ? excludeSourcePathRules : includeSourcePathRules).add(rule);
                }
            }
            }
        }

        return new CaptureRequestFilter(
                expression,
                includeHostRules,
                excludeHostRules,
                includeMethodRules,
                excludeMethodRules,
                includePathRules,
                excludePathRules,
                includeQueryRules,
                excludeQueryRules,
                includeUrlRules,
                excludeUrlRules,
                includeSchemeRules,
                excludeSchemeRules,
                includeTypeRules,
                excludeTypeRules,
                includeRegexRules,
                excludeRegexRules,
                includeSourceRules,
                excludeSourceRules,
                includeClientRules,
                excludeClientRules,
                includeSourcePortRules,
                excludeSourcePortRules,
                includePidRules,
                excludePidRules,
                includeProcessRules,
                excludeProcessRules,
                includeSourcePathRules,
                excludeSourcePathRules,
                summaryTokens
        );
    }

    boolean matches(String host, String requestUri, String fullUrl, Map<String, String> headers) {
        return matches("", host, requestUri, fullUrl, headers, CaptureSourceInfo.unknown());
    }

    boolean matches(String method, String host, String requestUri, String fullUrl, Map<String, String> headers) {
        return matches(method, host, requestUri, fullUrl, headers, CaptureSourceInfo.unknown());
    }

    boolean matches(String method,
                    String host,
                    String requestUri,
                    String fullUrl,
                    Map<String, String> headers,
                    CaptureSourceInfo sourceInfo) {
        RequestParts parts = RequestParts.from(method, host, requestUri, fullUrl, headers, sourceInfo);
        if (expression != null) {
            return expression.matches(parts);
        }
        if (matchesAny(excludeHostRules, parts.host())
                || matchesAny(excludeMethodRules, parts.method())
                || matchesAny(excludePathRules, parts.path())
                || matchesAny(excludeQueryRules, parts.query())
                || matchesAny(excludeUrlRules, parts.fullUrl())
                || matchesAny(excludeSchemeRules, parts.scheme())
                || matchesAny(excludeTypeRules, parts.resourceType())
                || matchesAny(excludeRegexRules, parts.fullUrl())
                || matchesAny(excludeSourceRules, parts.sourceParts().source())
                || matchesAny(excludeClientRules, parts.sourceParts().client())
                || matchesAny(excludeSourcePortRules, parts.sourceParts().sourcePort())
                || matchesAny(excludePidRules, parts.sourceParts().pid())
                || matchesAny(excludeProcessRules, parts.sourceParts().process())
                || matchesAny(excludeSourcePathRules, parts.sourceParts().sourcePath())) {
            return false;
        }
        return matchesDimension(includeHostRules, parts.host())
                && matchesDimension(includeMethodRules, parts.method())
                && matchesDimension(includePathRules, parts.path())
                && matchesDimension(includeQueryRules, parts.query())
                && matchesDimension(includeUrlRules, parts.fullUrl())
                && matchesDimension(includeSchemeRules, parts.scheme())
                && matchesDimension(includeTypeRules, parts.resourceType())
                && matchesDimension(includeRegexRules, parts.fullUrl())
                && matchesDimension(includeSourceRules, parts.sourceParts().source())
                && matchesDimension(includeClientRules, parts.sourceParts().client())
                && matchesDimension(includeSourcePortRules, parts.sourceParts().sourcePort())
                && matchesDimension(includePidRules, parts.sourceParts().pid())
                && matchesDimension(includeProcessRules, parts.sourceParts().process())
                && matchesDimension(includeSourcePathRules, parts.sourceParts().sourcePath());
    }

    boolean shouldMitmHost(String host) {
        return shouldMitmHost(host, CaptureSourceInfo.unknown());
    }

    boolean shouldMitmHost(String host, CaptureSourceInfo sourceInfo) {
        String normalizedHost = normalizeHost(host);
        SourceParts sourceParts = SourceParts.from(sourceInfo);
        if (expression != null) {
            return expression.mayMatchPreRequest(normalizedHost, sourceParts) != TriState.FALSE;
        }
        if (matchesAny(excludeHostRules, normalizedHost)
                || matchesAny(excludeSourceRules, sourceParts.source())
                || matchesAny(excludeClientRules, sourceParts.client())
                || matchesAny(excludeSourcePortRules, sourceParts.sourcePort())
                || matchesAny(excludePidRules, sourceParts.pid())
                || matchesAny(excludeProcessRules, sourceParts.process())
                || matchesAny(excludeSourcePathRules, sourceParts.sourcePath())) {
            return false;
        }
        if (!includeHostRules.isEmpty() && !matchesAny(includeHostRules, normalizedHost)) {
            return false;
        }
        return matchesDimension(includeSourceRules, sourceParts.source())
                && matchesDimension(includeClientRules, sourceParts.client())
                && matchesDimension(includeSourcePortRules, sourceParts.sourcePort())
                && matchesDimension(includePidRules, sourceParts.pid())
                && matchesDimension(includeProcessRules, sourceParts.process())
                && matchesDimension(includeSourcePathRules, sourceParts.sourcePath());
    }

    boolean requiresResolvedSource() {
        if (expression != null) {
            return expression.requiresResolvedSource();
        }
        return !includeSourceRules.isEmpty()
                || !excludeSourceRules.isEmpty()
                || !includePidRules.isEmpty()
                || !excludePidRules.isEmpty()
                || !includeProcessRules.isEmpty()
                || !excludeProcessRules.isEmpty()
                || !includeSourcePathRules.isEmpty()
                || !excludeSourcePathRules.isEmpty();
    }

    String summary() {
        if (summaryTokens.isEmpty()) {
            return t(MessageKeys.TOOLBOX_CAPTURE_FILTER_ALL);
        }
        return t(MessageKeys.TOOLBOX_CAPTURE_FILTER_RULES, String.join(", ", summaryTokens));
    }

    private boolean matchesDimension(List<Rule> includeRules, String candidate) {
        return includeRules.isEmpty() || matchesAny(includeRules, candidate);
    }

    private boolean matchesAny(List<Rule> rules, String candidate) {
        for (Rule rule : rules) {
            if (rule.matches(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeHost(String host) {
        if (host == null) {
            return "";
        }
        String normalized = host.trim().toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static List<String> tokenize(String rawValue) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quoteChar = 0;
        for (int i = 0; i < rawValue.length(); i++) {
            char ch = rawValue.charAt(i);
            if (quoted) {
                if (ch == quoteChar) {
                    quoted = false;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '"' || ch == '\'') {
                quoted = true;
                quoteChar = ch;
                continue;
            }
            if (Character.isWhitespace(ch) || ch == ',' || ch == ';') {
                flushToken(tokens, current);
                continue;
            }
            if (ch == '(' || ch == ')') {
                if (isRegexToken(current)) {
                    current.append(ch);
                    continue;
                }
                flushToken(tokens, current);
                tokens.add(String.valueOf(ch));
                continue;
            }
            current.append(ch);
        }
        flushToken(tokens, current);
        return tokens;
    }

    private static boolean isRegexToken(StringBuilder current) {
        if (current == null || current.isEmpty()) {
            return false;
        }
        String token = current.toString().toLowerCase(Locale.ROOT);
        return token.startsWith("regex:") || token.startsWith("!regex:");
    }

    private static void flushToken(List<String> tokens, StringBuilder current) {
        if (current.length() == 0) {
            return;
        }
        tokens.add(current.toString());
        current.setLength(0);
    }

    private static boolean containsExpressionSyntax(List<String> tokens) {
        for (String token : tokens) {
            if ("(".equals(token) || ")".equals(token)
                    || "or".equalsIgnoreCase(token)
                    || "and".equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    private record RequestParts(String method,
                                String host,
                                String path,
                                String query,
                                String fullUrl,
                                String scheme,
                                String resourceType,
                                SourceParts sourceParts) {
        private static RequestParts from(String method,
                                         String host,
                                         String requestUri,
                                         String fullUrl,
                                         Map<String, String> headers,
                                         CaptureSourceInfo sourceInfo) {
            String normalizedHost = normalizeHost(host);
            String normalizedMethod = method == null ? "" : method.trim().toLowerCase(Locale.ROOT);
            String uri = requestUri == null || requestUri.isBlank() ? "/" : requestUri;
            int queryIndex = uri.indexOf('?');
            String path = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
            String query = queryIndex >= 0 && queryIndex < uri.length() - 1 ? uri.substring(queryIndex + 1) : "";
            if (path.isBlank()) {
                path = "/";
            }
            String normalizedFullUrl = (fullUrl == null ? "" : fullUrl).toLowerCase(Locale.ROOT);
            return new RequestParts(
                    normalizedMethod,
                    normalizedHost,
                    path.toLowerCase(Locale.ROOT),
                    query.toLowerCase(Locale.ROOT),
                    normalizedFullUrl,
                    detectScheme(normalizedFullUrl),
                    detectResourceType(path, headers),
                    SourceParts.from(sourceInfo)
            );
        }

        private static String detectScheme(String fullUrl) {
            if (fullUrl.startsWith("https://")) {
                return "https";
            }
            if (fullUrl.startsWith("http://")) {
                return "http";
            }
            if (fullUrl.startsWith("wss://")) {
                return "https";
            }
            if (fullUrl.startsWith("ws://")) {
                return "http";
            }
            return "";
        }

        private static String detectResourceType(String path, Map<String, String> headers) {
            String lowerPath = path == null ? "" : path.toLowerCase(Locale.ROOT);
            Map<String, String> normalizedHeaders = normalizeHeaders(headers);
            String accept = normalizedHeaders.getOrDefault("accept", "");
            String contentType = normalizedHeaders.getOrDefault("content-type", "");
            String fetchDest = normalizedHeaders.getOrDefault("sec-fetch-dest", "");
            String upgrade = normalizedHeaders.getOrDefault("upgrade", "");
            String requestedWith = normalizedHeaders.getOrDefault("x-requested-with", "");

            if ("websocket".equalsIgnoreCase(upgrade)) {
                return "websocket";
            }
            if (accept.contains("text/event-stream") || contentType.contains("text/event-stream")) {
                return "sse";
            }
            if (matchesAny(fetchDest, "image") || hasExtension(lowerPath, ".png", ".jpg", ".jpeg", ".gif", ".webp", ".svg", ".ico", ".bmp", ".avif", ".tif", ".tiff")) {
                return "image";
            }
            if (matchesAny(fetchDest, "style") || hasExtension(lowerPath, ".css") || accept.contains("text/css")) {
                return "css";
            }
            if (matchesAny(fetchDest, "script") || hasExtension(lowerPath, ".js", ".mjs", ".cjs") || accept.contains("javascript")) {
                return "js";
            }
            if (matchesAny(fetchDest, "font") || hasExtension(lowerPath, ".woff", ".woff2", ".ttf", ".otf", ".eot")) {
                return "font";
            }
            if (matchesAny(fetchDest, "audio", "video") || hasExtension(lowerPath, ".mp4", ".mp3", ".wav", ".webm", ".ogg", ".m4a", ".mov", ".avi", ".mkv")) {
                return "media";
            }
            if (contentType.contains("application/json") || accept.contains("application/json") || hasExtension(lowerPath, ".json")) {
                return "json";
            }
            if ("xmlhttprequest".equals(requestedWith) || lowerPath.contains("/api/") || lowerPath.startsWith("/api")) {
                return "api";
            }
            if (matchesAny(fetchDest, "document", "iframe", "frame") || hasExtension(lowerPath, ".html", ".htm") || accept.contains("text/html")) {
                return "html";
            }
            return "other";
        }

        private static Map<String, String> normalizeHeaders(Map<String, String> headers) {
            Map<String, String> normalized = new LinkedHashMap<>();
            if (headers == null) {
                return normalized;
            }
            headers.forEach((key, value) -> normalized.put(
                    key == null ? "" : key.toLowerCase(Locale.ROOT),
                    value == null ? "" : value.toLowerCase(Locale.ROOT)));
            return normalized;
        }

        private static boolean matchesAny(String value, String... candidates) {
            for (String candidate : candidates) {
                if (candidate.equalsIgnoreCase(value)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean hasExtension(String path, String... extensions) {
            int queryIndex = path.indexOf('?');
            String normalizedPath = queryIndex >= 0 ? path.substring(0, queryIndex) : path;
            for (String extension : extensions) {
                if (normalizedPath.endsWith(extension)) {
                    return true;
                }
            }
            return false;
        }
    }

    private record SourceParts(String source,
                               String client,
                               String sourcePort,
                               String pid,
                               String process,
                               String sourcePath) {
        private static SourceParts from(CaptureSourceInfo sourceInfo) {
            CaptureSourceInfo safe = sourceInfo == null ? CaptureSourceInfo.unknown() : sourceInfo;
            String clientEndpoint = normalizeText(safe.clientEndpoint());
            String proxyEndpoint = normalizeText(safe.proxyEndpoint());
            String processId = normalizeText(safe.processId());
            String processName = normalizeText(safe.processName());
            String processPath = normalizeText(safe.processPath());
            String combined = String.join(" ",
                    clientEndpoint,
                    proxyEndpoint,
                    processId,
                    processName,
                    processPath).trim();
            String client = String.join(" ", normalizeText(safe.clientHost()), clientEndpoint).trim();
            return new SourceParts(
                    combined,
                    client,
                    safe.clientPort() > 0 ? String.valueOf(safe.clientPort()) : "",
                    processId,
                    processName,
                    processPath
            );
        }

        private static String normalizeText(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private enum TriState {
        TRUE,
        FALSE,
        UNKNOWN;

        static TriState and(TriState left, TriState right) {
            if (left == FALSE || right == FALSE) {
                return FALSE;
            }
            if (left == TRUE && right == TRUE) {
                return TRUE;
            }
            return UNKNOWN;
        }

        static TriState or(TriState left, TriState right) {
            if (left == TRUE || right == TRUE) {
                return TRUE;
            }
            if (left == FALSE && right == FALSE) {
                return FALSE;
            }
            return UNKNOWN;
        }
    }

    private interface Expression {
        boolean matches(RequestParts parts);

        TriState mayMatchPreRequest(String normalizedHost, SourceParts sourceParts);

        boolean requiresResolvedSource();
    }

    private record RuleExpression(Rule rule) implements Expression {
        @Override
        public boolean matches(RequestParts parts) {
            boolean matched = switch (rule.type()) {
                case HOST -> rule.matches(parts.host());
                case METHOD -> rule.matches(parts.method());
                case PATH -> rule.matches(parts.path());
                case QUERY -> rule.matches(parts.query());
                case URL -> rule.matches(parts.fullUrl());
                case SCHEME -> rule.matches(parts.scheme());
                case TYPE -> rule.matches(parts.resourceType());
                case REGEX -> rule.matches(parts.fullUrl());
                case SOURCE -> rule.matches(parts.sourceParts().source());
                case CLIENT -> rule.matches(parts.sourceParts().client());
                case SOURCE_PORT -> rule.matches(parts.sourceParts().sourcePort());
                case PID -> rule.matches(parts.sourceParts().pid());
                case PROCESS -> rule.matches(parts.sourceParts().process());
                case SOURCE_PATH -> rule.matches(parts.sourceParts().sourcePath());
            };
            return rule.exclude() ? !matched : matched;
        }

        @Override
        public TriState mayMatchPreRequest(String normalizedHost, SourceParts sourceParts) {
            String candidate = switch (rule.type()) {
                case HOST -> normalizedHost;
                case SOURCE -> sourceParts.source();
                case CLIENT -> sourceParts.client();
                case SOURCE_PORT -> sourceParts.sourcePort();
                case PID -> sourceParts.pid();
                case PROCESS -> sourceParts.process();
                case SOURCE_PATH -> sourceParts.sourcePath();
                default -> null;
            };
            if (candidate == null) {
                return TriState.UNKNOWN;
            }
            boolean matched = rule.matches(candidate);
            if (rule.exclude()) {
                return matched ? TriState.FALSE : TriState.TRUE;
            }
            return matched ? TriState.TRUE : TriState.FALSE;
        }

        @Override
        public boolean requiresResolvedSource() {
            return rule.type().requiresResolvedSource();
        }
    }

    private record AndExpression(List<Expression> operands) implements Expression {
        @Override
        public boolean matches(RequestParts parts) {
            for (Expression operand : operands) {
                if (!operand.matches(parts)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public TriState mayMatchPreRequest(String normalizedHost, SourceParts sourceParts) {
            TriState result = TriState.TRUE;
            for (Expression operand : operands) {
                result = TriState.and(result, operand.mayMatchPreRequest(normalizedHost, sourceParts));
                if (result == TriState.FALSE) {
                    return TriState.FALSE;
                }
            }
            return result;
        }

        @Override
        public boolean requiresResolvedSource() {
            for (Expression operand : operands) {
                if (operand.requiresResolvedSource()) {
                    return true;
                }
            }
            return false;
        }
    }

    private record OrExpression(List<Expression> operands) implements Expression {
        @Override
        public boolean matches(RequestParts parts) {
            for (Expression operand : operands) {
                if (operand.matches(parts)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public TriState mayMatchPreRequest(String normalizedHost, SourceParts sourceParts) {
            TriState result = TriState.FALSE;
            for (Expression operand : operands) {
                result = TriState.or(result, operand.mayMatchPreRequest(normalizedHost, sourceParts));
                if (result == TriState.TRUE) {
                    return TriState.TRUE;
                }
            }
            return result;
        }

        @Override
        public boolean requiresResolvedSource() {
            for (Expression operand : operands) {
                if (operand.requiresResolvedSource()) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class ExpressionParser {
        private final List<String> tokens;
        private int index;

        private ExpressionParser(List<String> tokens) {
            this.tokens = tokens;
        }

        private Expression parse() {
            Expression expression = parseOrExpression();
            if (index < tokens.size()) {
                throw new IllegalArgumentException("Unexpected token: " + tokens.get(index));
            }
            return expression;
        }

        private Expression parseOrExpression() {
            List<Expression> operands = new ArrayList<>();
            operands.add(parseAndExpression());
            while (matchOperator("or")) {
                operands.add(parseAndExpression());
            }
            return operands.size() == 1 ? operands.get(0) : new OrExpression(List.copyOf(operands));
        }

        private Expression parseAndExpression() {
            List<Expression> operands = new ArrayList<>();
            operands.add(parsePrimary());
            while (true) {
                if (matchOperator("and")) {
                    operands.add(parsePrimary());
                    continue;
                }
                String next = peek();
                if (next == null || ")".equals(next) || "or".equalsIgnoreCase(next)) {
                    break;
                }
                operands.add(parsePrimary());
            }
            return operands.size() == 1 ? operands.get(0) : new AndExpression(List.copyOf(operands));
        }

        private Expression parsePrimary() {
            String token = peek();
            if (token == null) {
                throw new IllegalArgumentException("Unexpected end of filter expression");
            }
            if ("(".equals(token)) {
                index++;
                Expression expression = parseOrExpression();
                expect(")");
                return expression;
            }
            if (")".equals(token) || "and".equalsIgnoreCase(token) || "or".equalsIgnoreCase(token)) {
                throw new IllegalArgumentException("Unexpected token: " + token);
            }
            index++;
            Rule rule = Rule.parse(token);
            if (rule == null) {
                throw new IllegalArgumentException("Invalid filter token: " + token);
            }
            return new RuleExpression(rule);
        }

        private boolean matchOperator(String operator) {
            String token = peek();
            if (token != null && operator.equalsIgnoreCase(token)) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(String expectedToken) {
            String token = peek();
            if (!expectedToken.equals(token)) {
                throw new IllegalArgumentException("Expected " + expectedToken + " but found " + token);
            }
            index++;
        }

        private String peek() {
            return index < tokens.size() ? tokens.get(index) : null;
        }
    }

    private enum RuleType {
        HOST,
        METHOD,
        PATH,
        QUERY,
        URL,
        SCHEME,
        TYPE,
        REGEX,
        SOURCE,
        CLIENT,
        SOURCE_PORT,
        PID,
        PROCESS,
        SOURCE_PATH;

        private boolean requiresResolvedSource() {
            return this == SOURCE || this == PID || this == PROCESS || this == SOURCE_PATH;
        }
    }

    private static final Set<String> QUICK_SCHEME_ALIASES = Set.of("http", "https");
    private static final Set<String> QUICK_TYPE_ALIASES = Set.of(
            "image", "img", "json", "html", "js", "css", "font", "media", "api", "sse", "ws", "websocket");
    private static final Map<String, String> CANONICAL_TYPE_ALIASES = Map.of(
            "img", "image",
            "ws", "websocket"
    );

    private record Rule(boolean exclude, RuleType type, String value, Pattern regexPattern, Pattern wildcardPattern) {
        private static Rule parse(String token) {
            boolean exclude = token.startsWith("!");
            String normalized = exclude ? token.substring(1).trim() : token.trim();
            if (normalized.isEmpty()) {
                return null;
            }

            RuleType type = RuleType.HOST;
            String value = normalized;
            String lower = normalized.toLowerCase(Locale.ROOT);
            if (lower.startsWith("host:")) {
                type = RuleType.HOST;
                value = normalized.substring(5);
            } else if (lower.startsWith("method:")) {
                type = RuleType.METHOD;
                value = normalized.substring(7);
            } else if (lower.startsWith("path:")) {
                type = RuleType.PATH;
                value = normalized.substring(5);
            } else if (lower.startsWith("query:")) {
                type = RuleType.QUERY;
                value = normalized.substring(6);
            } else if (lower.startsWith("url:")) {
                type = RuleType.URL;
                value = normalized.substring(4);
            } else if (lower.startsWith("scheme:")) {
                type = RuleType.SCHEME;
                value = normalized.substring(7);
            } else if (lower.startsWith("type:")) {
                type = RuleType.TYPE;
                value = normalized.substring(5);
            } else if (lower.startsWith("regex:")) {
                type = RuleType.REGEX;
                value = normalized.substring(6);
            } else if (lower.startsWith("source:")) {
                type = RuleType.SOURCE;
                value = normalized.substring(7);
            } else if (lower.startsWith("client:")) {
                type = RuleType.CLIENT;
                value = normalized.substring(7);
            } else if (lower.startsWith("sourceport:")) {
                type = RuleType.SOURCE_PORT;
                value = normalized.substring(11);
            } else if (lower.startsWith("clientport:")) {
                type = RuleType.SOURCE_PORT;
                value = normalized.substring(11);
            } else if (lower.startsWith("pid:")) {
                type = RuleType.PID;
                value = normalized.substring(4);
            } else if (lower.startsWith("process:")) {
                type = RuleType.PROCESS;
                value = normalized.substring(8);
            } else if (lower.startsWith("sourcepath:")) {
                type = RuleType.SOURCE_PATH;
                value = normalized.substring(11);
            } else if (lower.startsWith("processpath:")) {
                type = RuleType.SOURCE_PATH;
                value = normalized.substring(12);
            } else if (QUICK_SCHEME_ALIASES.contains(lower)) {
                type = RuleType.SCHEME;
                value = lower;
            } else if (QUICK_TYPE_ALIASES.contains(lower)) {
                type = RuleType.TYPE;
                value = CANONICAL_TYPE_ALIASES.getOrDefault(lower, lower);
            }

            String cleanedValue = value == null ? "" : value.trim();
            if (cleanedValue.isEmpty()) {
                return null;
            }
            if (type == RuleType.METHOD) {
                cleanedValue = cleanedValue.toUpperCase(Locale.ROOT);
            } else if (type == RuleType.SCHEME || type == RuleType.TYPE) {
                cleanedValue = cleanedValue.toLowerCase(Locale.ROOT);
            }

            Pattern regexPattern = null;
            Pattern wildcardPattern = null;
            if (type == RuleType.REGEX) {
                regexPattern = Pattern.compile(cleanedValue, Pattern.CASE_INSENSITIVE);
            } else if (cleanedValue.contains("*")) {
                wildcardPattern = Pattern.compile(globToRegex(cleanedValue), Pattern.CASE_INSENSITIVE);
            }

            return new Rule(exclude, type, cleanedValue, regexPattern, wildcardPattern);
        }

        private String summaryToken() {
            String prefix = switch (type) {
                case HOST -> "host:";
                case METHOD -> "method:";
                case PATH -> "path:";
                case QUERY -> "query:";
                case URL -> "url:";
                case SCHEME -> "scheme:";
                case TYPE -> "type:";
                case REGEX -> "regex:";
                case SOURCE -> "source:";
                case CLIENT -> "client:";
                case SOURCE_PORT -> "sourcePort:";
                case PID -> "pid:";
                case PROCESS -> "process:";
                case SOURCE_PATH -> "sourcePath:";
            };
            if ((type == RuleType.HOST && !value.contains("*") && !value.contains(":"))
                    || type == RuleType.METHOD
                    || type == RuleType.SCHEME
                    || type == RuleType.TYPE) {
                return (exclude ? "!" : "") + value;
            }
            return (exclude ? "!" : "") + prefix + value;
        }

        private boolean matches(String candidate) {
            if (candidate == null) {
                return false;
            }
            return switch (type) {
                case HOST -> matchesHost(candidate);
                case METHOD, PATH, QUERY, URL, SCHEME, TYPE, SOURCE, CLIENT, SOURCE_PORT, PID, PROCESS, SOURCE_PATH -> matchesText(candidate);
                case REGEX -> regexPattern != null && regexPattern.matcher(candidate).find();
            };
        }

        private boolean matchesHost(String candidate) {
            String normalizedCandidate = normalizeHost(candidate);
            String normalizedValue = normalizeHost(value);
            if (wildcardPattern != null) {
                return wildcardPattern.matcher(normalizedCandidate).matches();
            }
            return normalizedCandidate.equals(normalizedValue)
                    || normalizedCandidate.endsWith("." + normalizedValue)
                    || normalizedCandidate.contains(normalizedValue);
        }

        private boolean matchesText(String candidate) {
            if (wildcardPattern != null) {
                return wildcardPattern.matcher(candidate).find();
            }
            return candidate.contains(value.toLowerCase(Locale.ROOT));
        }

        private static String globToRegex(String value) {
            StringBuilder regex = new StringBuilder();
            regex.append(".*");
            for (char ch : value.toCharArray()) {
                if (ch == '*') {
                    regex.append(".*");
                } else {
                    regex.append(Pattern.quote(String.valueOf(ch)));
                }
            }
            regex.append(".*");
            return regex.toString();
        }
    }
}
