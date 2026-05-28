package com.laker.postman.performance.core.request;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
public class PerformanceOutboundRequest {
    String id;
    String name;
    PerformanceProtocol protocol;
    String method;
    String url;
    List<PerformanceRequestKeyValue> headers;
    List<PerformanceRequestKeyValue> queryParams;
    String bodyType;
    String body;
    List<PerformanceRequestFormDataPart> formData;
    List<PerformanceRequestKeyValue> urlencoded;
    PerformanceAuthConfig authConfig;
    Boolean followRedirects;
    Boolean cookieJarEnabled;
    String httpVersion;
    Integer requestTimeoutMs;

    public PerformanceOutboundRequest(String id,
                                      String name,
                                      PerformanceProtocol protocol,
                                      String method,
                                      String url,
                                      List<PerformanceRequestKeyValue> headers,
                                      List<PerformanceRequestKeyValue> queryParams,
                                      String bodyType,
                                      String body,
                                      List<PerformanceRequestFormDataPart> formData,
                                      List<PerformanceRequestKeyValue> urlencoded,
                                      PerformanceAuthConfig authConfig,
                                      Boolean followRedirects,
                                      Boolean cookieJarEnabled,
                                      String httpVersion,
                                      Integer requestTimeoutMs) {
        this.id = blankToEmpty(id);
        this.name = blankToEmpty(name);
        this.protocol = protocol == null ? PerformanceProtocol.HTTP : protocol;
        this.method = normalizeMethod(method);
        this.url = blankToEmpty(url);
        this.headers = enabledKeyValues(headers);
        this.queryParams = enabledKeyValues(queryParams);
        this.bodyType = blankToEmpty(bodyType);
        this.body = blankToEmpty(body);
        this.formData = enabledFormData(formData);
        this.urlencoded = enabledKeyValues(urlencoded);
        this.authConfig = authConfig == null ? PerformanceAuthConfig.inherit() : authConfig;
        this.followRedirects = followRedirects;
        this.cookieJarEnabled = cookieJarEnabled;
        this.httpVersion = normalizeHttpVersion(httpVersion);
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public static PerformanceOutboundRequest fromSnapshot(PerformanceRequestSnapshot snapshot) {
        PerformanceRequestSnapshot source = snapshot == null ? PerformanceRequestSnapshot.empty() : snapshot;
        return PerformanceOutboundRequest.builder()
                .id(source.getId())
                .name(source.getName())
                .protocol(source.getProtocol())
                .method(source.getMethod())
                .url(source.getUrl())
                .headers(source.getHeaders())
                .queryParams(source.getParams())
                .bodyType(source.getBodyType())
                .body(source.getBody())
                .formData(source.getFormData())
                .urlencoded(source.getUrlencoded())
                .authConfig(PerformanceAuthConfig.fromSnapshotFields(
                        source.getAuthType(),
                        source.getAuthUsername(),
                        source.getAuthPassword(),
                        source.getAuthToken()
                ))
                .followRedirects(source.getFollowRedirects())
                .cookieJarEnabled(source.getCookieJarEnabled())
                .httpVersion(source.getHttpVersion())
                .requestTimeoutMs(source.getRequestTimeoutMs())
                .build();
    }

    private static List<PerformanceRequestKeyValue> enabledKeyValues(List<PerformanceRequestKeyValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value.isEnabled() && !value.getKey().isBlank())
                .map(value -> new PerformanceRequestKeyValue(true, value.getKey().trim(), value.getValue()))
                .toList();
    }

    private static List<PerformanceRequestFormDataPart> enabledFormData(List<PerformanceRequestFormDataPart> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value.isEnabled() && !value.getKey().isBlank())
                .map(value -> new PerformanceRequestFormDataPart(true, value.getKey().trim(), value.getType(), value.getValue()))
                .toList();
    }

    private static String normalizeMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "GET";
        }
        return method.trim().toUpperCase();
    }

    private static String normalizeHttpVersion(String httpVersion) {
        if (httpVersion == null || httpVersion.trim().isEmpty()) {
            return PerformanceRequestSnapshot.HTTP_VERSION_AUTO;
        }
        return httpVersion.trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
