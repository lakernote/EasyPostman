package com.laker.postman.service.http;

import com.laker.postman.model.*;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;

/**
 * 负责构建 PreparedRequest
 */
@UtilityClass
public class PreparedRequestBuilder {

    public static PreparedRequest build(HttpRequestItem item) {
        PreparedRequest req = new PreparedRequest();
        req.id = item.getId();
        req.method = item.getMethod();

        // 拼接 params 到 url，但暂不替换变量
        // Build params map from paramsList
        Map<String, String> params = new LinkedHashMap<>();
        if (item.getParamsList() != null) {
            for (HttpParam param : item.getParamsList()) {
                if (param.isEnabled()) {
                    params.put(param.getKey(), param.getValue());
                }
            }
        }
        String urlString = HttpRequestUtil.buildUrlWithParams(item.getUrl(), params);
        req.url = HttpRequestUtil.encodeUrlParams(urlString); // 暂不替换变量

        req.body = item.getBody(); // 暂不替换变量
        req.bodyType = item.getBodyType();

        // 根据 formDataList 判断是否是 multipart
        boolean hasFormData = false;
        boolean hasFormFiles = false;
        if (item.getFormDataList() != null) {
            for (HttpFormData data : item.getFormDataList()) {
                if (data.isEnabled()) {
                    if (data.isText()) {
                        hasFormData = true;
                    } else if (data.isFile()) {
                        hasFormFiles = true;
                    }
                }
            }
        }
        req.isMultipart = hasFormData || hasFormFiles;
        req.followRedirects = SettingManager.isFollowRedirects();

        // 填充 List 数据，支持相同 key
        // 先复制原始 headersList，然后添加认证头
        req.headersList = buildHeadersListWithAuth(item);
        req.formDataList = item.getFormDataList();
        req.urlencodedList = item.getUrlencodedList();
        req.paramsList = item.getParamsList();

        return req;
    }

    /**
     * 构建包含认证信息的 headersList
     * 如果配置了认证，会自动添加 Authorization 头（如果不存在）
     */
    private static List<HttpHeader> buildHeadersListWithAuth(HttpRequestItem item) {
        List<HttpHeader> headersList = new ArrayList<>();

        // 复制原始的 headers
        if (item.getHeadersList() != null) {
            headersList.addAll(item.getHeadersList());
        }

        // 检查是否已有 Authorization 头
        boolean hasAuthHeader = false;
        if (item.getHeadersList() != null) {
            for (HttpHeader header : item.getHeadersList()) {
                if (header.isEnabled() && "Authorization".equalsIgnoreCase(header.getKey())) {
                    hasAuthHeader = true;
                    break;
                }
            }
        }

        // 如果没有 Authorization 头，根据 authType 添加
        if (!hasAuthHeader) {
            String authType = item.getAuthType();
            if (AUTH_TYPE_BASIC.equals(authType)) {
                String username = EnvironmentService.replaceVariables(item.getAuthUsername());
                String password = EnvironmentService.replaceVariables(item.getAuthPassword());
                if (username != null) {
                    String token = java.util.Base64.getEncoder().encodeToString(
                            (username + ":" + (password == null ? "" : password)).getBytes()
                    );
                    HttpHeader authHeader = new HttpHeader();
                    authHeader.setKey("Authorization");
                    authHeader.setValue("Basic " + token);
                    authHeader.setEnabled(true);
                    headersList.add(authHeader);
                }
            } else if (AUTH_TYPE_BEARER.equals(authType)) {
                String token = EnvironmentService.replaceVariables(item.getAuthToken());
                if (token != null && !token.isEmpty()) {
                    HttpHeader authHeader = new HttpHeader();
                    authHeader.setKey("Authorization");
                    authHeader.setValue("Bearer " + token);
                    authHeader.setEnabled(true);
                    headersList.add(authHeader);
                }
            }
        }

        return headersList;
    }

    /**
     * 在前置脚本执行后，替换所有变量占位符
     */
    public static void replaceVariablesAfterPreScript(PreparedRequest req) {
        // 替换 List 中的变量，支持相同 key
        replaceVariablesInHeadersList(req.headersList);
        replaceVariablesInFormDataList(req.formDataList);
        replaceVariablesInUrlencodedList(req.urlencodedList);
        replaceVariablesInParamsList(req.paramsList);

        // 重新构建 URL（包含脚本动态添加的 params）
        rebuildUrlWithParams(req);

        // 替换URL中的变量
        req.url = EnvironmentService.replaceVariables(req.url);

        // 替换Body中的变量
        req.body = EnvironmentService.replaceVariables(req.body);
    }

    /**
     * 重新构建 URL，包含脚本中动态添加的 params
     * buildUrlWithParams 会自动避免重复的 key
     */
    private static void rebuildUrlWithParams(PreparedRequest req) {
        if (req.paramsList == null || req.paramsList.isEmpty()) return;

        // 提取所有启用的 params 到 Map（脚本可能添加了新的 params）
        Map<String, String> params = new LinkedHashMap<>();
        for (HttpParam param : req.paramsList) {
            if (param.isEnabled()) {
                params.put(param.getKey(), param.getValue());
            }
        }

        // 重新构建 URL（buildUrlWithParams 会自动避免重复的 key）
        if (!params.isEmpty()) {
            req.url = HttpRequestUtil.buildUrlWithParams(req.url, params);
        }
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

    private static void replaceVariablesInParamsList(List<HttpParam> list) {
        if (list == null) return;
        for (HttpParam item : list) {
            if (item.isEnabled()) {
                item.setKey(EnvironmentService.replaceVariables(item.getKey()));
                item.setValue(EnvironmentService.replaceVariables(item.getValue()));
            }
        }
    }
}