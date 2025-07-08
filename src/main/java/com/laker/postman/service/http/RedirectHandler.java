package com.laker.postman.service.http;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RedirectInfo;
import com.laker.postman.panel.collections.edit.RequestEditPanel;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责处理重定向链
 */
public class RedirectHandler {
    public static HttpResponse executeWithRedirects(PreparedRequest req, int maxRedirects) throws Exception {
        String url = req.url;
        String method = req.method;
        String body = req.body;
        Map<String, String> origHeaders = req.headers;
        Map<String, String> headers = new LinkedHashMap<>(origHeaders);
        Map<String, String> formData = req.formData;
        Map<String, String> formFiles = req.formFiles;
        Map<String, String> urlencoded = req.urlencoded;
        boolean isMultipart = req.isMultipart;
        int redirectCount = 0;
        boolean followRedirects = req.followRedirects;
        URL prevUrl = new URL(url);
        while (redirectCount <= maxRedirects) {
            PreparedRequest currentReq = new PreparedRequest();
            currentReq.id = req.id;
            currentReq.url = url;
            currentReq.method = method;
            currentReq.body = body;
            currentReq.headers = headers;
            currentReq.formData = formData;
            currentReq.formFiles = formFiles;
            currentReq.isMultipart = isMultipart;
            currentReq.followRedirects = followRedirects;
            currentReq.urlencoded = urlencoded;
            currentReq.logEvent = true; // 记录事件日志
            HttpResponse resp = HttpSingleRequestExecutor.execute(currentReq);
            req.okHttpHeaders = currentReq.okHttpHeaders; // 更新请求头
            req.okHttpRequestBody = currentReq.okHttpRequestBody; // 更新真实请求体内容
            // 记录本次响应
            RedirectInfo info = new RedirectInfo();
            info.url = url;
            info.statusCode = resp.code;
            info.headers = resp.headers;
            info.responseBody = resp.body;
            info.location = extractLocationHeader(resp);
            // 判断是否重定向
            if (info.statusCode >= 300 && info.statusCode < 400 && info.location != null) {
                URL nextUrl = info.location.startsWith("http") ? new URL(info.location) : new URL(prevUrl, info.location);
                boolean isCrossDomain = !prevUrl.getHost().equalsIgnoreCase(nextUrl.getHost());
                url = nextUrl.toString();
                redirectCount++;
                // 处理 method/body
                if (info.statusCode == 301 || info.statusCode == 302 || info.statusCode == 303) {
                    method = "GET";
                    body = null;
                    isMultipart = false;
                    formData = null;
                    formFiles = null;
                    urlencoded = null;
                } else if (info.statusCode == 307 || info.statusCode == 308) {
                    // 保持原 method/body
                }
                // 处理 headers
                headers = new LinkedHashMap<>(headers); // 基于上一次请求 header
                headers.remove("Content-Length"); // 移除 Content-Length 头
                headers.remove("Host"); // 移除 Host 头
                headers.remove("Content-Type"); // 移除 Content-Type 头
                if (isCrossDomain) {
                    headers.remove("Authorization"); // 跨域请求时移除 Authorization
                    headers.remove("Cookie"); // 跨域请求时移除 Cookie
                }
                StringBuilder sb = new StringBuilder();
                sb.append("[重定向] ");
                sb.append("状态码: ").append(info.statusCode).append(", ");
                sb.append("URL: ").append(info.url);
                sb.append(", Location: ").append(info.location);
                SingletonFactory.getInstance(RequestEditPanel.class).getRequestEditSubPanel(req.id).getNetworkLogPanel().appendLog(sb.toString(), java.awt.Color.ORANGE, true);
                prevUrl = nextUrl;
            } else {
                return resp;
            }
        }
        return null;
    }

    private static String extractLocationHeader(HttpResponse resp) {
        if (resp.headers != null) {
            for (Map.Entry<String, List<String>> entry : resp.headers.entrySet()) {
                if (entry.getKey() != null && "Location".equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue().get(0);
                }
            }
        }
        return null;
    }
}