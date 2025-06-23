package com.laker.postman.util;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.HttpService;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.*;

@Slf4j
public class HttpRequestExecutor {

    // Cookie管理：host -> cookieName -> cookieValue
    private static final Map<String, Map<String, String>> COOKIE_STORE = new HashMap<>();

    // Cookie变更监听
    private static final List<Runnable> COOKIE_CHANGE_LISTENERS = new ArrayList<>();

    public static void registerCookieChangeListener(Runnable listener) {
        if (listener != null && !COOKIE_CHANGE_LISTENERS.contains(listener)) {
            COOKIE_CHANGE_LISTENERS.add(listener);
        }
    }

    public static void unregisterCookieChangeListener(Runnable listener) {
        COOKIE_CHANGE_LISTENERS.remove(listener);
    }

    private static void notifyCookieChange() {
        for (Runnable r : COOKIE_CHANGE_LISTENERS) {
            try {
                r.run();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * 获取指定host的cookie字符串
     */
    public static String getCookieHeader(String host) {
        Map<String, String> cookies = COOKIE_STORE.get(host);
        if (cookies == null || cookies.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            if (!sb.isEmpty()) sb.append("; ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 设置指定host的cookie
     */
    public static void setCookies(String host, List<String> setCookieHeaders) {
        if (host == null || setCookieHeaders == null) return;
        Map<String, String> cookies = COOKIE_STORE.computeIfAbsent(host, k -> new HashMap<>());
        for (String header : setCookieHeaders) {
            String[] parts = header.split(";");
            if (parts.length > 0) {
                String[] kv = parts[0].split("=", 2);
                if (kv.length == 2) {
                    cookies.put(kv[0].trim(), kv[1].trim());
                }
            }
        }
        notifyCookieChange();
    }

    /**
     * 获取所有cookie（用于UI展示）
     */
    public static Map<String, Map<String, String>> getAllCookies() {
        return COOKIE_STORE;
    }

    /**
     * 构建通用请求参数，适用于单发、批量、压测等
     */
    public static PreparedRequest buildPreparedRequest(HttpRequestItem item) {
        PreparedRequest req = new PreparedRequest();
        req.method = item.getMethod();
        Map<String, String> headers = item.getHeaders() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getHeaders());
        // 拼接 params 到 url
        String urlString = buildUrlWithParams(item.getUrl(), item.getParams());
        req.url = encodeUrlParams(EnvironmentService.replaceVariables(urlString));
        addContentTypeHeader(headers, item);
        addAuthorization(headers, item);
        // 自动加Cookie
        try {
            URL urlObj = new java.net.URL(req.url);
            String host = urlObj.getHost();
            String cookieHeader = getCookieHeader(host);
            if (cookieHeader != null && !cookieHeader.isEmpty()) {
                headers.put("Cookie", cookieHeader);
            }
        } catch (Exception exception) {
            log.error("", exception);
        }
        req.headers = HttpService.processHeaders(headers);
        // 判断 x-www-form-urlencoded
        boolean isFormUrlencoded = false;
        for (String key : req.headers.keySet()) {
            if ("Content-Type".equalsIgnoreCase(key) &&
                    req.headers.get(key) != null &&
                    req.headers.get(key).toLowerCase().contains("application/x-www-form-urlencoded")) {
                isFormUrlencoded = true;
                break;
            }
        }
        if (isFormUrlencoded && item.getFormData() == null && item.getFormFiles() == null) {
            // 解析 body 为 k=v&k2=v2 结构，转为编码后字符串
            String body = item.getBody();
            if (body != null && !body.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                String[] pairs = body.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf('=');
                    if (idx > 0) {
                        String k = pair.substring(0, idx);
                        String v = pair.substring(idx + 1);
                        if (!sb.isEmpty()) sb.append("&");
                        sb.append(HttpUtil.encodeURIComponent(k)).append("=").append(HttpUtil.encodeURIComponent(v));
                    } else if (!pair.isEmpty()) {
                        if (!sb.isEmpty()) sb.append("&");
                        sb.append(HttpUtil.encodeURIComponent(pair)).append("=");
                    }
                }
                req.body = sb.toString();
            } else {
                req.body = "";
            }
        } else {
            req.body = EnvironmentService.replaceVariables(item.getBody());
        }
        req.isMultipart = item.getFormData() != null && !item.getFormData().isEmpty();
        if (req.isMultipart) {
            req.formData = item.getFormData();
            req.formFiles = item.getFormFiles();
        }
        // 新增：默认自动重定向，后续可由UI赋值
        req.followRedirects = item.isFollowRedirects != null ? item.isFollowRedirects : true;
        return req;
    }

    public static HttpResponse execute(PreparedRequest req) throws Exception {
        long start = System.currentTimeMillis();
        HttpResponse resp;
        if (req.isMultipart) {
            resp = HttpService.sendRequestWithMultipart(req.url, req.method, req.headers, req.formData, req.formFiles, req.followRedirects);
        } else {
            resp = HttpService.sendRequest(req.url, req.method, req.headers, req.body, req.followRedirects);
        }
        resp.costMs = System.currentTimeMillis() - start;
        // 解析Set-Cookie
        try {
            URL urlObj = new URL(req.url);
            String host = urlObj.getHost();
            List<String> setCookieHeaders = new ArrayList<>();
            if (resp.headers != null) {
                for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                    if (entry.getKey() != null && "Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                        setCookieHeaders.addAll(entry.getValue());
                    }
                }
            }
            if (!setCookieHeaders.isEmpty()) {
                setCookies(host, setCookieHeaders);
            }
        } catch (Exception ignore) {
        }
        return resp;
    }

    // 拼接 params 到 url，避免重复
    public static String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) return url;
        StringBuilder sb = new StringBuilder(url);
        boolean hasQuestionMark = url.contains("?");
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

    // 对 URL 的参数部分做 encodeURIComponent 处理
    private static String encodeUrlParams(String url) {
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

    // 根据 Body 类型自动添加 Content-Type 请求头（如果用户没有手动设置）
    private static void addContentTypeHeader(Map<String, String> headers, HttpRequestItem item) {
        boolean hasContentType = false;
        for (String key : headers.keySet()) {
            if ("Content-Type".equalsIgnoreCase(key)) {
                hasContentType = true;
                break;
            }
        }
        if (!hasContentType) {
            if (item.getFormData() != null && !item.getFormData().isEmpty()) {
                headers.put("Content-Type", "multipart/form-data");
            } else if (item.getBody() != null && !item.getBody().isEmpty()) {
                headers.put("Content-Type", "application/json");
            }
        }
    }

    // 添加认证头
    private static void addAuthorization(Map<String, String> headers, HttpRequestItem item) {
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

    // 重定向信息结构体
    public static class RedirectInfo {
        public String url;
        public int statusCode;
        public Map<String, List<String>> headers;
        public String location;
        public String responseBody;
    }

    public static class ResponseWithRedirects {
        public HttpResponse finalResponse;
        public List<RedirectInfo> redirects = new ArrayList<>();
    }

    /**
     * 支持重定向链的请求执行，返回最终响应和重定向过程
     */
    public static ResponseWithRedirects executeWithRedirects(PreparedRequest req, int maxRedirects) throws Exception {
        ResponseWithRedirects result = new ResponseWithRedirects();
        String url = req.url;
        String method = req.method;
        String body = req.body;
        Map<String, String> origHeaders = req.headers;
        Map<String, String> headers = new LinkedHashMap<>(origHeaders);
        Map<String, String> formData = req.formData;
        Map<String, String> formFiles = req.formFiles;
        boolean isMultipart = req.isMultipart;
        int redirectCount = 0;
        boolean followRedirects = req.followRedirects;
        while (redirectCount <= maxRedirects) {
            HttpResponse resp;
            if (isMultipart) {
                resp = HttpService.sendRequestWithMultipart(url, method, headers, formData, formFiles, followRedirects);
            } else {
                resp = HttpService.sendRequest(url, method, headers, body, followRedirects);
            }
            // 记录本次响应
            RedirectInfo info = new RedirectInfo();
            info.url = url;
            info.statusCode = resp.code;
            info.headers = resp.headers;
            info.responseBody = resp.body;
            info.location = null;
            if (resp.headers != null) {
                for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                    if (entry.getKey() != null && "Location".equalsIgnoreCase(entry.getKey())) {
                        info.location = entry.getValue().get(0);
                        break;
                    }
                }
            }
            result.redirects.add(info);
            // 处理Set-Cookie
            try {
                java.net.URL urlObj = new java.net.URL(url);
                String host = urlObj.getHost();
                List<String> setCookieHeaders = new ArrayList<>();
                if (resp.headers != null) {
                    for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                        if (entry.getKey() != null && "Set-Cookie".equalsIgnoreCase(entry.getKey())) {
                            setCookieHeaders.addAll(entry.getValue());
                        }
                    }
                }
                if (!setCookieHeaders.isEmpty()) {
                    setCookies(host, setCookieHeaders);
                }
            } catch (Exception ignore) {
            }
            // 判断是否重定向
            if (info.statusCode >= 300 && info.statusCode < 400 && info.location != null) {
                // 计算新url
                url = info.location.startsWith("http") ? info.location : new java.net.URL(new java.net.URL(url), info.location).toString();
                redirectCount++;
                // 302/303: 跳转后method变GET，body清空，multipart清空
                if (info.statusCode == 302 || info.statusCode == 303) {
                    method = "GET";
                    body = null;
                    isMultipart = false;
                    formData = null;
                    formFiles = null;
                }
                // 307/308: method不变，body不变
                // 其余情况method不变
                // 跳转时headers需重置（去掉Content-Type/Content-Length/Host等）
                headers = new LinkedHashMap<>(origHeaders);
                headers.remove("Content-Length");
                headers.remove("Host");
                headers.remove("Content-Type");
                continue;
            } else {
                // 非重定向，返回最终响应
                result.finalResponse = resp;
                break;
            }
        }
        return result;
    }
}