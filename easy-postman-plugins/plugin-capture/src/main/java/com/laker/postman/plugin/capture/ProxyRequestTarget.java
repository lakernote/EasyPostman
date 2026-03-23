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
                int port = uri.getPort() > 0 ? uri.getPort() : ("https".equalsIgnoreCase(scheme) ? 443 : 80);
                String path = buildPath(uri);
                return new ProxyRequestTarget(uri.getHost(), port, uri.toString(), path, scheme);
            }
        } catch (IllegalArgumentException ignore) {
            // Fall through to Host header fallback.
        }

        String hostHeader = request.headers().get(HttpHeaderNames.HOST);
        if (hostHeader == null || hostHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Host header");
        }
        String host = hostHeader;
        int port = 80;
        int colonIndex = hostHeader.lastIndexOf(':');
        if (colonIndex > 0 && colonIndex < hostHeader.length() - 1) {
            host = hostHeader.substring(0, colonIndex);
            port = Integer.parseInt(hostHeader.substring(colonIndex + 1));
        }
        String path = rawUri == null || rawUri.isBlank() ? "/" : rawUri;
        return new ProxyRequestTarget(host, port, "http://" + hostHeader + path, path, "http");
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
}
