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
 * OpenAPI 3.x 格式解析器
 * 支持 OpenAPI 3.0.x 和 3.1.x
 */
@Slf4j
@UtilityClass
class OpenApi3Parser {

    /**
     * 解析 OpenAPI 3.x 格式
     *
     * @param openApiRoot OpenAPI 3.x JSON 对象
     * @return 解析结果
     */
    static CollectionParseResult parse(JSONObject openApiRoot) {
        // 获取基本信息
        JSONObject info = openApiRoot.getJSONObject("info");
        String title = info != null ? info.getStr("title", "OpenAPI") : "OpenAPI";
        String version = info != null ? info.getStr("version", "") : "";
        String collectionName = version.isEmpty() ? title : title + " (" + version + ")";

        // 创建根分组
        RequestGroup collectionGroup = new RequestGroup(collectionName);
        CollectionParseResult result = new CollectionParseResult(collectionGroup);

        // 解析 servers 并创建环境变量
        parseServersToEnvironments(openApiRoot, result, title);

        // 获取服务器列表，决定是否使用变量引用
        JSONArray servers = openApiRoot.getJSONArray("servers");
        String baseUrl = "";

        if (servers != null && !servers.isEmpty()) {
            // 如果定义了 servers，使用 {{baseUrl}} 变量引用
            baseUrl = "{{baseUrl}}";
            log.debug("检测到 {} 个服务器配置，请求URL将使用 {{{{baseUrl}}}} 变量", servers.size());
        }


        // 解析全局安全配置
        JSONObject components = openApiRoot.getJSONObject("components");
        JSONObject securitySchemes = null;
        if (components != null) {
            securitySchemes = components.getJSONObject("securitySchemes");
        }

        // 解析 paths
        JSONObject paths = openApiRoot.getJSONObject("paths");
        if (paths == null || paths.isEmpty()) {
            log.warn("OpenAPI 文件中没有定义任何路径");
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
                        securitySchemes
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
     * 解析 OpenAPI 3.x 的单个操作
     */
    private static HttpRequestItem parseOperation(
            String method,
            String path,
            JSONObject operation,
            String baseUrl,
            JSONObject securitySchemes) {

        HttpRequestItem req = new HttpRequestItem();
        req.setId(UUID.randomUUID().toString());
        req.setMethod(method);

        // 设置请求名称
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
        parseSecurity(operation, req, securitySchemes);

        return req;
    }

    /**
     * 解析 OpenAPI 3.x 的参数
     */
    private static void parseParameters(JSONObject operation, HttpRequestItem req) {
        JSONArray parameters = operation.getJSONArray("parameters");
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        List<HttpParam> queryParams = new ArrayList<>();
        List<HttpHeader> headers = new ArrayList<>();

        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            String in = param.getStr("in", "");
            String name = param.getStr("name", "");

            // 从 schema 中获取默认值
            String value = "";
            JSONObject schema = param.getJSONObject("schema");
            if (schema != null && schema.containsKey("default")) {
                value = schema.get("default").toString();
            }

            switch (in) {
                case "query":
                    queryParams.add(new HttpParam(true, name, value));
                    break;
                case "header":
                    headers.add(new HttpHeader(true, name, value));
                    break;
                case "path":
                    // Path 参数直接在 URL 中
                    break;
                case "cookie":
                    // Cookie 参数可以作为 header 处理
                    headers.add(new HttpHeader(true, "Cookie", name + "=" + value));
                    break;
            }
        }

        if (!queryParams.isEmpty()) {
            req.setParamsList(queryParams);
        }
        if (!headers.isEmpty()) {
            req.setHeadersList(headers);
        }
    }

    /**
     * 解析 OpenAPI 3.x 的请求体
     */
    private static void parseRequestBody(JSONObject operation, HttpRequestItem req) {
        JSONObject requestBody = operation.getJSONObject("requestBody");
        if (requestBody == null) {
            return;
        }

        JSONObject content = requestBody.getJSONObject("content");
        if (content == null) {
            return;
        }

        // 优先查找 application/json
        JSONObject jsonContent = content.getJSONObject("application/json");
        if (jsonContent != null) {
            JSONObject schema = jsonContent.getJSONObject("schema");
            if (schema != null) {
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
        } else if (content.containsKey("application/x-www-form-urlencoded")) {
            // 处理表单数据
            JSONObject formContent = content.getJSONObject("application/x-www-form-urlencoded");
            JSONObject schema = formContent.getJSONObject("schema");
            if (schema != null) {
                List<HttpFormUrlencoded> urlencoded = new ArrayList<>();
                JSONObject properties = schema.getJSONObject("properties");
                if (properties != null) {
                    for (String key : properties.keySet()) {
                        urlencoded.add(new HttpFormUrlencoded(true, key, ""));
                    }
                }
                req.setUrlencodedList(urlencoded);
            }
        } else if (content.containsKey("multipart/form-data")) {
            // 处理 multipart 表单
            JSONObject formContent = content.getJSONObject("multipart/form-data");
            JSONObject schema = formContent.getJSONObject("schema");
            if (schema != null) {
                List<HttpFormData> formData = new ArrayList<>();
                JSONObject properties = schema.getJSONObject("properties");
                if (properties != null) {
                    for (String key : properties.keySet()) {
                        JSONObject propSchema = properties.getJSONObject(key);
                        String format = propSchema.getStr("format", "");
                        if ("binary".equals(format)) {
                            formData.add(new HttpFormData(true, key, HttpFormData.TYPE_FILE, ""));
                        } else {
                            formData.add(new HttpFormData(true, key, HttpFormData.TYPE_TEXT, ""));
                        }
                    }
                }
                req.setFormDataList(formData);
            }
        }
    }

    /**
     * 解析 OpenAPI 3.x 的安全配置
     */
    private static void parseSecurity(JSONObject operation, HttpRequestItem req, JSONObject securitySchemes) {
        JSONArray security = operation.getJSONArray("security");
        if (security == null || security.isEmpty() || securitySchemes == null) {
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
        JSONObject securityScheme = securitySchemes.getJSONObject(securityName);
        if (securityScheme == null) {
            req.setAuthType(AUTH_TYPE_NONE);
            return;
        }

        String type = securityScheme.getStr("type", "");
        switch (type) {
            case "http":
                String scheme = securityScheme.getStr("scheme", "");
                if ("basic".equals(scheme)) {
                    req.setAuthType(AUTH_TYPE_BASIC);
                    req.setAuthUsername("");
                    req.setAuthPassword("");
                } else if ("bearer".equals(scheme)) {
                    req.setAuthType(AUTH_TYPE_BEARER);
                    req.setAuthToken("");
                }
                break;

            case "apiKey":
                String in = securityScheme.getStr("in", "");
                String name = securityScheme.getStr("name", "");
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
            case "openIdConnect":
                req.setAuthType(AUTH_TYPE_BEARER);
                req.setAuthToken("");
                break;
        }
    }

    /**
     * 解析 servers 并创建环境变量
     */
    private static void parseServersToEnvironments(JSONObject openApiRoot, CollectionParseResult result, String apiTitle) {
        JSONArray servers = openApiRoot.getJSONArray("servers");
        if (servers == null || servers.isEmpty()) {
            log.debug("OpenAPI 文件中没有定义 servers，跳过环境变量创建");
            return;
        }

        log.info("解析到 {} 个服务器配置，将创建对应的环境变量", servers.size());

        for (int i = 0; i < servers.size(); i++) {
            JSONObject server = servers.getJSONObject(i);
            String url = server.getStr("url", "");
            String description = server.getStr("description", "");

            if (url.isEmpty()) {
                continue;
            }

            // 创建环境名称
            String envName;
            if (!description.isEmpty()) {
                envName = apiTitle + " - " + description;
            } else {
                envName = apiTitle + " - Server " + (i + 1);
            }

            // 创建环境
            Environment env = new Environment(envName);
            env.setId(UUID.randomUUID().toString());

            // 添加 baseUrl 变量
            env.addVariable("baseUrl", url);

            // 解析 server variables（如果有）
            JSONObject variables = server.getJSONObject("variables");
            if (variables != null && !variables.isEmpty()) {
                for (String varName : variables.keySet()) {
                    JSONObject varObj = variables.getJSONObject(varName);
                    String defaultValue = varObj.getStr("default", "");
                    env.addVariable(varName, defaultValue);
                }
            }

            result.addEnvironment(env);
            log.debug("创建环境: {} -> {}", envName, url);
        }
    }
}
