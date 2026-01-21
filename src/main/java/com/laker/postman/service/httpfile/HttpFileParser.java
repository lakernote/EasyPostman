package com.laker.postman.service.httpfile;

import com.laker.postman.model.*;
import com.laker.postman.service.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;
import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.BODY_TYPE_NONE;
import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.BODY_TYPE_FORM_URLENCODED;
import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.BODY_TYPE_RAW;

/**
 * HTTP 文件解析器
 * 负责解析 .http 文件格式（REST Client 格式），转换为内部数据结构
 */
@Slf4j
public class HttpFileParser {
    // 常量定义
    private static final String GROUP = "group";
    private static final String REQUEST = "request";

    // 匹配请求分隔符：### 开头的注释
    private static final Pattern REQUEST_SEPARATOR_PATTERN = Pattern.compile("^###\\s*(.+)$");
    // 匹配 HTTP 方法 + URL：GET/POST/PUT/DELETE/PATCH + URL
    private static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("^(GET|POST|PUT|DELETE|PATCH|HEAD|OPTIONS)\\s+(.+)$");
    // 匹配头部：Key: Value
    private static final Pattern HEADER_PATTERN = Pattern.compile("^([^:]+):\\s*(.+)$");
    // 匹配单行注释：# 开头
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^#\\s*(.*)$");

    /**
     * 私有构造函数，防止实例化
     */
    private HttpFileParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 解析 HTTP 文件，返回根节点
     *
     * @param content HTTP 文件内容
     * @return 集合根节点，如果解析失败返回 null
     */
    public static DefaultMutableTreeNode parseHttpFile(String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                log.error("HTTP文件内容为空");
                return null;
            }

            // 创建分组节点
            String groupName = "HTTP Import " + System.currentTimeMillis();
            RequestGroup collectionGroup = new RequestGroup(groupName);
            DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionGroup});

            // 按行分割
            String[] lines = content.split("\n");
            List<HttpRequestItem> requests = parseHttpRequests(lines);

            // 将解析的请求添加到树节点
            for (HttpRequestItem request : requests) {
                if (request != null && request.getUrl() != null && !request.getUrl().isEmpty()) {
                    DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, request});
                    collectionNode.add(requestNode);
                }
            }

            if (collectionNode.getChildCount() == 0) {
                log.warn("HTTP文件中没有解析到有效的请求");
                return null;
            }

            return collectionNode;
        } catch (Exception e) {
            log.error("解析HTTP文件失败", e);
            return null;
        }
    }

    /**
     * 解析 HTTP 请求列表
     */
    private static List<HttpRequestItem> parseHttpRequests(String[] lines) {
        List<HttpRequestItem> requests = new ArrayList<>();
        HttpRequestItem currentRequest = null;
        StringBuilder bodyBuilder = null;
        StringBuilder responseScriptBuilder = null;
        boolean inBody = false;
        boolean inResponseScript = false;
        String contentType = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            // 跳过空行
            if (trimmedLine.isEmpty()) {
                if (inBody && bodyBuilder != null) {
                    // 空行可能是 body 的一部分
                    bodyBuilder.append("\n");
                }
                continue;
            }

            // 跳过单行注释（# 开头，但不是 ###）
            if (COMMENT_PATTERN.matcher(trimmedLine).matches() && !trimmedLine.startsWith("###")) {
                continue;
            }

            // 检查响应处理脚本开始（> {% ... %} 格式）
            if (trimmedLine.startsWith(">") && trimmedLine.contains("{%")) {
                // 如果在 body 中遇到响应脚本，说明 body 已结束
                if (inBody) {
                    inBody = false;
                }
                // 初始化响应脚本 builder
                if (responseScriptBuilder == null) {
                    responseScriptBuilder = new StringBuilder();
                } else if (!responseScriptBuilder.isEmpty()) {
                    responseScriptBuilder.append("\n");
                }
                // 提取脚本内容（去掉 > {% 和 %}）
                String scriptLine = trimmedLine.substring(1).trim(); // 去掉 >
                if (scriptLine.startsWith("{%") && scriptLine.endsWith("%}")) {
                    // 单行脚本
                    scriptLine = scriptLine.substring(2, scriptLine.length() - 2).trim();
                    responseScriptBuilder.append(scriptLine);
                    inResponseScript = false; // 单行脚本立即结束
                } else if (scriptLine.startsWith("{%")) {
                    // 多行脚本开始
                    scriptLine = scriptLine.substring(2).trim();
                    responseScriptBuilder.append(scriptLine);
                    inResponseScript = true;
                } else {
                    // 未知格式，跳过
                    inResponseScript = false;
                }
                continue;
            }

            // 如果在响应脚本模式中，继续收集多行脚本
            if (inResponseScript) {
                if (trimmedLine.endsWith("%}")) {
                    // 脚本结束
                    String scriptLine = trimmedLine;
                    if (scriptLine.endsWith("%}")) {
                        scriptLine = scriptLine.substring(0, scriptLine.length() - 2).trim();
                    }
                    if (!scriptLine.isEmpty()) {
                        responseScriptBuilder.append("\n").append(scriptLine);
                    }
                    inResponseScript = false;
                } else {
                    // 继续收集脚本内容
                    responseScriptBuilder.append("\n").append(trimmedLine);
                }
                continue;
            }

            // 检查是否是请求分隔符（### 开头的注释）
            Matcher separatorMatcher = REQUEST_SEPARATOR_PATTERN.matcher(trimmedLine);
            if (separatorMatcher.matches()) {
                // 保存上一个请求
                if (currentRequest != null) {
                    finishRequest(currentRequest, bodyBuilder, contentType);
                    // 处理响应脚本
                    if (responseScriptBuilder != null && !responseScriptBuilder.isEmpty()) {
                        String convertedScript = convertResponseScriptToPostman(responseScriptBuilder.toString());
                        currentRequest.setPostscript(convertedScript);
                    }
                    requests.add(currentRequest);
                }
                // 开始新请求
                String requestName = separatorMatcher.group(1).trim();
                currentRequest = new HttpRequestItem();
                currentRequest.setId(UUID.randomUUID().toString());
                currentRequest.setName(requestName.isEmpty() ? "未命名请求" : requestName);
                bodyBuilder = new StringBuilder();
                responseScriptBuilder = new StringBuilder();
                inBody = false;
                contentType = null;
                continue;
            }

            // 检查是否是 HTTP 方法行（如果没有当前请求，自动创建一个）
            Matcher methodMatcher = HTTP_METHOD_PATTERN.matcher(trimmedLine);
            if (methodMatcher.matches()) {
                // 如果还没有当前请求，先创建一个（支持没有 ### 分隔符的情况）
                if (currentRequest == null) {
                    currentRequest = new HttpRequestItem();
                    currentRequest.setId(UUID.randomUUID().toString());
                    currentRequest.setName("未命名请求");
                    bodyBuilder = new StringBuilder();
                    responseScriptBuilder = new StringBuilder();
                    inBody = false;
                    inResponseScript = false;
                    contentType = null;
                }
                String method = methodMatcher.group(1).toUpperCase();
                String url = methodMatcher.group(2).trim();
                currentRequest.setMethod(method);
                currentRequest.setUrl(url);
                continue;
            }

            // 如果没有当前请求，跳过
            if (currentRequest == null) {
                continue;
            }

            // 检查是否是 Content-Type 头部（可能影响 body 解析）
            if (trimmedLine.toLowerCase().startsWith("content-type:")) {
                Matcher contentTypeMatcher = HEADER_PATTERN.matcher(trimmedLine);
                if (contentTypeMatcher.matches()) {
                    contentType = contentTypeMatcher.group(2).trim().toLowerCase();
                    if (currentRequest.getHeadersList() == null) {
                        currentRequest.setHeadersList(new ArrayList<>());
                    }
                    currentRequest.getHeadersList().add(new HttpHeader(true, "Content-Type", contentTypeMatcher.group(2).trim()));
                    continue;
                }
            }

            // 检查是否是头部行
            Matcher headerMatcher = HEADER_PATTERN.matcher(trimmedLine);
            if (headerMatcher.matches() && !inBody) {
                String headerName = headerMatcher.group(1).trim();
                String headerValue = headerMatcher.group(2).trim();
                // 处理 Authorization 头部
                if ("Authorization".equalsIgnoreCase(headerName)) {
                    parseAuthorization(currentRequest, headerValue);
                } else {
                    // 添加到头部列表
                    if (currentRequest.getHeadersList() == null) {
                        currentRequest.setHeadersList(new ArrayList<>());
                    }
                    currentRequest.getHeadersList().add(new HttpHeader(true, headerName, headerValue));
                }
                continue;
            }

            // 其他情况视为 body 内容
            if (currentRequest.getMethod() != null &&
                    ("POST".equals(currentRequest.getMethod()) ||
                            "PUT".equals(currentRequest.getMethod()) ||
                            "PATCH".equals(currentRequest.getMethod()) ||
                            "DELETE".equals(currentRequest.getMethod()))) {
                inBody = true;
                if (bodyBuilder != null && !bodyBuilder.isEmpty()) {
                    bodyBuilder.append("\n");
                }
                if (bodyBuilder == null) {
                    bodyBuilder = new StringBuilder();
                }
                bodyBuilder.append(line);
            }
        }

        // 保存最后一个请求
        if (currentRequest != null) {
            finishRequest(currentRequest, bodyBuilder, contentType);
            // 处理响应脚本
            if (responseScriptBuilder != null && responseScriptBuilder.length() > 0) {
                String convertedScript = convertResponseScriptToPostman(responseScriptBuilder.toString());
                currentRequest.setPostscript(convertedScript);
            }
            requests.add(currentRequest);
        }

        return requests;
    }

    /**
     * 完成请求解析，设置 body 和其他属性
     */
    private static void finishRequest(HttpRequestItem request, StringBuilder bodyBuilder, String contentType) {
        // 设置协议类型
        if (HttpUtil.isSSERequest(request)) {
            request.setProtocol(RequestItemProtocolEnum.SSE);
        } else if (HttpUtil.isWebSocketRequest(request.getUrl())) {
            request.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        } else {
            request.setProtocol(RequestItemProtocolEnum.HTTP);
        }

        // 处理 body
        if (bodyBuilder != null && !bodyBuilder.isEmpty()) {
            String body = bodyBuilder.toString().trim();
            if (!body.isEmpty()) {
                // 根据 Content-Type 处理 body
                if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                    // 解析 urlencoded 格式
                    List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();

                    // 处理多行格式（每行可能以 & 结尾）
                    // 先移除所有换行符和多余的空格，然后按 & 分割
                    String normalizedBody = body.replaceAll("\\s*&\\s*\\n\\s*", "&")  // 处理 & 后换行
                            .replaceAll("\\n\\s*", "&")           // 处理换行（视为 & 分隔）
                            .replaceAll("&+", "&");              // 移除重复的 &

                    String[] pairs = normalizedBody.split("&");
                    for (String pair : pairs) {
                        pair = pair.trim();
                        if (pair.isEmpty()) {
                            continue;
                        }

                        // 处理 key = value 或 key=value 格式
                        int equalsIndex = pair.indexOf('=');
                        if (equalsIndex > 0) {
                            String name = pair.substring(0, equalsIndex).trim();
                            String value = pair.substring(equalsIndex + 1).trim();
                            if (!name.isEmpty()) {
                                urlencodedList.add(new HttpFormUrlencoded(true, name, value));
                            }
                        } else if (!pair.contains("=")) {
                            // 没有等号，视为单独的 key
                            urlencodedList.add(new HttpFormUrlencoded(true, pair, ""));
                        }
                    }
                    request.setUrlencodedList(urlencodedList);
                    request.setBodyType(BODY_TYPE_FORM_URLENCODED);
                } else {
                    // 默认作为 raw body
                    request.setBody(body);
                    request.setBodyType(BODY_TYPE_RAW);
                }
            } else {
                // body 为空
                request.setBodyType(BODY_TYPE_NONE);
            }
        } else {
            // 没有 body
            request.setBodyType(BODY_TYPE_NONE);
        }

        // 如果没有设置名称，使用 URL
        if (request.getName() == null || request.getName().isEmpty() || "未命名请求".equals(request.getName())) {
            try {
                java.net.URI uri = new java.net.URI(request.getUrl());
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    String[] parts = path.split("/");
                    String name = parts[parts.length - 1];
                    if (name.isEmpty() && parts.length > 1) {
                        name = parts[parts.length - 2];
                    }
                    request.setName(name.isEmpty() ? request.getUrl() : name);
                } else {
                    request.setName(request.getUrl());
                }
            } catch (Exception e) {
                request.setName(request.getUrl());
            }
        }
    }

    /**
     * 将 IntelliJ HTTP Client 的响应脚本转换为 Postman 风格的脚本
     * 例如：client.global.set("token", response.body.token) -> pm.environment.set("token", pm.response.json().token)
     */
    private static String convertResponseScriptToPostman(String httpClientScript) {
        if (httpClientScript == null || httpClientScript.trim().isEmpty()) {
            return "";
        }

        StringBuilder postmanScript = new StringBuilder();
        String[] lines = httpClientScript.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            String convertedLine = line;

            // 转换 client.global.set 为 pm.environment.set
            if (line.contains("client.global.set")) {
                convertedLine = line.replace("client.global.set", "pm.environment.set");

                // 转换 response.body 为 pm.response.json()
                // 例如：response.body.token -> pm.response.json().token
                convertedLine = convertedLine.replaceAll("response\\.body\\.([a-zA-Z0-9_]+)", "pm.response.json().$1");

                // 如果只有 response.body（没有属性访问），转换为 pm.response.json()
                convertedLine = convertedLine.replace("response.body", "pm.response.json()");
            }

            // 转换 client.global.get 为 pm.environment.get
            if (line.contains("client.global.get")) {
                convertedLine = convertedLine.replace("client.global.get", "pm.environment.get");
            }

            // 转换 client.test 为 pm.test
            if (line.contains("client.test")) {
                convertedLine = convertedLine.replace("client.test", "pm.test");
            }

            // 转换 response.status 为 pm.response.code
            if (line.contains("response.status")) {
                convertedLine = convertedLine.replace("response.status", "pm.response.code");
            }

            // 转换 response.headers 为 pm.response.headers
            if (line.contains("response.headers")) {
                convertedLine = convertedLine.replace("response.headers", "pm.response.headers");
            }

            if (!postmanScript.isEmpty()) {
                postmanScript.append("\n");
            }
            postmanScript.append(convertedLine);
        }

        return postmanScript.toString();
    }

    /**
     * 解析 Authorization 头部
     */
    private static void parseAuthorization(HttpRequestItem request, String authValue) {
        if (authValue.startsWith("Basic ")) {
            request.setAuthType(AUTH_TYPE_BASIC);
            String credentials = authValue.substring(6).trim();
            // 检查是否是变量占位符格式：Basic {{username}} {{password}}
            if (credentials.contains("{{") && credentials.contains("}}")) {
                // 变量格式，尝试提取用户名和密码变量
                // 格式可能是 "Basic {{username}} {{password}}" 或 "Basic {{username}}:{{password}}"
                String[] parts = credentials.split("\\s+");
                if (parts.length >= 2) {
                    // 格式：Basic {{username}} {{password}}
                    request.setAuthUsername(parts[0]);
                    request.setAuthPassword(parts[1]);
                } else {
                    // 格式：Basic {{username}}:{{password}}
                    int colonIndex = credentials.indexOf(':');
                    if (colonIndex > 0) {
                        request.setAuthUsername(credentials.substring(0, colonIndex));
                        request.setAuthPassword(credentials.substring(colonIndex + 1));
                    } else {
                        request.setAuthUsername(credentials);
                        request.setAuthPassword("");
                    }
                }
            } else {
                // Base64 编码格式
                try {
                    String decoded = new String(Base64.getDecoder().decode(credentials));
                    String[] parts = decoded.split(":", 2);
                    if (parts.length == 2) {
                        request.setAuthUsername(parts[0]);
                        request.setAuthPassword(parts[1]);
                    }
                } catch (Exception e) {
                    log.warn("解析 Basic 认证失败，可能是变量格式", e);
                    // 如果解析失败，可能是变量格式，直接设置
                    request.setAuthUsername(credentials);
                    request.setAuthPassword("");
                }
            }
        } else if (authValue.startsWith("Bearer ")) {
            request.setAuthType(AUTH_TYPE_BEARER);
            request.setAuthToken(authValue.substring(7));
        }
    }

}

