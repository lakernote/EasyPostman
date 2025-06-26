package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.EnvironmentService;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
public class HttpRequestExecutor {

    // Cookie 管理器
    private static final CookieManager COOKIE_MANAGER = new CookieManager();

    public static void registerCookieChangeListener(Runnable listener) {
        COOKIE_MANAGER.registerListener(listener);
    }

    public static void unregisterCookieChangeListener(Runnable listener) {
        COOKIE_MANAGER.unregisterListener(listener);
    }

    public static String getCookieHeader(String host) {
        return COOKIE_MANAGER.getCookieHeader(host);
    }

    public static void setCookies(String host, List<String> setCookieHeaders) {
        COOKIE_MANAGER.setCookies(host, setCookieHeaders);
    }

    public static Map<String, Map<String, String>> getAllCookies() {
        return COOKIE_MANAGER.getAllCookies();
    }


    public static HttpResponse execute(PreparedRequest req) throws Exception {
        HttpResponse resp = sendRequestByType(req);
        handleSetCookie(resp, req.url);
        return resp;
    }

    // 以下为工具方法，供 PreparedRequestBuilder/RedirectHandler 复用
    public static String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) return url;
        StringBuilder sb = new StringBuilder(url);
        boolean hasQuestionMark = url.contains("?");
        Set<String> urlParamKeys = extractUrlParamKeys(url, hasQuestionMark);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (urlParamKeys.contains(entry.getKey())) continue;
            if (hasQuestionMark) {
                sb.append("&");
            } else {
                sb.append("?");
                hasQuestionMark = true;
            }
            sb.append(HttpUtil.encodeURIComponent(entry.getKey()))
                    .append("=")
                    .append(HttpUtil.encodeURIComponent(entry.getValue()));
        }
        return sb.toString();
    }

    static Set<String> extractUrlParamKeys(String url, boolean hasQuestionMark) {
        Set<String> urlParamKeys = new LinkedHashSet<>();
        if (hasQuestionMark) {
            String paramStr = url.substring(url.indexOf('?') + 1);
            String[] pairs = paramStr.split("&");
            for (String pair : pairs) {
                int eqIdx = pair.indexOf('=');
                if (eqIdx > 0) {
                    String k = pair.substring(0, eqIdx);
                    urlParamKeys.add(k);
                } else if (!pair.isEmpty()) {
                    urlParamKeys.add(pair);
                }
            }
        }
        return urlParamKeys;
    }

    static String encodeUrlParams(String url) {
        if (url == null || !url.contains("?")) return url;
        int idx = url.indexOf('?');
        String baseUrl = url.substring(0, idx);
        String paramStr = url.substring(idx + 1);
        StringBuilder sb = new StringBuilder(baseUrl);
        sb.append("?");
        String[] pairs = paramStr.split("&");
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            int eqIdx = pair.indexOf('=');
            if (eqIdx > 0) {
                String k = pair.substring(0, eqIdx);
                String v = pair.substring(eqIdx + 1);
                sb.append(HttpUtil.encodeURIComponent(k)).append("=").append(HttpUtil.encodeURIComponent(v));
            } else {
                sb.append(HttpUtil.encodeURIComponent(pair));
            }
            if (i < pairs.length - 1) sb.append("&");
        }
        return sb.toString();
    }

    static void addContentTypeHeader(Map<String, String> headers, HttpRequestItem item) {
        boolean hasContentType = headers.keySet().stream().anyMatch("Content-Type"::equalsIgnoreCase);
        if (!hasContentType) {
            if (item.getFormData() != null && !item.getFormData().isEmpty()) {
                headers.put("Content-Type", "multipart/form-data");
            } else if (item.getUrlencoded() != null && !item.getUrlencoded().isEmpty()) {
                headers.put("Content-Type", "application/x-www-form-urlencoded");
            } else if (item.getBody() != null && !item.getBody().isEmpty()) {
                headers.put("Content-Type", "application/json");
            }
        }
    }

    static void addAuthorization(Map<String, String> headers, HttpRequestItem item) {
        String authType = item.getAuthType();
        if ("basic".equals(authType)) {
            String username = EnvironmentService.replaceVariables(item.getAuthUsername());
            String password = EnvironmentService.replaceVariables(item.getAuthPassword());
            if (!headers.containsKey("Authorization") && username != null) {
                String token = Base64.getEncoder().encodeToString((username + ":" + (password == null ? "" : password)).getBytes());
                headers.put("Authorization", "Basic " + token);
            }
        } else if ("bearer".equals(authType)) {
            String token = EnvironmentService.replaceVariables(item.getAuthToken());
            if (!headers.containsKey("Authorization") && token != null && !token.isEmpty()) {
                headers.put("Authorization", "Bearer " + token);
            }
        }
    }

    static void addCookieHeaderIfNeeded(String url, Map<String, String> headers) {
        try {
            URL urlObj = new URL(url);
            String host = getHost(urlObj);
            String cookieHeader = getCookieHeader(host);
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                headers.put("Cookie", cookieHeader);
            }
        } catch (Exception exception) {
            log.error("", exception);
        }
    }

    static HttpResponse sendRequestByType(PreparedRequest req) throws Exception {
        if (req.isMultipart) {
            return HttpService.sendRequestWithMultipart(req.url, req.method, req.headers, req.formData, req.formFiles, req.followRedirects);
        } else {
            return HttpService.sendRequest(req.url, req.method, req.headers, req.body, req.followRedirects);
        }
    }

    static void handleSetCookie(HttpResponse resp, String url) {
        try {
            URL urlObj = new URL(url);
            String host = getHost(urlObj);
            List<String> setCookieHeaders = extractSetCookieHeaders(resp);
            if (!setCookieHeaders.isEmpty()) {
                setCookies(host, setCookieHeaders);
            }
        } catch (Exception ignore) {
        }
    }

    static List<String> extractSetCookieHeaders(HttpResponse resp) {
        List<String> setCookieHeaders = new ArrayList<>();
        if (resp.headers != null) {
            for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                if (entry.getKey() != null && "Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                    setCookieHeaders.addAll(entry.getValue());
                }
            }
        }
        return setCookieHeaders;
    }

    private static String getHost(URL urlObj) {
        return urlObj.getHost();
    }
}