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
    private final String rawValue;
    private final List<Rule> includeHostRules;
    private final List<Rule> excludeHostRules;
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
    private final List<String> summaryTokens;

    private CaptureRequestFilter(String rawValue,
                                 List<Rule> includeHostRules,
                                 List<Rule> excludeHostRules,
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
                                 List<String> summaryTokens) {
        this.rawValue = rawValue;
        this.includeHostRules = List.copyOf(includeHostRules);
        this.excludeHostRules = List.copyOf(excludeHostRules);
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
        this.summaryTokens = List.copyOf(summaryTokens);
    }

    static CaptureRequestFilter parse(String rawValue) {
        String normalizedRaw = rawValue == null ? "" : rawValue.trim();
        List<Rule> includeHostRules = new ArrayList<>();
        List<Rule> excludeHostRules = new ArrayList<>();
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
        List<String> summaryTokens = new ArrayList<>();

        if (!normalizedRaw.isEmpty()) {
            for (String token : normalizedRaw.split("[,;\\s\\r\\n]+")) {
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
                    case PATH -> (rule.exclude() ? excludePathRules : includePathRules).add(rule);
                    case QUERY -> (rule.exclude() ? excludeQueryRules : includeQueryRules).add(rule);
                    case URL -> (rule.exclude() ? excludeUrlRules : includeUrlRules).add(rule);
                    case SCHEME -> (rule.exclude() ? excludeSchemeRules : includeSchemeRules).add(rule);
                    case TYPE -> (rule.exclude() ? excludeTypeRules : includeTypeRules).add(rule);
                    case REGEX -> (rule.exclude() ? excludeRegexRules : includeRegexRules).add(rule);
                }
            }
        }

        return new CaptureRequestFilter(
                normalizedRaw,
                includeHostRules,
                excludeHostRules,
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
                summaryTokens
        );
    }

    boolean matches(String host, String requestUri, String fullUrl, Map<String, String> headers) {
        RequestParts parts = RequestParts.from(host, requestUri, fullUrl, headers);
        if (matchesAny(excludeHostRules, parts.host())
                || matchesAny(excludePathRules, parts.path())
                || matchesAny(excludeQueryRules, parts.query())
                || matchesAny(excludeUrlRules, parts.fullUrl())
                || matchesAny(excludeSchemeRules, parts.scheme())
                || matchesAny(excludeTypeRules, parts.resourceType())
                || matchesAny(excludeRegexRules, parts.fullUrl())) {
            return false;
        }
        return matchesDimension(includeHostRules, parts.host())
                && matchesDimension(includePathRules, parts.path())
                && matchesDimension(includeQueryRules, parts.query())
                && matchesDimension(includeUrlRules, parts.fullUrl())
                && matchesDimension(includeSchemeRules, parts.scheme())
                && matchesDimension(includeTypeRules, parts.resourceType())
                && matchesDimension(includeRegexRules, parts.fullUrl());
    }

    boolean shouldMitmHost(String host) {
        String normalizedHost = normalizeHost(host);
        if (matchesAny(excludeHostRules, normalizedHost)) {
            return false;
        }
        if (includeHostRules.isEmpty()) {
            return true;
        }
        return matchesAny(includeHostRules, normalizedHost);
    }

    String rawValue() {
        return rawValue;
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

    private record RequestParts(String host,
                                String path,
                                String query,
                                String fullUrl,
                                String scheme,
                                String resourceType) {
        private static RequestParts from(String host, String requestUri, String fullUrl, Map<String, String> headers) {
            String normalizedHost = normalizeHost(host);
            String uri = requestUri == null || requestUri.isBlank() ? "/" : requestUri;
            int queryIndex = uri.indexOf('?');
            String path = queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
            String query = queryIndex >= 0 && queryIndex < uri.length() - 1 ? uri.substring(queryIndex + 1) : "";
            if (path.isBlank()) {
                path = "/";
            }
            String normalizedFullUrl = (fullUrl == null ? "" : fullUrl).toLowerCase(Locale.ROOT);
            return new RequestParts(
                    normalizedHost,
                    path.toLowerCase(Locale.ROOT),
                    query.toLowerCase(Locale.ROOT),
                    normalizedFullUrl,
                    detectScheme(normalizedFullUrl),
                    detectResourceType(path, headers)
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
            String requestedWith = normalizedHeaders.getOrDefault("x-requested-with", "");

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

    private enum RuleType {
        HOST,
        PATH,
        QUERY,
        URL,
        SCHEME,
        TYPE,
        REGEX
    }

    private static final Set<String> QUICK_SCHEME_ALIASES = Set.of("http", "https");
    private static final Set<String> QUICK_TYPE_ALIASES = Set.of("image", "img", "json", "html", "js", "css", "font", "media", "api");
    private static final Map<String, String> CANONICAL_TYPE_ALIASES = Map.of(
            "img", "image"
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
            if (type == RuleType.SCHEME || type == RuleType.TYPE) {
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
                case PATH -> "path:";
                case QUERY -> "query:";
                case URL -> "url:";
                case SCHEME -> "scheme:";
                case TYPE -> "type:";
                case REGEX -> "regex:";
            };
            if ((type == RuleType.HOST && !value.contains("*") && !value.contains(":"))
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
                case PATH, QUERY, URL, SCHEME, TYPE -> matchesText(candidate);
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
