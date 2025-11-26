package com.laker.postman.service.http;

import com.laker.postman.model.*;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 负责构建 PreparedRequest
 */
@UtilityClass
public class PreparedRequestBuilder {

    public static PreparedRequest build(HttpRequestItem item) {
        PreparedRequest req = new PreparedRequest();
        req.id = item.getId();
        req.method = item.getMethod();
        // Build headers map from headersList
        Map<String, String> headers = new LinkedHashMap<>();
        if (item.getHeadersList() != null) {
            for (HttpHeader header : item.getHeadersList()) {
                if (header.isEnabled()) {
                    headers.put(header.getKey(), header.getValue());
                }
            }
        }
        // 拼接 params 到 url，但暂不替换变量
        String urlString = HttpRequestUtil.buildUrlWithParams(item.getUrl(), item.getParams());
        req.url = HttpRequestUtil.encodeUrlParams(urlString); // 暂不替换变量
        HttpRequestUtil.addAuthorization(headers, item);
        req.headers = headers; // 暂不替换变量
        req.body = item.getBody(); // 暂不替换变量
        req.bodyType = item.getBodyType();
        req.urlencoded = item.getUrlencoded(); // 暂不替换变量
        boolean hasFormData = item.getFormData() != null && !item.getFormData().isEmpty();
        boolean hasFormFiles = item.getFormFiles() != null && !item.getFormFiles().isEmpty();
        req.isMultipart = hasFormData || hasFormFiles;
        if (req.isMultipart) {
            req.formData = item.getFormData(); // 暂不替换变量
            req.formFiles = item.getFormFiles(); // 暂不替换变量
        }
        req.followRedirects = SettingManager.isFollowRedirects();

        // 填充 List 数据，支持相同 key
        req.headersList = item.getHeadersList();
        req.formDataList = item.getFormDataList();
        req.urlencodedList = item.getUrlencodedList();

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

        // 替换 List 中的变量，支持相同 key
        replaceVariablesInHeadersList(req.headersList);
        replaceVariablesInFormDataList(req.formDataList);
        replaceVariablesInUrlencodedList(req.urlencodedList);
    }

    private static Map<String, String> replaceVariables(Map<String, String> headers) {
        if (headers == null) return new LinkedHashMap<>();
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

    private static void replaceVariablesInHeadersList(List<HttpHeader> list) {
        if (list == null) return;
        for (HttpHeader item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInFormDataList(List<HttpFormData> list) {
        if (list == null) return;
        for (HttpFormData item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }

    private static void replaceVariablesInUrlencodedList(List<HttpFormUrlencoded> list) {
        if (list == null) return;
        for (HttpFormUrlencoded item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }
}