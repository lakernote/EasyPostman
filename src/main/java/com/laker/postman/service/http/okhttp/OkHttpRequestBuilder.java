package com.laker.postman.service.http.okhttp;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.util.JsonUtil;
import lombok.experimental.UtilityClass;
import okhttp3.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * OkHttp 请求构建工具类
 */
@UtilityClass
public class OkHttpRequestBuilder {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String DEFAULT_JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String DEFAULT_FORM_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
    private static final String APPLICATION_JSON = "application/json";
    private static final String METHOD_GET = "GET";
    private static final String METHOD_HEAD = "HEAD";
    private static final byte[] EMPTY_BODY = new byte[0];


    /**
     * 构建普通请求
     */
    public static Request buildRequest(PreparedRequest req) {
        String methodUpper = req.method.toUpperCase();
        String contentType = extractContentType(req.headers);
        RequestBody requestBody = buildRequestBody(req.body, methodUpper, contentType);

        Request.Builder builder = new Request.Builder()
                .url(req.url)
                .method(methodUpper, requestBody);

        addHeaders(builder, req.headers);

        return builder.build();
    }

    /**
     * 构建 multipart/form-data 的 OkHttp Request
     */
    public static Request buildMultipartRequest(PreparedRequest req) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        addFormDataParts(multipartBuilder, req.formData);
        addFormFileParts(multipartBuilder, req.formFiles);

        Request.Builder builder = new Request.Builder()
                .url(req.url)
                .method(req.method, multipartBuilder.build());

        addHeaders(builder, req.headers);

        return builder.build();
    }

    /**
     * 构建 application/x-www-form-urlencoded 请求
     */
    public static Request buildFormRequest(PreparedRequest req) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        addFormUrlEncodedParts(formBuilder, req.urlencoded);

        Request.Builder builder = new Request.Builder()
                .url(req.url)
                .method(req.method, formBuilder.build());

        boolean hasContentType = addHeaders(builder, req.headers);

        if (!hasContentType) {
            builder.addHeader(CONTENT_TYPE, DEFAULT_FORM_CONTENT_TYPE);
        }

        return builder.build();
    }

    /**
     * 从 headers 中提取 Content-Type
     */
    private static String extractContentType(Map<String, String> headers) {
        if (headers == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (CONTENT_TYPE.equalsIgnoreCase(entry.getKey())) {
                String value = entry.getValue();
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * 构建请求体
     */
    private static RequestBody buildRequestBody(String body, String method, String contentType) {
        if (METHOD_GET.equals(method) || METHOD_HEAD.equals(method)) {
            return null;
        }

        if (body != null && !body.isEmpty()) {
            return createRequestBodyWithContent(body, contentType);
        }

        return createEmptyRequestBody(contentType);
    }

    /**
     * 创建包含内容的请求体
     */
    private static RequestBody createRequestBodyWithContent(String body, String contentType) {
        String actualContentType = contentType != null ? contentType : DEFAULT_JSON_CONTENT_TYPE;
        String processedBody = processBodyContent(body, actualContentType);

        return RequestBody.create(processedBody, MediaType.parse(actualContentType));
    }

    /**
     * 处理请求体内容（如去除 JSON5 注释）
     */
    private static String processBodyContent(String body, String contentType) {
        if (isJsonContentType(contentType)) {
            return cleanJsonComments(body);
        }
        return body;
    }

    /**
     * 判断是否为 JSON Content-Type
     */
    private static boolean isJsonContentType(String contentType) {
        return contentType != null && contentType.toLowerCase().contains(APPLICATION_JSON);
    }

    /**
     * 清理 JSON 注释（支持 JSON5）
     */
    private static String cleanJsonComments(String json) {
        try {
            return JsonUtil.cleanJsonComments(json);
        } catch (Exception e) {
            // 如果清理失败，返回原始内容
            return json;
        }
    }

    /**
     * 创建空请求体
     */
    private static RequestBody createEmptyRequestBody(String contentType) {
        MediaType mediaType = contentType != null ? MediaType.parse(contentType) : null;
        return RequestBody.create(EMPTY_BODY, mediaType);
    }

    /**
     * 添加表单数据部分
     */
    private static void addFormDataParts(MultipartBody.Builder builder, Map<String, String> formData) {
        if (formData == null) {
            return;
        }

        for (Map.Entry<String, String> entry : formData.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isEmpty()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                builder.addFormDataPart(key, value);
            }
        }
    }

    /**
     * 添加表单文件部分
     */
    private static void addFormFileParts(MultipartBody.Builder builder, Map<String, String> formFiles) {
        if (formFiles == null) {
            return;
        }

        for (Map.Entry<String, String> entry : formFiles.entrySet()) {
            File file = new File(entry.getValue());
            if (file.exists()) {
                String mimeType = detectMimeType(file);
                builder.addFormDataPart(
                        entry.getKey(),
                        file.getName(),
                        RequestBody.create(file, MediaType.parse(mimeType))
                );
            }
        }
    }

    /**
     * 检测文件 MIME 类型
     */
    private static String detectMimeType(File file) {
        try {
            String mimeType = Files.probeContentType(file.toPath());
            return mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
        } catch (Exception e) {
            return DEFAULT_MIME_TYPE;
        }
    }

    /**
     * 添加 URL 编码表单部分
     */
    private static void addFormUrlEncodedParts(FormBody.Builder builder, Map<String, String> urlencoded) {
        if (urlencoded == null) {
            return;
        }

        for (Map.Entry<String, String> entry : urlencoded.entrySet()) {
            String key = entry.getKey();
            if (key != null && !key.isEmpty()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                builder.add(key, value);
            }
        }
    }

    /**
     * 添加 HTTP 请求头
     *
     * @return 是否包含 Content-Type 头
     */
    private static boolean addHeaders(Request.Builder builder, Map<String, String> headers) {
        if (headers == null) {
            return false;
        }

        boolean hasContentType = false;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (isValidHeaderName(key)) {
                builder.addHeader(key, value != null ? value : "");

                if (CONTENT_TYPE.equalsIgnoreCase(key)) {
                    hasContentType = true;
                }
            }
            // 非法 header name 自动跳过
        }

        return hasContentType;
    }

    /**
     * 判断 header name 是否为合法的 ASCII 字符且不包含非法字符
     */
    private static boolean isValidHeaderName(String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }

        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            // 仅允许 33~126 范围的 ASCII 字符，且不能包含冒号
            if (c < 33 || c > 126 || c == ':') {
                return false;
            }
        }
        return true;
    }
}