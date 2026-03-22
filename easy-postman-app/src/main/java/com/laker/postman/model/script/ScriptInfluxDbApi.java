package com.laker.postman.model.script;

import cn.hutool.json.JSONUtil;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.util.JsonUtil;
import okhttp3.*;

import java.io.IOException;
import java.util.*;

/**
 * Script InfluxDB API for pm.influxdb / pm.influx.
 */
public class ScriptInfluxDbApi {
    private static final String DEFAULT_BASE_URL = "http://localhost:8086";
    private static final String JSON_MIME = "application/json";
    private static final String TEXT_PLAIN = "text/plain; charset=utf-8";
    private static final String TEXT_CSV = "text/csv";
    private static final String APPLICATION_VND_FLUX = "application/vnd.flux";
    private static final String AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Token ";
    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 30_000;
    private static final int WRITE_TIMEOUT_MS = 30_000;
    private final OkHttpClient httpClient;

    public ScriptInfluxDbApi() {
        this(null);
    }

    public ScriptInfluxDbApi(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public InfluxResponse query(Object options) {
        Map<String, Object> map = ScriptOptionUtil.toMap(options);
        String mode = resolveMode(map);
        return isV2(mode) ? queryV2(map) : queryV1(map);
    }

    public InfluxResponse write(Object options) {
        Map<String, Object> map = ScriptOptionUtil.toMap(options);
        String mode = resolveMode(map);
        String lineProtocol = ScriptOptionUtil.getRequiredString(map,
                "lineProtocol", "body", "data", "payload", "value");
        return isV2(mode) ? writeV2(map, lineProtocol) : writeV1(map, lineProtocol);
    }

    /**
     * Execute one raw InfluxDB HTTP request.
     *
     * options:
     * baseUrl/host/url, path, method, headers, body, mode/version
     * token, username, password
     */
    public InfluxResponse request(Object options) {
        Map<String, Object> map = ScriptOptionUtil.toMap(options);
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String path = ScriptOptionUtil.getRequiredString(map, "path");
        String method = ScriptOptionUtil.getString(map, "GET", "method").toUpperCase(Locale.ROOT);
        String body = ScriptOptionUtil.getString(map, "", "body", "data", "payload");
        String accept = ScriptOptionUtil.getString(map, "", "accept");
        String contentType = ScriptOptionUtil.getString(map, "", "contentType");
        String mode = resolveMode(map);

        try {
            HttpUrl.Builder urlBuilder = buildUrl(baseUrl, path);
            if (!isV2(mode) && shouldAttachV1CredentialParams(path)) {
                appendV1CredentialQueryParams(urlBuilder, map);
            }
            Request request = buildRequest(urlBuilder.build(), method, body, contentType, accept, map, mode);
            return executeRequest(request, mode, path, "request");
        } catch (Exception e) {
            throw new RuntimeException("InfluxDB request failed: " + rootMessage(e), e);
        }
    }

    private InfluxResponse queryV1(Map<String, Object> map) {
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String database = ScriptOptionUtil.getRequiredString(map, "db", "database");
        String query = ScriptOptionUtil.getRequiredString(map, "query", "q", "influxql");
        String epoch = ScriptOptionUtil.getString(map, "ms", "epoch");

        try {
            HttpUrl.Builder urlBuilder = buildUrl(baseUrl, "/query");
            urlBuilder.addQueryParameter("db", database);
            urlBuilder.addQueryParameter("q", query);
            if (!ScriptOptionUtil.isBlank(epoch)) {
                urlBuilder.addQueryParameter("epoch", epoch);
            }
            appendV1CredentialQueryParams(urlBuilder, map);
            Request request = buildRequest(urlBuilder.build(), "GET", "", "", JSON_MIME, map, "v1");
            return executeRequest(request, "v1", "/query", "query");
        } catch (Exception e) {
            throw new RuntimeException("InfluxDB query failed: " + rootMessage(e), e);
        }
    }

    private InfluxResponse queryV2(Map<String, Object> map) {
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String org = ScriptOptionUtil.getRequiredString(map, "org");
        String query = ScriptOptionUtil.getRequiredString(map, "query", "q", "flux");

        try {
            HttpUrl.Builder urlBuilder = buildUrl(baseUrl, "/api/v2/query");
            urlBuilder.addQueryParameter("org", org);
            Request request = buildRequest(
                    urlBuilder.build(),
                    "POST",
                    query,
                    APPLICATION_VND_FLUX,
                    TEXT_CSV,
                    map,
                    "v2");
            return executeRequest(request, "v2", "/api/v2/query", "query");
        } catch (Exception e) {
            throw new RuntimeException("InfluxDB query failed: " + rootMessage(e), e);
        }
    }

    private InfluxResponse writeV1(Map<String, Object> map, String lineProtocol) {
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String database = ScriptOptionUtil.getRequiredString(map, "db", "database");
        String precision = ScriptOptionUtil.getString(map, "ms", "precision");

        try {
            HttpUrl.Builder urlBuilder = buildUrl(baseUrl, "/write");
            urlBuilder.addQueryParameter("db", database);
            if (!ScriptOptionUtil.isBlank(precision)) {
                urlBuilder.addQueryParameter("precision", precision);
            }
            appendV1CredentialQueryParams(urlBuilder, map);
            Request request = buildRequest(
                    urlBuilder.build(),
                    "POST",
                    lineProtocol,
                    TEXT_PLAIN,
                    "",
                    map,
                    "v1");
            return executeRequest(request, "v1", "/write", "write");
        } catch (Exception e) {
            throw new RuntimeException("InfluxDB write failed: " + rootMessage(e), e);
        }
    }

    private InfluxResponse writeV2(Map<String, Object> map, String lineProtocol) {
        String baseUrl = ScriptOptionUtil.getString(map, DEFAULT_BASE_URL, "baseUrl", "host", "url");
        String org = ScriptOptionUtil.getRequiredString(map, "org");
        String bucket = ScriptOptionUtil.getRequiredString(map, "bucket");
        String precision = ScriptOptionUtil.getString(map, "ms", "precision");

        try {
            HttpUrl.Builder urlBuilder = buildUrl(baseUrl, "/api/v2/write");
            urlBuilder.addQueryParameter("org", org);
            urlBuilder.addQueryParameter("bucket", bucket);
            if (!ScriptOptionUtil.isBlank(precision)) {
                urlBuilder.addQueryParameter("precision", precision);
            }
            Request request = buildRequest(
                    urlBuilder.build(),
                    "POST",
                    lineProtocol,
                    TEXT_PLAIN,
                    "",
                    map,
                    "v2");
            return executeRequest(request, "v2", "/api/v2/write", "write");
        } catch (Exception e) {
            throw new RuntimeException("InfluxDB write failed: " + rootMessage(e), e);
        }
    }

    private Request buildRequest(
            HttpUrl url,
            String method,
            String body,
            String contentType,
            String accept,
            Map<String, Object> options,
            String mode) {
        Request.Builder builder = new Request.Builder().url(url);

        if (!ScriptOptionUtil.isBlank(accept)) {
            builder.header("Accept", accept);
        }
        if (!ScriptOptionUtil.isBlank(contentType)) {
            builder.header("Content-Type", contentType);
        }

        if (isV2(mode)) {
            String token = ScriptOptionUtil.getString(options, "", "token");
            if (!token.isBlank()) {
                builder.header(AUTHORIZATION, TOKEN_PREFIX + token);
            }
        }
        applyCustomHeaders(builder, ScriptOptionUtil.get(options, "headers", "header"));

        RequestBody requestBody = buildRequestBody(body, contentType);
        switch (method) {
            case "GET" -> builder.get();
            case "POST" -> builder.post(requestBody == null ? emptyBody() : requestBody);
            case "PUT" -> builder.put(requestBody == null ? emptyBody() : requestBody);
            case "DELETE" -> {
                if (requestBody == null) {
                    builder.delete();
                } else {
                    builder.delete(requestBody);
                }
            }
            case "HEAD" -> builder.head();
            default -> builder.method(method, requestBody);
        }
        return builder.build();
    }

    private RequestBody buildRequestBody(String body, String contentType) {
        if (body == null || body.isBlank()) {
            return null;
        }
        MediaType mediaType = ScriptOptionUtil.isBlank(contentType) ? null : MediaType.get(contentType);
        return RequestBody.create(body, mediaType);
    }

    private RequestBody emptyBody() {
        return RequestBody.create(new byte[0], null);
    }

    private InfluxResponse executeRequest(Request request, String mode, String path, String operation) throws IOException {
        try (Response response = resolveHttpClient(request.url().toString()).newCall(request).execute()) {
            String responseBody = response.body() == null ? "" : response.body().string();
            Object json = JsonUtil.isTypeJSON(responseBody) ? JSONUtil.parse(responseBody) : null;
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("mode", mode);
            meta.put("operation", operation);
            meta.put("path", path);
            meta.put("contentType", response.header("Content-Type"));
            return new InfluxResponse(response.code(), responseBody, json, meta);
        }
    }

    private OkHttpClient resolveHttpClient(String url) {
        if (httpClient != null) {
            return httpClient;
        }
        return OkHttpClientManager.getClientForUrl(
                url,
                true,
                CONNECT_TIMEOUT_MS,
                READ_TIMEOUT_MS,
                WRITE_TIMEOUT_MS
        );
    }

    private void applyCustomHeaders(Request.Builder builder, Object headersObj) {
        Object normalized = ScriptOptionUtil.normalize(headersObj);
        if (normalized == null) {
            return;
        }
        if (normalized instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey()).trim();
                if (!key.isBlank()) {
                    String value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                    builder.header(key, value);
                }
            }
            return;
        }
        if (normalized instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item instanceof Map<?, ?> itemMap) {
                    Map<String, Object> values = toStringMap(itemMap);
                    String key = ScriptOptionUtil.getString(values, "", "key", "name");
                    if (!key.isBlank()) {
                        String value = ScriptOptionUtil.getString(values, "", "value");
                        builder.header(key, value);
                    }
                }
            }
        }
    }

    private Map<String, Object> toStringMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
            result.put(key, entry.getValue());
        }
        return result;
    }

    private HttpUrl.Builder buildUrl(String baseUrl, String path) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse(normalizedBaseUrl),
                "Invalid baseUrl: " + normalizedBaseUrl);
        HttpUrl.Builder builder = httpUrl.newBuilder();
        String normalizedPath = path == null ? "" : path.trim();
        String[] raw = normalizedPath.split("\\?", 2);
        builder.encodedPath(normalizeRequestPath(raw[0]));
        if (raw.length > 1 && !raw[1].isBlank()) {
            appendRawQuery(builder, raw[1]);
        }
        return builder;
    }

    private String normalizeRequestPath(String path) {
        String normalized = path == null ? "" : path.trim();
        if (normalized.isEmpty()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private void appendRawQuery(HttpUrl.Builder builder, String rawQuery) {
        for (String part : rawQuery.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            String[] kv = part.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";
            builder.addEncodedQueryParameter(key, value);
        }
    }

    private boolean shouldAttachV1CredentialParams(String path) {
        return path != null && (path.startsWith("/query") || path.startsWith("/write"));
    }

    private void appendV1CredentialQueryParams(HttpUrl.Builder builder, Map<String, Object> options) {
        String username = ScriptOptionUtil.getString(options, "", "username", "user");
        String password = ScriptOptionUtil.getString(options, "", "password", "pass");
        if (!username.isBlank()) {
            builder.addQueryParameter("u", username);
        }
        if (!password.isBlank()) {
            builder.addQueryParameter("p", password);
        }
    }

    private String resolveMode(Map<String, Object> map) {
        String explicitMode = ScriptOptionUtil.getString(map, "", "mode", "version", "apiVersion")
                .toLowerCase(Locale.ROOT);
        if ("v2".equals(explicitMode) || "2".equals(explicitMode) || "flux".equals(explicitMode)) {
            return "v2";
        }
        if ("v1".equals(explicitMode) || "1".equals(explicitMode) || "influxql".equals(explicitMode)) {
            return "v1";
        }
        if (ScriptOptionUtil.get(map, "token") != null
                || ScriptOptionUtil.get(map, "org") != null
                || ScriptOptionUtil.get(map, "bucket") != null) {
            return "v2";
        }
        return "v1";
    }

    private boolean isV2(String mode) {
        return "v2".equalsIgnoreCase(mode);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = ScriptOptionUtil.isBlank(baseUrl) ? DEFAULT_BASE_URL : baseUrl.trim();
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String msg = current.getMessage();
        return (msg == null || msg.isBlank()) ? current.toString() : msg;
    }

    public static class InfluxResponse {
        public final int code;
        public final String body;
        public final Object json;
        public final Map<String, Object> meta;

        public InfluxResponse(int code, String body, Object json, Map<String, Object> meta) {
            this.code = code;
            this.body = body;
            this.json = json;
            this.meta = meta == null ? Map.of() : meta;
        }
    }
}
