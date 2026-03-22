package com.laker.postman.model.script;

import cn.hutool.json.JSONUtil;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.JsonUtil;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Script Elasticsearch API for pm.es / pm.elasticsearch.
 */
public class ScriptElasticsearchApi {
    private static final String DEFAULT_BASE_URL = "http://localhost:9200";
    private static final String JSON_UTF8 = "application/json; charset=utf-8";
    private static final String JSON_MIME = "application/json";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;

    public EsResponse query(Object options) {
        return request(options);
    }

    /**
     * Execute one Elasticsearch request.
     *
     * options:
     * baseUrl/host/url, username, password
     * method, path, body, headers
     */
    public EsResponse request(Object options) {
        Map<String, Object> map = ScriptOptionUtil.toMap(options);
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String method = ScriptOptionUtil.getString(map, "GET", "method").toUpperCase();
        String path = ScriptOptionUtil.getRequiredString(map, "path");
        String body = ScriptOptionUtil.getString(map, "", "body");
        String username = ScriptOptionUtil.getString(map, "", "username", "user");
        String password = ScriptOptionUtil.getString(map, "", "password", "pass");

        try {
            Request request = buildRequest(baseUrl, method, path, body, username, password, map);
            try (Response response = createHttpClient(baseUrl).newCall(request).execute()) {
                String responseBody = response.body() == null ? "" : response.body().string();
                Object json = JsonUtil.isTypeJSON(responseBody) ? JSONUtil.parse(responseBody) : null;
                return new EsResponse(response.code(), responseBody, json);
            }
        } catch (Exception e) {
            throw new RuntimeException("Elasticsearch request failed: " + rootMessage(e), e);
        }
    }

    private OkHttpClient createHttpClient(String baseUrl) {
        return OkHttpClientManager.getClientForUrl(
                baseUrl,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
    }

    private Request buildRequest(
            String baseUrl,
            String method,
            String path,
            String body,
            String username,
            String password,
            Map<String, Object> options) throws IOException {
        String url = normalizeBaseUrl(baseUrl) + normalizePath(path);
        Request.Builder builder = new Request.Builder().url(url);
        builder.header("Content-Type", JSON_MIME);

        String authHeader = buildBasicAuthHeader(username, password);
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        applyCustomHeaders(builder, ScriptOptionUtil.get(options, "headers", "header"));

        RequestBody requestBody = (body == null || body.isBlank())
                ? null
                : RequestBody.create(body, MediaType.get(JSON_UTF8));

        switch (method) {
            case "POST" -> builder.post(requestBody == null ? RequestBody.create(new byte[0], null) : requestBody);
            case "PUT" -> builder.put(requestBody == null ? RequestBody.create(new byte[0], MediaType.get(JSON_MIME)) : requestBody);
            case "DELETE" -> {
                if (requestBody == null) builder.delete();
                else builder.delete(requestBody);
            }
            case "HEAD" -> builder.head();
            case "GET" -> {
                if (requestBody == null) {
                    builder.get();
                } else {
                    // Keep toolbox behavior: GET with body falls back to POST for compatibility.
                    builder.method("POST", requestBody);
                }
            }
            default -> builder.method(method, requestBody);
        }

        return builder.build();
    }

    private void applyCustomHeaders(Request.Builder builder, Object headersObj) {
        Object normalized = ScriptOptionUtil.normalize(headersObj);
        if (normalized == null) {
            return;
        }
        if (normalized instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim();
                if (key.isBlank()) {
                    continue;
                }
                String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                builder.header(key, value);
            }
        }
    }

    private String buildBasicAuthHeader(String username, String password) {
        if (ScriptOptionUtil.isBlank(username)) {
            return null;
        }
        String raw = username + ":" + (password == null ? "" : password);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizePath(String path) {
        String value = path == null ? "" : path.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required option: path");
        }
        return value.startsWith("/") ? value : "/" + value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String msg = current.getMessage();
        return (msg == null || msg.isBlank()) ? current.toString() : msg;
    }

    public static class EsResponse {
        public final int code;
        public final String body;
        public final Object json;
        public final Map<String, Object> meta;

        public EsResponse(int code, String body, Object json) {
            this.code = code;
            this.body = body;
            this.json = json;
            this.meta = new LinkedHashMap<>();
        }
    }
}
