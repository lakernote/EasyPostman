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
                                        String previewAuthorizationHeaderValue) {
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
        req.transportAuth = createTransportAuth(effectiveItem);
        req.headersList = cloneHeaders(buildHeadersListWithResolvedAuth(effectiveItem, variableResolver));
        req.formDataList = cloneFormData(effectiveItem.getFormDataList());
        req.urlencodedList = cloneUrlencoded(effectiveItem.getUrlencodedList());
        req.pathVariablesList = cloneParams(effectiveItem.getPathVariablesList());
        req.paramsList = cloneParams(effectiveItem.getParamsList());
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
                resolvePreviewAuthorizationHeaderValue(effectiveItem, variableResolver)
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
        if (hasEnabledAuthorizationHeader(headers)) {
            return headers;
        }
        HttpHeader authHeader = tryCreateResolvableAuthHeader(item, variableResolver);
        if (authHeader != null) {
            headers.add(authHeader);
        }
        return headers;
    }

    private static String resolvePreviewAuthorizationHeaderValue(HttpRequestItem item,
                                                                 Function<String, String> variableResolver) {
        if (item == null || hasEnabledAuthorizationHeader(item.getHeadersList())) {
            return null;
        }
        HttpHeader authHeader = tryCreateResolvableAuthHeader(item, variableResolver);
        return authHeader != null ? authHeader.getValue() : null;
    }

    private static boolean hasEnabledAuthorizationHeader(List<HttpHeader> headers) {
        if (headers == null || headers.isEmpty()) {
            return false;
        }
        return headers.stream()
                .anyMatch(h -> h != null && h.isEnabled() && HEADER_AUTHORIZATION.equalsIgnoreCase(h.getKey()));
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
