package com.laker.postman.plugin.capture;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;

import java.net.URI;

final class ProxyRequestTarget {
    final String host;
    final int port;
    final String fullUrl;
    final String requestUri;
    final String scheme;

    private ProxyRequestTarget(String host, int port, String fullUrl, String requestUri, String scheme) {
        this.host = host;
        this.port = port;
        this.fullUrl = fullUrl;
        this.requestUri = requestUri;
        this.scheme = scheme;
    }

    static ProxyRequestTarget resolve(FullHttpRequest request) {
        String rawUri = request.uri();
        try {
            URI uri = URI.create(rawUri);
            if (uri.isAbsolute() && uri.getHost() != null) {
                String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
                String normalizedScheme = normalizeScheme(scheme);
                int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(normalizedScheme) ? 443 : 80);
                String path = buildPath(uri);
                return new ProxyRequestTarget(uri.getHost(), port, uri.toString(), path, normalizedScheme);
            }
        } catch (IllegalArgumentException ignore) {
            // Fall through to Host header fallback.
        }

        String hostHeader = request.headers().get(HttpHeaderNames.HOST);
        if (hostHeader == null || hostHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Host header");
        }
        HostPort hostPort = parseAuthority(hostHeader, 80);
        String path = rawUri == null || rawUri.isBlank() ? "/" : rawUri;
        return new ProxyRequestTarget(
                hostPort.host(),
                hostPort.port(),
                "http://" + hostPort.authorityForUri(80) + path,
                path,
                "http"
        );
    }

    static HostPort parseAuthority(String authority, int defaultPort) {
        String normalized = authority == null ? "" : authority.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Missing host");
        }
        if (normalized.startsWith("[")) {
            int closingIndex = normalized.indexOf(']');
            if (closingIndex <= 1) {
                throw new IllegalArgumentException("Invalid IPv6 host: " + authority);
            }
            String host = normalized.substring(1, closingIndex);
            String remainder = normalized.substring(closingIndex + 1);
            if (remainder.isEmpty()) {
                return new HostPort(host, defaultPort);
            }
            if (!remainder.startsWith(":")) {
                throw new IllegalArgumentException("Invalid authority: " + authority);
            }
            return new HostPort(host, parsePort(remainder.substring(1), authority));
        }

        int colonCount = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == ':') {
                colonCount++;
            }
        }
        if (colonCount == 0) {
            return new HostPort(normalized, defaultPort);
        }
        if (colonCount > 1) {
            return new HostPort(normalized, defaultPort);
        }

        int colonIndex = normalized.indexOf(':');
        String host = normalized.substring(0, colonIndex).trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Invalid authority: " + authority);
        }
        return new HostPort(host, parsePort(normalized.substring(colonIndex + 1), authority));
    }

    private static int parsePort(String portText, String authority) {
        if (portText == null || portText.isBlank()) {
            throw new IllegalArgumentException("Missing port in authority: " + authority);
        }
        try {
            int port = Integer.parseInt(portText.trim());
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("Invalid port in authority: " + authority);
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid port in authority: " + authority, ex);
        }
    }

    private static String formatHostForUri(String host) {
        if (host == null || host.isBlank()) {
            return "";
        }
        if (host.indexOf(':') >= 0 && !(host.startsWith("[") && host.endsWith("]"))) {
            return "[" + host + "]";
        }
        return host;
    }

    private static String buildPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/";
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            path += "?" + uri.getRawQuery();
        }
        return path;
    }

    private static String normalizeScheme(String scheme) {
        if ("ws".equalsIgnoreCase(scheme)) {
            return "http";
        }
        if ("wss".equalsIgnoreCase(scheme)) {
            return "https";
        }
        return scheme == null ? "http" : scheme;
    }

    record HostPort(String host, int port) {
        String authorityForUri(int defaultPort) {
            String formattedHost = formatHostForUri(host);
            return port == defaultPort ? formattedHost : formattedHost + ":" + port;
        }
    }
}
