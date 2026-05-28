package com.laker.postman.performance.runtime.okhttp;

import com.laker.postman.performance.core.request.PerformanceAuthConfig;
import com.laker.postman.performance.core.request.PerformanceAuthType;
import com.laker.postman.performance.core.request.PerformanceOutboundRequest;
import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

final class OkHttpRequestFactory {
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final MediaType TEXT_MEDIA_TYPE = MediaType.get("text/plain; charset=utf-8");
    private static final MediaType OCTET_STREAM_MEDIA_TYPE = MediaType.get("application/octet-stream");
    private static final String HEADER_AUTHORIZATION = "Authorization";

    Request build(PerformanceOutboundRequest request) {
        Request.Builder builder = new Request.Builder()
                .url(resolveUrl(request))
                .method(request.getMethod(), requestBody(request));
        for (PerformanceRequestKeyValue header : request.getHeaders()) {
            builder.addHeader(header.getKey(), header.getValue());
        }
        String authorization = authorizationHeaderValue(request);
        if (authorization != null && !hasAuthorizationHeader(request.getHeaders())) {
            builder.addHeader(HEADER_AUTHORIZATION, authorization);
        }
        return builder.build();
    }

    private String resolveUrl(PerformanceOutboundRequest request) {
        HttpUrl parsed = HttpUrl.parse(toHttpUrlForParsing(request.getUrl()));
        if (parsed == null) {
            throw new IllegalArgumentException("Invalid request URL: " + request.getUrl());
        }
        HttpUrl.Builder builder = parsed.newBuilder();
        for (PerformanceRequestKeyValue param : request.getQueryParams()) {
            builder.addQueryParameter(param.getKey(), param.getValue());
        }
        return restoreWebSocketScheme(request.getUrl(), builder.build().toString());
    }

    private RequestBody requestBody(PerformanceOutboundRequest request) {
        String method = request.getMethod();
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return null;
        }
        if (!request.getFormData().isEmpty()) {
            MultipartBody.Builder multipart = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (PerformanceRequestFormDataPart part : request.getFormData()) {
                if (part.isFile()) {
                    File file = new File(part.getValue());
                    if (!file.isFile()) {
                        throw new IllegalArgumentException("Form data file does not exist: " + part.getValue());
                    }
                    multipart.addFormDataPart(
                            part.getKey(),
                            file.getName(),
                            RequestBody.create(file, OCTET_STREAM_MEDIA_TYPE)
                    );
                } else {
                    multipart.addFormDataPart(part.getKey(), part.getValue());
                }
            }
            return multipart.build();
        }
        if (!request.getUrlencoded().isEmpty()) {
            FormBody.Builder formBody = new FormBody.Builder(StandardCharsets.UTF_8);
            for (PerformanceRequestKeyValue value : request.getUrlencoded()) {
                formBody.add(value.getKey(), value.getValue());
            }
            return formBody.build();
        }
        String body = request.getBody();
        if (body == null || body.isEmpty()) {
            return RequestBody.create(new byte[0], null);
        }
        return RequestBody.create(body, mediaTypeFor(request));
    }

    private MediaType mediaTypeFor(PerformanceOutboundRequest request) {
        String bodyType = request.getBodyType() == null ? "" : request.getBodyType().toLowerCase();
        if (bodyType.contains("json")) {
            return JSON_MEDIA_TYPE;
        }
        return TEXT_MEDIA_TYPE;
    }

    private static boolean hasAuthorizationHeader(List<PerformanceRequestKeyValue> headers) {
        for (PerformanceRequestKeyValue header : headers) {
            if (HEADER_AUTHORIZATION.equalsIgnoreCase(header.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static String authorizationHeaderValue(PerformanceOutboundRequest request) {
        PerformanceAuthConfig authConfig = request.getAuthConfig();
        if (authConfig.getType() == PerformanceAuthType.BASIC && !isBlank(authConfig.getUsername())) {
            String credentials = authConfig.getUsername() + ":" + authConfig.getPassword();
            return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        }
        if (authConfig.getType() == PerformanceAuthType.BEARER && !isBlank(authConfig.getToken())) {
            return "Bearer " + authConfig.getToken();
        }
        return null;
    }

    private static String toHttpUrlForParsing(String url) {
        if (url == null) {
            return "";
        }
        if (startsWithIgnoreCase(url, "ws://")) {
            return "http://" + url.substring("ws://".length());
        }
        if (startsWithIgnoreCase(url, "wss://")) {
            return "https://" + url.substring("wss://".length());
        }
        return url;
    }

    private static String restoreWebSocketScheme(String originalUrl, String resolvedUrl) {
        if (startsWithIgnoreCase(originalUrl, "ws://") && startsWithIgnoreCase(resolvedUrl, "http://")) {
            return "ws://" + resolvedUrl.substring("http://".length());
        }
        if (startsWithIgnoreCase(originalUrl, "wss://") && startsWithIgnoreCase(resolvedUrl, "https://")) {
            return "wss://" + resolvedUrl.substring("https://".length());
        }
        return resolvedUrl;
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value != null && value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
