package com.laker.postman.service.http;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.EnvironmentService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 负责构建 PreparedRequest
 */
public class PreparedRequestBuilder {
    public static PreparedRequest build(HttpRequestItem item) {
        PreparedRequest req = new PreparedRequest();
        req.method = item.getMethod();
        Map<String, String> headers = item.getHeaders() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getHeaders());
        // 拼接 params 到 url
        String urlString = HttpRequestUtil.buildUrlWithParams(item.getUrl(), item.getParams());
        req.url = HttpRequestUtil.encodeUrlParams(EnvironmentService.replaceVariables(urlString));
        HttpRequestUtil.addContentTypeHeader(headers, item);
        HttpRequestUtil.addAuthorization(headers, item);
        HttpRequestUtil.addCookieHeaderIfNeeded(req.url, headers);
        req.headers = replaceVariables(headers);
        req.body = EnvironmentService.replaceVariables(item.getBody());
        req.urlencoded = replaceVariables(item.getUrlencoded());
        req.isMultipart = item.getFormData() != null && !item.getFormData().isEmpty();
        if (req.isMultipart) {
            req.formData = replaceVariables(item.getFormData());
            req.formFiles = replaceVariables(item.getFormFiles());
        }
        req.followRedirects = item.isFollowRedirects != null ? item.isFollowRedirects : true;
        return req;
    }

    private static Map<String, String> replaceVariables(Map<String, String> headers) {
        if (headers == null) return null;
        Map<String, String> processedHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            String processedKey = EnvironmentService.replaceVariables(key);
            String processedValue = EnvironmentService.replaceVariables(value);
            processedHeaders.put(processedKey, processedValue);
        }
        return processedHeaders;
    }
}