package com.laker.postman.service.swagger;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.*;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * Swagger/OpenAPI 格式解析器
 * 支持 Swagger 2.0 和 OpenAPI 3.0+ 格式
 */
@Slf4j
@UtilityClass
public class SwaggerParser {

    // 常量定义
    private static final String GROUP = "group";
    private static final String REQUEST = "request";

    /**
     * 解析 Swagger/OpenAPI JSON 文件，返回根节点
     *
     * @param json Swagger/OpenAPI JSON 字符串
     * @return 集合根节点，如果解析失败返回 null
     */
    public static DefaultMutableTreeNode parseSwagger(String json) {
        try {
            JSONObject swaggerRoot = JSONUtil.parseObj(json);

            // 检测版本
            String version = detectVersion(swaggerRoot);
            if (version == null) {
                log.error("无法识别的Swagger/OpenAPI格式");
                return null;
            }

            log.info("检测到 {} 格式", version);

            // 根据版本调用不同的解析方法
            if (version.startsWith("Swagger 2")) {
                return parseSwagger2(swaggerRoot);
            } else if (version.startsWith("OpenAPI 3")) {
                return parseOpenAPI3(swaggerRoot);
            }

            return null;
        } catch (Exception e) {
            log.error("解析Swagger文件失败", e);
            return null;
        }
    }

    /**
     * 检测 Swagger/OpenAPI 版本
     */
    private static String detectVersion(JSONObject root) {
        if (root.containsKey("swagger") && root.getStr("swagger").startsWith("2")) {
            return "Swagger 2.0";
        } else if (root.containsKey("openapi") && root.getStr("openapi").startsWith("3")) {
            return "OpenAPI 3.x";
        }
        return null;
    }

    /**
     * 解析 Swagger 2.0 格式
     */
    private static DefaultMutableTreeNode parseSwagger2(JSONObject swaggerRoot) {
        // 获取基本信息
        JSONObject info = swaggerRoot.getJSONObject("info");
        String title = info != null ? info.getStr("title", "Swagger API") : "Swagger API";
        String version = info != null ? info.getStr("version", "") : "";
        String collectionName = version.isEmpty() ? title : title + " (" + version + ")";

        // 创建根节点
        RequestGroup collectionGroup = new RequestGroup(collectionName);
        DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionGroup});

        // 获取基础URL
        String baseUrl = buildSwagger2BaseUrl(swaggerRoot);

        // 解析全局安全配置
        JSONObject securityDefsMap = swaggerRoot.getJSONObject("securityDefinitions");

        // 解析paths
        JSONObject paths = swaggerRoot.getJSONObject("paths");
        if (paths == null || paths.isEmpty()) {
            log.warn("Swagger文件中没有定义任何路径");
            return collectionNode;
        }

        // 按tag组织请求
        Map<String, DefaultMutableTreeNode> tagNodes = new HashMap<>();

        // 遍历所有路径
        for (String path : paths.keySet()) {
            JSONObject pathItem = paths.getJSONObject(path);

            // 遍历该路径下的所有HTTP方法
            for (String method : pathItem.keySet()) {
                if (isNotHttpMethod(method)) {
                    continue;
                }

                JSONObject operation = pathItem.getJSONObject(method);
                HttpRequestItem requestItem = parseSwagger2Operation(
                        method.toUpperCase(),
                        path,
                        operation,
                        baseUrl,
                        securityDefsMap
                );

                if (requestItem != null) {
                    // 获取tags，用于分组
                    JSONArray tags = operation.getJSONArray("tags");
                    String tag = (tags != null && !tags.isEmpty()) ? tags.getStr(0) : "Default";

                    // 创建或获取tag节点
                    DefaultMutableTreeNode tagNode = tagNodes.get(tag);
                    if (tagNode == null) {
                        RequestGroup tagGroup = new RequestGroup(tag);
                        tagNode = new DefaultMutableTreeNode(new Object[]{GROUP, tagGroup});
                        tagNodes.put(tag, tagNode);
                        collectionNode.add(tagNode);
                    }

                    DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, requestItem});
                    tagNode.add(requestNode);
                }
            }
        }

        return collectionNode;
    }

    /**
     * 解析 OpenAPI 3.x 格式
     */
    private static DefaultMutableTreeNode parseOpenAPI3(JSONObject openApiRoot) {
        // 获取基本信息
        JSONObject info = openApiRoot.getJSONObject("info");
        String title = info != null ? info.getStr("title", "OpenAPI") : "OpenAPI";
        String version = info != null ? info.getStr("version", "") : "";
        String collectionName = version.isEmpty() ? title : title + " (" + version + ")";

        // 创建根节点
        RequestGroup collectionGroup = new RequestGroup(collectionName);
        DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionGroup});

        // 获取服务器列表
        JSONArray servers = openApiRoot.getJSONArray("servers");
        String baseUrl = "";
        if (servers != null && !servers.isEmpty()) {
            baseUrl = servers.getJSONObject(0).getStr("url", "");
        }

        // 解析全局安全配置
        JSONObject components = openApiRoot.getJSONObject("components");
        JSONObject securitySchemes = null;
        if (components != null) {
            securitySchemes = components.getJSONObject("securitySchemes");
        }

        // 解析paths
        JSONObject paths = openApiRoot.getJSONObject("paths");
        if (paths == null || paths.isEmpty()) {
            log.warn("OpenAPI文件中没有定义任何路径");
            return collectionNode;
        }

        // 按tag组织请求
        Map<String, DefaultMutableTreeNode> tagNodes = new HashMap<>();

        // 遍历所有路径
        for (String path : paths.keySet()) {
            JSONObject pathItem = paths.getJSONObject(path);

            // 遍历该路径下的所有HTTP方法
            for (String method : pathItem.keySet()) {
                if (isNotHttpMethod(method)) {
                    continue;
                }

                JSONObject operation = pathItem.getJSONObject(method);
                HttpRequestItem requestItem = parseOpenAPI3Operation(
                        method.toUpperCase(),
                        path,
                        operation,
                        baseUrl,
                        securitySchemes
                );

                if (requestItem != null) {
                    // 获取tags，用于分组
                    JSONArray tags = operation.getJSONArray("tags");
                    String tag = (tags != null && !tags.isEmpty()) ? tags.getStr(0) : "Default";

                    // 创建或获取tag节点
                    DefaultMutableTreeNode tagNode = tagNodes.get(tag);
                    if (tagNode == null) {
                        RequestGroup tagGroup = new RequestGroup(tag);
                        tagNode = new DefaultMutableTreeNode(new Object[]{GROUP, tagGroup});
                        tagNodes.put(tag, tagNode);
                        collectionNode.add(tagNode);
                    }

                    DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, requestItem});
                    tagNode.add(requestNode);
                }
            }
        }

        return collectionNode;
    }

    /**
     * 构建 Swagger 2.0 的基础URL
     */
    private static String buildSwagger2BaseUrl(JSONObject swaggerRoot) {
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
     * 解析 Swagger 2.0 的单个操作
     */
    private static HttpRequestItem parseSwagger2Operation(
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

        // 设置URL
        String fullUrl = baseUrl.isEmpty() ? path : baseUrl + path;
        req.setUrl(fullUrl);

        // 解析参数
        parseSwagger2Parameters(operation, req);

        // 解析请求体
        parseSwagger2RequestBody(operation, req);

        // 解析安全配置
        parseSwagger2Security(operation, req, securityDefinitions);

        return req;
    }

    /**
     * 解析 OpenAPI 3.x 的单个操作
     */
    private static HttpRequestItem parseOpenAPI3Operation(
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

        // 设置URL
        String fullUrl = baseUrl.isEmpty() ? path : baseUrl + path;
        req.setUrl(fullUrl);

        // 解析参数
        parseOpenAPI3Parameters(operation, req);

        // 解析请求体
        parseOpenAPI3RequestBody(operation, req);

        // 解析安全配置
        parseOpenAPI3Security(operation, req, securitySchemes);

        return req;
    }

    /**
     * 解析 Swagger 2.0 的参数
     */
    private static void parseSwagger2Parameters(JSONObject operation, HttpRequestItem req) {
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
                    // Path参数直接在URL中，不需要额外处理
                    break;
                case "body":
                    // body参数在parseSwagger2RequestBody中处理
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
     * 解析 OpenAPI 3.x 的参数
     */
    private static void parseOpenAPI3Parameters(JSONObject operation, HttpRequestItem req) {
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

            // 从schema中获取默认值
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
                    // Path参数直接在URL中
                    break;
                case "cookie":
                    // Cookie参数可以作为header处理
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
     * 解析 Swagger 2.0 的请求体
     */
    private static void parseSwagger2RequestBody(JSONObject operation, HttpRequestItem req) {
        JSONArray parameters = operation.getJSONArray("parameters");
        if (parameters == null) {
            return;
        }

        for (Object paramObj : parameters) {
            JSONObject param = (JSONObject) paramObj;
            if ("body".equals(param.getStr("in"))) {
                JSONObject schema = param.getJSONObject("schema");
                if (schema != null) {
                    // 尝试生成示例JSON
                    String exampleBody = generateExampleFromSchema(schema);
                    req.setBody(exampleBody);

                    // 添加Content-Type header
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
     * 解析 OpenAPI 3.x 的请求体
     */
    private static void parseOpenAPI3RequestBody(JSONObject operation, HttpRequestItem req) {
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
                String exampleBody = generateExampleFromSchema(schema);
                req.setBody(exampleBody);

                // 添加Content-Type header
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
            // 处理multipart表单
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
     * 从schema生成示例JSON
     */
    private static String generateExampleFromSchema(JSONObject schema) {
        if (schema == null) {
            return "{}";
        }

        // 如果有example，直接使用
        if (schema.containsKey("example")) {
            Object example = schema.get("example");
            if (example instanceof JSONObject || example instanceof JSONArray) {
                return example.toString();
            }
            return JSONUtil.toJsonStr(example);
        }

        String type = schema.getStr("type", "object");

        switch (type) {
            case "object":
                JSONObject properties = schema.getJSONObject("properties");
                if (properties == null) {
                    return "{}";
                }
                JSONObject result = new JSONObject();
                for (String key : properties.keySet()) {
                    JSONObject propSchema = properties.getJSONObject(key);
                    result.set(key, generateExampleValue(propSchema));
                }
                return result.toString();

            case "array":
                JSONObject items = schema.getJSONObject("items");
                JSONArray array = new JSONArray();
                if (items != null) {
                    array.add(generateExampleValue(items));
                }
                return array.toString();

            default:
                return JSONUtil.toJsonStr(generateExampleValue(schema));
        }
    }

    /**
     * 生成示例值
     */
    private static Object generateExampleValue(JSONObject schema) {
        if (schema == null) {
            return "";
        }

        if (schema.containsKey("example")) {
            return schema.get("example");
        }

        String type = schema.getStr("type", "string");

        switch (type) {
            case "string":
                String format = schema.getStr("format", "");
                if ("date".equals(format)) {
                    return "2024-01-01";
                } else if ("date-time".equals(format)) {
                    return "2024-01-01T00:00:00Z";
                } else if ("email".equals(format)) {
                    return "user@example.com";
                }
                return "string";

            case "integer":
                return 0;

            case "number":
                return 0.0;

            case "boolean":
                return false;

            case "array":
                return new JSONArray();

            case "object":
                return new JSONObject();

            default:
                return "";
        }
    }

    /**
     * 解析 Swagger 2.0 的安全配置
     */
    private static void parseSwagger2Security(JSONObject operation, HttpRequestItem req, JSONObject securityDefinitions) {
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

    /**
     * 解析 OpenAPI 3.x 的安全配置
     */
    private static void parseOpenAPI3Security(JSONObject operation, HttpRequestItem req, JSONObject securitySchemes) {
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
     * 判断是否不是HTTP方法
     */
    private static boolean isNotHttpMethod(String method) {
        return !"get".equalsIgnoreCase(method) &&
                !"post".equalsIgnoreCase(method) &&
                !"put".equalsIgnoreCase(method) &&
                !"delete".equalsIgnoreCase(method) &&
                !"patch".equalsIgnoreCase(method) &&
                !"head".equalsIgnoreCase(method) &&
                !"options".equalsIgnoreCase(method);
    }
}

