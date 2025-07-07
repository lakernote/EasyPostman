package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.EnvironmentService;

import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class HttpRequestUtil {
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

    public static Set<String> extractUrlParamKeys(String url, boolean hasQuestionMark) {
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

    public static String encodeUrlParams(String url) {
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

    public static void addContentTypeHeader(Map<String, String> headers, HttpRequestItem item) {
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

    public static void addAuthorization(Map<String, String> headers, HttpRequestItem item) {
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

    /**
     * 提取 baseUri（协议+host+port），端口为-1时补全默认端口，确保与Chrome一致
     * 提取 URL 的基本 URI 部分（协议 + 主机 + 端口）
     *
     * @param urlString 完整的 URL 字符串
     * @return 基本 URI 字符串
     */
    public static String extractBaseUri(String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString);
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            int defaultPort = url.getDefaultPort();
            int usePort = (port == -1) ? defaultPort : port;
            String portPart = (usePort == -1 || (protocol.equals("http") && usePort == 80) || (protocol.equals("https") && usePort == 443)) ? "" : (":" + usePort);
            return protocol + "://" + host + portPart;
        } catch (Exception e) {
            return urlString; // fallback
        }
    }
}