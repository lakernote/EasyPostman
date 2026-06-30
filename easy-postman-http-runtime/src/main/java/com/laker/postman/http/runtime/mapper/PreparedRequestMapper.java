package com.laker.postman.http.runtime.mapper;

import com.laker.postman.http.runtime.config.HttpRequestRuntimeSettingsResolver;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.request.model.*;
import com.laker.postman.request.util.HttpUrlUtil;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.laker.postman.request.model.RequestAuthTypes.*;

@UtilityClass
public class PreparedRequestMapper {

    private static final String HEADER_AUTHORIZATION = "Authorization";

    public record DeferredAuthorization(String authType,
                                        String authUsername,
                                        String authPassword,
                                        String authToken,
                                        String authApiKeyName,
                                        String authApiKeyValue,
                                        String authApiKeyPlacement,
                                        String previewHeaderKey,
                                        String previewHeaderValue,
                                        String previewQueryParamKey,
                                        String previewQueryParamValue) {
    }

    public static PreparedRequest map(HttpRequestItem effectiveItem, Function<String, String> variableResolver) {
        PreparedRequest req = new PreparedRequest();
        req.id = effectiveItem.getId();
        req.name = effectiveItem.getName();
        req.method = effectiveItem.getMethod();
        req.body = effectiveItem.getBody();
        req.bodyType = effectiveItem.getBodyType();
        req.url = buildRawUrlWithParams(effectiveItem);
        req.isMultipart = checkIsMultipart(effectiveItem.getFormDataList());
        req.followRedirects = HttpRequestRuntimeSettingsResolver.resolveFollowRedirects(effectiveItem);
        req.cookieJarEnabled = HttpRequestRuntimeSettingsResolver.resolveCookieJarEnabled(effectiveItem);
        req.proxyPolicy = HttpRequestRuntimeSettingsResolver.resolveProxyPolicy(effectiveItem);
        req.sslVerificationEnabled = HttpRequestRuntimeSettingsResolver.resolveSslVerificationEnabled(effectiveItem);
        req.httpVersion = HttpRequestRuntimeSettingsResolver.resolveHttpVersion(effectiveItem);
        req.requestTimeoutMs = HttpRequestRuntimeSettingsResolver.resolveRequestTimeoutMs(effectiveItem);
        req.webSocketPingIntervalMs = HttpRequestRuntimeSettingsResolver.resolveWebSocketPingIntervalMs(effectiveItem);
        req.transportAuth = createTransportAuth(effectiveItem);
        req.headersList = cloneHeaders(buildHeadersListWithResolvedAuth(effectiveItem, variableResolver));
        req.formDataList = cloneFormData(effectiveItem.getFormDataList());
        req.urlencodedList = cloneUrlencoded(effectiveItem.getUrlencodedList());
        req.pathVariablesList = cloneParams(effectiveItem.getPathVariablesList());
        req.paramsList = cloneParams(buildParamsListWithResolvedAuth(effectiveItem, variableResolver));
        req.prescript = effectiveItem.getPrescript();
        req.postscript = effectiveItem.getPostscript();
        return req;
    }

    public static DeferredAuthorization resolveDeferredAuthorization(HttpRequestItem effectiveItem,
                                                                     Function<String, String> variableResolver) {
        return new DeferredAuthorization(
                effectiveItem.getAuthType(),
                effectiveItem.getAuthUsername(),
                effectiveItem.getAuthPassword(),
                effectiveItem.getAuthToken(),
                effectiveItem.getAuthApiKeyName(),
                effectiveItem.getAuthApiKeyValue(),
                effectiveItem.getAuthApiKeyPlacement(),
                resolvePreviewHeaderKey(effectiveItem, variableResolver),
                resolvePreviewHeaderValue(effectiveItem, variableResolver),
                resolvePreviewQueryParamKey(effectiveItem, variableResolver),
                resolvePreviewQueryParamValue(effectiveItem, variableResolver)
        );
    }

    private static TransportAuth createTransportAuth(HttpRequestItem item) {
        if (item == null || !AUTH_TYPE_DIGEST.equals(item.getAuthType())) {
            return null;
        }
        return new TransportAuth(item.getAuthType(), item.getAuthUsername(), item.getAuthPassword());
    }

    private static String buildRawUrlWithParams(HttpRequestItem item) {
        String url = item.getUrl();
        if (item.getParamsList() == null || item.getParamsList().isEmpty()) {
            return url;
        }
        boolean hasQuery = url != null && url.contains("?");
        Set<String> existingKeys = url != null
                ? HttpUrlUtil.extractQueryParamKeys(url, hasQuery)
                : java.util.Collections.emptySet();
        StringBuilder sb = new StringBuilder(url != null ? url : "");
        for (HttpParam param : item.getParamsList()) {
            if (!param.isEnabled()) continue;
            String key = param.getKey();
            if (key == null || key.isEmpty()) continue;
            if (existingKeys.contains(key)) continue;
            sb.append(hasQuery ? "&" : "?");
            hasQuery = true;
            sb.append(key).append("=").append(param.getValue() != null ? param.getValue() : "");
        }
        return sb.toString();
    }

    private static boolean checkIsMultipart(List<HttpFormData> formDataList) {
        if (formDataList == null) {
            return false;
        }
        for (HttpFormData data : formDataList) {
            if (data.isEnabled() && (data.isText() || data.isFile())) {
                return true;
            }
        }
        return false;
    }

    private static List<HttpHeader> buildHeadersListWithResolvedAuth(HttpRequestItem item,
                                                                     Function<String, String> variableResolver) {
        List<HttpHeader> headers = cloneHeaders(item.getHeadersList());
        if (headers == null) {
            headers = new ArrayList<>();
        }
        HttpHeader authHeader = tryCreateResolvableAuthHeader(item, variableResolver);
        if (authHeader != null && !hasEnabledHeader(headers, authHeader.getKey())) {
            headers.add(authHeader);
        }
        return headers;
    }

    private static List<HttpParam> buildParamsListWithResolvedAuth(HttpRequestItem item,
                                                                   Function<String, String> variableResolver) {
        List<HttpParam> params = cloneParams(item.getParamsList());
        if (params == null) {
            params = new ArrayList<>();
        }
        HttpParam authParam = tryCreateResolvableQueryAuthParam(item, variableResolver);
        if (authParam != null && !hasEnabledQueryParam(item.getUrl(), params, authParam.getKey())) {
            params.add(authParam);
        }
        return params;
    }

    private static String resolvePreviewHeaderKey(HttpRequestItem item,
                                                  Function<String, String> variableResolver) {
        HttpHeader authHeader = resolvePreviewHeader(item, variableResolver);
        return authHeader != null ? authHeader.getKey() : null;
    }

    private static String resolvePreviewHeaderValue(HttpRequestItem item,
                                                    Function<String, String> variableResolver) {
        HttpHeader authHeader = resolvePreviewHeader(item, variableResolver);
        return authHeader != null ? authHeader.getValue() : null;
    }

    private static HttpHeader resolvePreviewHeader(HttpRequestItem item,
                                                   Function<String, String> variableResolver) {
        if (item == null) {
            return null;
        }
        HttpHeader authHeader = tryCreateResolvableAuthHeader(item, variableResolver);
        if (authHeader == null || hasEnabledHeader(item.getHeadersList(), authHeader.getKey())) {
            return null;
        }
        return authHeader;
    }

    private static String resolvePreviewQueryParamKey(HttpRequestItem item,
                                                      Function<String, String> variableResolver) {
        HttpParam authParam = resolvePreviewQueryParam(item, variableResolver);
        return authParam != null ? authParam.getKey() : null;
    }

    private static String resolvePreviewQueryParamValue(HttpRequestItem item,
                                                        Function<String, String> variableResolver) {
        HttpParam authParam = resolvePreviewQueryParam(item, variableResolver);
        return authParam != null ? authParam.getValue() : null;
    }

    private static HttpParam resolvePreviewQueryParam(HttpRequestItem item,
                                                      Function<String, String> variableResolver) {
        HttpParam authParam = tryCreateResolvableQueryAuthParam(item, variableResolver);
        if (authParam == null || hasEnabledQueryParam(item.getUrl(), item.getParamsList(), authParam.getKey())) {
            return null;
        }
        return authParam;
    }

    private static boolean hasEnabledHeader(List<HttpHeader> headers, String key) {
        if (headers == null || headers.isEmpty() || key == null || key.isEmpty()) {
            return false;
        }
        return headers.stream()
                .anyMatch(h -> h != null && h.isEnabled() && key.equalsIgnoreCase(h.getKey()));
    }

    private static boolean hasEnabledQueryParam(String url, List<HttpParam> params, String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String encodedKey = HttpUrlUtil.encodeComponent(key);
        boolean hasQuery = url != null && url.contains("?");
        if (HttpUrlUtil.extractQueryParamKeys(url, hasQuery).contains(encodedKey)) {
            return true;
        }
        if (params == null || params.isEmpty()) {
            return false;
        }
        return params.stream()
                .anyMatch(param -> param != null
                        && param.isEnabled()
                        && key.equals(param.getKey()));
    }

    private static HttpHeader tryCreateResolvableAuthHeader(HttpRequestItem item,
                                                            Function<String, String> variableResolver) {
        if (item == null || item.getAuthType() == null) {
            return null;
        }
        if (AUTH_TYPE_BASIC.equals(item.getAuthType())) {
            return createResolvableBasicAuthHeader(item.getAuthUsername(), item.getAuthPassword(), variableResolver);
        }
        if (AUTH_TYPE_BEARER.equals(item.getAuthType())) {
            return createResolvableBearerAuthHeader(item.getAuthToken(), variableResolver);
        }
        if (AUTH_TYPE_API_KEY.equals(item.getAuthType())
                && AuthApiKeyPlacement.HEADER == AuthApiKeyPlacement.fromConstant(item.getAuthApiKeyPlacement())) {
            return createResolvableApiKeyHeader(item.getAuthApiKeyName(), item.getAuthApiKeyValue(), variableResolver);
        }
        return null;
    }

    private static HttpHeader createResolvableBasicAuthHeader(String rawUsername,
                                                              String rawPassword,
                                                              Function<String, String> variableResolver) {
        String username = resolve(variableResolver, rawUsername);
        if (username == null || username.isEmpty() || containsUnresolvedPlaceholder(username)) {
            return null;
        }

        String password = resolve(variableResolver, rawPassword);
        if (containsUnresolvedPlaceholder(password)) {
            return null;
        }

        String credentials = username + ":" + (password == null ? "" : password);
        String token = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
        return authorizationHeader("Basic " + token);
    }

    private static HttpHeader createResolvableBearerAuthHeader(String rawToken,
                                                               Function<String, String> variableResolver) {
        String token = resolve(variableResolver, rawToken);
        if (token == null || token.isEmpty() || containsUnresolvedPlaceholder(token)) {
            return null;
        }
        return authorizationHeader("Bearer " + token);
    }

    private static HttpHeader createResolvableApiKeyHeader(String rawName,
                                                           String rawValue,
                                                           Function<String, String> variableResolver) {
        String key = resolve(variableResolver, rawName);
        if (key == null || key.isEmpty() || containsUnresolvedPlaceholder(key)) {
            return null;
        }
        String value = resolve(variableResolver, rawValue);
        if (value == null || value.isEmpty() || containsUnresolvedPlaceholder(value)) {
            return null;
        }
        HttpHeader header = new HttpHeader();
        header.setKey(key);
        header.setValue(value);
        header.setEnabled(true);
        return header;
    }

    private static HttpParam tryCreateResolvableQueryAuthParam(HttpRequestItem item,
                                                               Function<String, String> variableResolver) {
        if (item == null) {
            return null;
        }
        if (AUTH_TYPE_API_KEY.equals(item.getAuthType())) {
            return tryCreateResolvableApiKeyParam(item, variableResolver);
        }
        return null;
    }

    private static HttpParam tryCreateResolvableApiKeyParam(HttpRequestItem item,
                                                            Function<String, String> variableResolver) {
        if (AuthApiKeyPlacement.QUERY_PARAMS != AuthApiKeyPlacement.fromConstant(item.getAuthApiKeyPlacement())) {
            return null;
        }
        String key = resolve(variableResolver, item.getAuthApiKeyName());
        String value = resolve(variableResolver, item.getAuthApiKeyValue());
        return createEnabledParam(key, value);
    }

    private static HttpParam createEnabledParam(String key, String value) {
        if (key == null || key.isEmpty() || containsUnresolvedPlaceholder(key)) {
            return null;
        }
        if (value == null || value.isEmpty() || containsUnresolvedPlaceholder(value)) {
            return null;
        }
        return new HttpParam(true, key, value, "");
    }

    private static String resolve(Function<String, String> variableResolver, String value) {
        return variableResolver == null ? value : variableResolver.apply(value);
    }

    private static HttpHeader authorizationHeader(String value) {
        HttpHeader authHeader = new HttpHeader();
        authHeader.setKey(HEADER_AUTHORIZATION);
        authHeader.setValue(value);
        authHeader.setEnabled(true);
        return authHeader;
    }

    private static boolean containsUnresolvedPlaceholder(String value) {
        return value != null && value.contains("{{") && value.contains("}}");
    }

    private static List<HttpHeader> cloneHeaders(List<HttpHeader> list) {
        if (list == null) {
            return null;
        }
        List<HttpHeader> cloned = new ArrayList<>(list.size());
        for (HttpHeader item : list) {
            cloned.add(item == null ? null : new HttpHeader(item.isEnabled(), item.getKey(), item.getValue(), item.getDescription()));
        }
        return cloned;
    }

    private static List<HttpFormData> cloneFormData(List<HttpFormData> list) {
        if (list == null) {
            return null;
        }
        List<HttpFormData> cloned = new ArrayList<>(list.size());
        for (HttpFormData item : list) {
            cloned.add(item == null ? null : new HttpFormData(
                    item.isEnabled(),
                    item.getKey(),
                    item.getType(),
                    item.getValue(),
                    item.getDescription()
            ));
        }
        return cloned;
    }

    private static List<HttpFormUrlencoded> cloneUrlencoded(List<HttpFormUrlencoded> list) {
        if (list == null) {
            return null;
        }
        List<HttpFormUrlencoded> cloned = new ArrayList<>(list.size());
        for (HttpFormUrlencoded item : list) {
            cloned.add(item == null ? null : new HttpFormUrlencoded(
                    item.isEnabled(),
                    item.getKey(),
                    item.getValue(),
                    item.getDescription()
            ));
        }
        return cloned;
    }

    private static List<HttpParam> cloneParams(List<HttpParam> list) {
        if (list == null) {
            return null;
        }
        List<HttpParam> cloned = new ArrayList<>(list.size());
        for (HttpParam item : list) {
            cloned.add(item == null ? null : new HttpParam(item.isEnabled(), item.getKey(), item.getValue(), item.getDescription()));
        }
        return cloned;
    }
}
