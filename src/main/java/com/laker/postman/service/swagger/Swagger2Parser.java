package com.laker.postman.service.swagger;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.laker.postman.model.*;
import com.laker.postman.service.common.CollectionNode;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.NodeType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Swagger 2.0 格式解析器
 */
@Slf4j
@UtilityClass
class Swagger2Parser {

    /**
     * 解析 Swagger 2.0 格式
     *
     * @param swaggerRoot Swagger 2.0 JSON 对象
     * @return 解析结果
     */
    static CollectionParseResult parse(JSONObject swaggerRoot) {
        // 获取基本信息
        JSONObject info = swaggerRoot.getJSONObject("info");
        String title = info != null ? info.getStr("title", "Swagger API") : "Swagger API";
        String version = info != null ? info.getStr("version", "") : "";
        String collectionName = version.isEmpty() ? title : title + " (" + version + ")";

        // 创建根分组
        RequestGroup collectionGroup = new RequestGroup(collectionName);
        CollectionParseResult result = new CollectionParseResult(collectionGroup);

        // 获取基础URL
        String baseUrl = buildBaseUrl(swaggerRoot);

        // 解析全局安全配置
        JSONObject securityDefsMap = swaggerRoot.getJSONObject("securityDefinitions");

        // 解析 paths
        JSONObject paths = swaggerRoot.getJSONObject("paths");
        if (paths == null || paths.isEmpty()) {
            log.warn("Swagger 2.0 文件中没有定义任何路径");
            return result;
        }

        // 按 tag 组织请求
        Map<String, CollectionNode> tagNodes = new HashMap<>();

        // 遍历所有路径
        for (String path : paths.keySet()) {
            JSONObject pathItem = paths.getJSONObject(path);

            // 遍历该路径下的所有 HTTP 方法
            for (String method : pathItem.keySet()) {
                if (SwaggerCommonUtil.isNotHttpMethod(method)) {
                    continue;
                }

                JSONObject operation = pathItem.getJSONObject(method);
                HttpRequestItem requestItem = parseOperation(
                        method.toUpperCase(),
                        path,
                        operation,
                        baseUrl,
                        securityDefsMap
                );

                if (requestItem != null) {
                    // 获取 tags，用于分组
                    String tag = getTag(operation);

                    // 创建或获取 tag 节点
                    CollectionNode tagNode = getOrCreateTagNode(tagNodes, tag);
                    tagNode.addChild(new CollectionNode(NodeType.REQUEST, requestItem));
                }
            }
        }

        // 添加所有 tag 节点到结果
        tagNodes.values().forEach(result::addChild);

        return result;
    }

    /**
     * 构建 Swagger 2.0 的基础 URL
     */
    private static String buildBaseUrl(JSONObject swaggerRoot) {
        String scheme = "http";
        JSONArray schemes = swaggerRoot.getJSONArray("schemes");
        if (schemes != null && !schemes.isEmpty()) {
            scheme = schemes.getStr(0);
        }

        String host = swaggerRoot.getStr("host", "");
        String basePath = swaggerRoot.getStr("basePath", "");

        if (host.isEmpty()) {
            return "";
        }

        StringBuilder baseUrl = new StringBuilder(scheme);
        baseUrl.append("://").append(host);
        if (!basePath.isEmpty() && !"/".equals(basePath)) {
            if (!basePath.startsWith("/")) {
                baseUrl.append("/");
            }
            baseUrl.append(basePath);
        }

        return baseUrl.toString();
    }

    /**
     * 获取 tag 名称
     */
    private static String getTag(JSONObject operation) {
        JSONArray tags = operation.getJSONArray("tags");
        return (tags != null && !tags.isEmpty()) ? tags.getStr(0) : "Default";
    }

    /**
     * 获取或创建 tag 节点
     */
    private static CollectionNode getOrCreateTagNode(Map<String, CollectionNode> tagNodes, String tag) {
        return tagNodes.computeIfAbsent(tag, k -> {
            RequestGroup group = new RequestGroup(k);
            return new CollectionNode(NodeType.GROUP, group);
        });
    }

    /**
     * 解析 Swagger 2.0 的单个操作
     */
    private static HttpRequestItem parseOperation(
            String method,
            String path,
            JSONObject operation,
            String baseUrl,
            JSONObject securityDefinitions) {

        HttpRequestItem req = new HttpRequestItem();
        req.setId(UUID.randomUUID().toString());
        req.setMethod(method);

        // 设置请求名称和描述
        String summary = operation.getStr("summary", "");
        String operationId = operation.getStr("operationId", "");
        String requestName = summary;
        if (requestName.isEmpty()) {
            requestName = operationId.isEmpty() ? method + " " + path : operationId;
        }
        req.setName(requestName);

        // 设置 URL
        String fullUrl = baseUrl.isEmpty() ? path : baseUrl + path;
        req.setUrl(fullUrl);

        // 解析参数
        parseParameters(operation, req);

        // 解析请求体
        parseRequestBody(operation, req);

        // 解析安全配置
        parseSecurity(operation, req, securityDefinitions);

        return req;
    }

    /**
     * 解析 Swagger 2.0 的参数
     */
    private static void parseParameters(JSONObject operation, HttpRequestItem req) {
        JSONArray parameters = operation.getJSONArray("parameters");
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        List<HttpParam> queryParams = new ArrayList<>();
        List<HttpHeader> headers = new ArrayList<>();
        List<HttpFormData> formData = new ArrayList<>();
        List<HttpFormUrlencoded> urlencoded = new ArrayList<>();

        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            String in = param.getStr("in", "");
            String name = param.getStr("name", "");
            Object defaultValue = param.get("default");
            String value = defaultValue != null ? defaultValue.toString() : "";

            switch (in) {
                case "query":
                    queryParams.add(new HttpParam(true, name, value));
                    break;
                case "header":
                    headers.add(new HttpHeader(true, name, value));
                    break;
                case "formData":
                    String type = param.getStr("type", "");
                    if ("file".equals(type)) {
                        formData.add(new HttpFormData(true, name, HttpFormData.TYPE_FILE, ""));
                    } else {
                        urlencoded.add(new HttpFormUrlencoded(true, name, value));
                    }
                    break;
                case "path":
                    // Path 参数直接在 URL 中，不需要额外处理
                    break;
                case "body":
                    // body 参数在 parseRequestBody 中处理
                    break;
            }
        }

        if (!queryParams.isEmpty()) {
            req.setParamsList(queryParams);
        }
        if (!headers.isEmpty()) {
            req.setHeadersList(headers);
        }
        if (!formData.isEmpty()) {
            req.setFormDataList(formData);
        }
        if (!urlencoded.isEmpty()) {
            req.setUrlencodedList(urlencoded);
        }
    }

    /**
     * 解析 Swagger 2.0 的请求体
     */
    private static void parseRequestBody(JSONObject operation, HttpRequestItem req) {
        JSONArray parameters = operation.getJSONArray("parameters");
        if (parameters == null) {
            return;
        }

        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            if ("body".equals(param.getStr("in"))) {
                JSONObject schema = param.getJSONObject("schema");
                if (schema != null) {
                    // 生成示例 JSON
                    String exampleBody = SwaggerCommonUtil.generateExampleFromSchema(schema);
                    req.setBody(exampleBody);

                    // 添加 Content-Type header
                    List<HttpHeader> headers = req.getHeadersList();
                    if (headers == null) {
                        headers = new ArrayList<>();
                    }
                    headers.add(new HttpHeader(true, "Content-Type", "application/json"));
                    req.setHeadersList(headers);
                }
                break;
            }
        }
    }

    /**
     * 解析 Swagger 2.0 的安全配置
     */
    private static void parseSecurity(JSONObject operation, HttpRequestItem req, JSONObject securityDefinitions) {
        JSONArray security = operation.getJSONArray("security");
        if (security == null || security.isEmpty() || securityDefinitions == null) {
            req.setAuthType(AUTH_TYPE_NONE);
            return;
        }

        // 获取第一个安全定义
        JSONObject firstSecurity = security.getJSONObject(0);
        if (firstSecurity == null || firstSecurity.isEmpty()) {
            req.setAuthType(AUTH_TYPE_NONE);
            return;
        }

        String securityName = firstSecurity.keySet().iterator().next();
        JSONObject securityDef = securityDefinitions.getJSONObject(securityName);
        if (securityDef == null) {
            req.setAuthType(AUTH_TYPE_NONE);
            return;
        }

        String type = securityDef.getStr("type", "");
        switch (type) {
            case "basic":
                req.setAuthType(AUTH_TYPE_BASIC);
                req.setAuthUsername("");
                req.setAuthPassword("");
                break;

            case "apiKey":
                String in = securityDef.getStr("in", "");
                String name = securityDef.getStr("name", "");
                if ("header".equals(in)) {
                    if ("Authorization".equalsIgnoreCase(name)) {
                        req.setAuthType(AUTH_TYPE_BEARER);
                        req.setAuthToken("");
                    } else {
                        List<HttpHeader> headers = req.getHeadersList();
                        if (headers == null) {
                            headers = new ArrayList<>();
                        }
                        headers.add(new HttpHeader(true, name, ""));
                        req.setHeadersList(headers);
                    }
                }
                break;

            case "oauth2":
                req.setAuthType(AUTH_TYPE_BEARER);
                req.setAuthToken("");
                break;
        }
    }
}
