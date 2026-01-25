package com.laker.postman.model;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 保存的响应对象
 * 类似 Postman 中保存的 Example 响应
 */
@Data
public class SavedResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id; // 响应唯一标识
    private String name; // 响应名称
    private long timestamp; // 保存时间戳

    // 原始请求信息（用于显示和对比）
    private OriginalRequest originalRequest;

    // 响应信息
    private int code; // 状态码
    private String status; // 状态描述，如 "OK"
    private List<HttpHeader> headers = new ArrayList<>(); // 响应头
    private List<CookieInfo> cookies = new ArrayList<>(); // Cookies
    private String body; // 响应体
    private String previewLanguage; // 预览语言类型，如 "json", "html", "xml" 等

    // 性能信息
    private long costMs; // 请求耗时（毫秒）
    private long bodySize; // 响应体大小（字节）
    private long headersSize; // 响应头大小（字节）

    /**
     * 原始请求信息（保存的快照）
     */
    @Data
    public static class OriginalRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        private String method; // 请求方法
        private String url; // 请求URL
        private List<HttpHeader> headers = new ArrayList<>(); // 请求头
        private List<HttpParam> params = new ArrayList<>(); // Query 参数
        private String bodyType; // 请求体类型
        private String body; // 请求体内容
        private List<HttpFormData> formDataList = new ArrayList<>(); // FormData
        private List<HttpFormUrlencoded> urlencodedList = new ArrayList<>(); // URL编码表单
    }

    /**
     * 从 PreparedRequest 和 HttpResponse 创建 SavedResponse
     */
    public static SavedResponse fromRequestAndResponse(String name, PreparedRequest request, HttpResponse response) {
        SavedResponse saved = new SavedResponse();
        saved.setId(UUID.randomUUID().toString());
        saved.setName(name);
        saved.setTimestamp(System.currentTimeMillis());

        // 保存原始请求
        OriginalRequest originalRequest = new OriginalRequest();
        originalRequest.setMethod(request.method);
        originalRequest.setUrl(request.url);
        originalRequest.setHeaders(request.headersList != null ? new ArrayList<>(request.headersList) : new ArrayList<>());
        originalRequest.setParams(request.paramsList != null ? new ArrayList<>(request.paramsList) : new ArrayList<>());
        originalRequest.setBodyType(request.bodyType);
        originalRequest.setBody(request.body);
        originalRequest.setFormDataList(request.formDataList != null ? new ArrayList<>(request.formDataList) : new ArrayList<>());
        originalRequest.setUrlencodedList(request.urlencodedList != null ? new ArrayList<>(request.urlencodedList) : new ArrayList<>());
        saved.setOriginalRequest(originalRequest);

        // 保存响应信息
        saved.setCode(response.code);

        // 转换响应头
        List<HttpHeader> headers = new ArrayList<>();
        if (response.headers != null) {
            for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
                String key = entry.getKey();
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    String value = String.join(", ", entry.getValue());
                    headers.add(new HttpHeader(true, key, value));
                }
            }
        }
        saved.setHeaders(headers);

        saved.setBody(response.body);
        saved.setCostMs(response.costMs);
        saved.setBodySize(response.bodySize);
        saved.setHeadersSize(response.headersSize);

        // 保存预览语言类型（根据 Content-Type 自动检测）
        saved.setPreviewLanguage(detectPreviewLanguage(response));

        return saved;
    }

    /**
     * 根据响应自动检测预览语言类型
     */
    private static String detectPreviewLanguage(HttpResponse response) {
        if (response.headers != null) {
            for (Map.Entry<String, List<String>> entry : response.headers.entrySet()) {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Content-Type")) {
                    List<String> values = entry.getValue();
                    if (values != null && !values.isEmpty()) {
                        String contentType = values.get(0).toLowerCase();
                        if (contentType.contains("json")) return "json";
                        if (contentType.contains("xml")) return "xml";
                        if (contentType.contains("html")) return "html";
                        if (contentType.contains("javascript")) return "javascript";
                        if (contentType.contains("css")) return "css";
                        if (contentType.contains("text")) return "text";
                    }
                }
            }
        }

        // 基于内容自动识别
        String body = response.body;
        if (body != null && !body.isEmpty()) {
            String trimmed = body.trim();
            if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                return "json";
            }
            if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
                if (trimmed.toLowerCase().contains("<html")) return "html";
                if (trimmed.toLowerCase().contains("<?xml")) return "xml";
            }
        }

        return "text"; // 默认为文本
    }
}

