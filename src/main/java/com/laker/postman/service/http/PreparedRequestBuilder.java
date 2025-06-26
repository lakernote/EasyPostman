package com.laker.postman.service.http;

import cn.hutool.core.map.MapUtil;
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
        String urlString = HttpRequestExecutor.buildUrlWithParams(item.getUrl(), item.getParams());
        req.url = HttpRequestExecutor.encodeUrlParams(EnvironmentService.replaceVariables(urlString));
        HttpRequestExecutor.addContentTypeHeader(headers, item);
        HttpRequestExecutor.addAuthorization(headers, item);
        HttpRequestExecutor.addCookieHeaderIfNeeded(req.url, headers);
        req.headers = HttpService.processHeaders(headers);
        // x-www-form-urlencoded 逻辑
        if (MapUtil.isNotEmpty(item.getUrlencoded())) {
            // 生成body字符串
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : item.getUrlencoded().entrySet()) {
                if (!sb.isEmpty()) sb.append("&");
                sb.append(HttpUtil.encodeURIComponent(entry.getKey()))
                        .append("=")
                        .append(HttpUtil.encodeURIComponent(entry.getValue()));
            }
            req.body = sb.toString();
        } else {
            req.body = EnvironmentService.replaceVariables(item.getBody());
        }
        req.isMultipart = item.getFormData() != null && !item.getFormData().isEmpty();
        if (req.isMultipart) {
            req.formData = item.getFormData();
            req.formFiles = item.getFormFiles();
        }
        req.followRedirects = item.isFollowRedirects != null ? item.isFollowRedirects : true;
        return req;
    }
}