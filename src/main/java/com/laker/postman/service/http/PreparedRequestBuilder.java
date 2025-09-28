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
        req.id = item.getId();
        req.method = item.getMethod();
        Map<String, String> headers = item.getHeaders() == null ? new LinkedHashMap<>() : new LinkedHashMap<>(item.getHeaders());
        // 拼接 params 到 url，但暂不替换变量
        String urlString = HttpRequestUtil.buildUrlWithParams(item.getUrl(), item.getParams());
        req.url = HttpRequestUtil.encodeUrlParams(urlString); // 暂不替换变量
        HttpRequestUtil.addAuthorization(headers, item);
        req.headers = headers; // 暂不替换变量
        req.body = item.getBody(); // 暂不替换变量
        req.bodyType = item.getBodyType();
        req.urlencoded = item.getUrlencoded(); // 暂不替换变量
        req.isMultipart = item.getFormData() != null && !item.getFormData().isEmpty();
        if (req.isMultipart) {
            req.formData = item.getFormData(); // 暂不替换变量
            req.formFiles = item.getFormFiles(); // 暂不替换变量
        }
        req.followRedirects = item.isFollowRedirects();
        return req;
    }

    /**
     * 在前置脚本执行后，替换所有变量占位符
     */
    public static void replaceVariablesAfterPreScript(PreparedRequest req) {
        // 替换URL中的变量
        req.url = EnvironmentService.replaceVariables(req.url);

        // 替换Headers中的变量
        req.headers = replaceVariables(req.headers);

        // 替换Body中的变量
        req.body = EnvironmentService.replaceVariables(req.body);

        // 替换urlencoded中的变量
        req.urlencoded = replaceVariables(req.urlencoded);

        // 替换form-data中的变量
        if (req.isMultipart) {
            req.formData = replaceVariables(req.formData);
            req.formFiles = replaceVariables(req.formFiles);
        }
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