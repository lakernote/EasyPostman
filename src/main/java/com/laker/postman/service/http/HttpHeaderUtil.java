package com.laker.postman.service.http;

import com.laker.postman.service.EnvironmentService;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求头处理工具类
 */
public class HttpHeaderUtil {
    public static Map<String, String> processHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        Map<String, String> processedHeaders = new HashMap<>();
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