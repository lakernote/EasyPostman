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

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

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
        boolean inBody = false;
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

            // 检查是否是请求分隔符（### 开头的注释）
            Matcher separatorMatcher = REQUEST_SEPARATOR_PATTERN.matcher(trimmedLine);
            if (separatorMatcher.matches()) {
                // 保存上一个请求
                if (currentRequest != null) {
                    finishRequest(currentRequest, bodyBuilder, contentType);
                    requests.add(currentRequest);
                }
                // 开始新请求
                String requestName = separatorMatcher.group(1).trim();
                currentRequest = new HttpRequestItem();
                currentRequest.setId(UUID.randomUUID().toString());
                currentRequest.setName(requestName.isEmpty() ? "未命名请求" : requestName);
                bodyBuilder = new StringBuilder();
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
                    inBody = false;
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
            requests.add(currentRequest);
        }

        return requests;
    }

    /**
     * 完成请求解析，设置 body 和其他属性
     */
    private static void finishRequest(HttpRequestItem request, StringBuilder bodyBuilder, String contentType) {
        // 设置协议类型
        if (HttpUtil.isSSERequest(request.getHeaders())) {
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
                    String[] pairs = body.split("&");
                    for (String pair : pairs) {
                        String[] kv = pair.split("=", 2);
                        String name = kv.length > 0 ? kv[0].trim() : "";
                        String value = kv.length > 1 ? kv[1].trim() : "";
                        if (!name.isEmpty()) {
                            urlencodedList.add(new HttpFormUrlencoded(true, name, value));
                        }
                    }
                    request.setUrlencodedList(urlencodedList);
                    request.setBodyType("urlencoded");
                } else {
                    // 默认作为 raw body
                    request.setBody(body);
                    request.setBodyType("raw");
                }
            }
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

