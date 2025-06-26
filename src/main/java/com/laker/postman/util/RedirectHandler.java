package com.laker.postman.util;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RedirectInfo;
import com.laker.postman.model.ResponseWithRedirects;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.HttpService;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责处理重定向链
 */
public class RedirectHandler {
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
            HttpResponse resp = isMultipart ?
                    HttpService.sendRequestWithMultipart(url, method, headers, formData, formFiles, followRedirects) :
                    HttpService.sendRequest(url, method, headers, body, followRedirects);
            // 记录本次响应
            RedirectInfo info = new RedirectInfo();
            info.url = url;
            info.statusCode = resp.code;
            info.headers = resp.headers;
            info.responseBody = resp.body;
            info.location = extractLocationHeader(resp);
            result.redirects.add(info);
            HttpRequestExecutor.handleSetCookie(resp, url);
            // 判断是否重定向
            if (info.statusCode >= 300 && info.statusCode < 400 && info.location != null) {
                url = info.location.startsWith("http") ? info.location : new URL(new URL(url), info.location).toString();
                redirectCount++;
                if (info.statusCode == 302 || info.statusCode == 303) {
                    method = "GET";
                    body = null;
                    isMultipart = false;
                    formData = null;
                    formFiles = null;
                }
                headers = new LinkedHashMap<>(origHeaders);
                headers.remove("Content-Length");
                headers.remove("Host");
                headers.remove("Content-Type");
            } else {
                result.finalResponse = resp;
                break;
            }
        }
        return result;
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