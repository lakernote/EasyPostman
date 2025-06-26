package com.laker.postman.service.http.okhttp;

import okhttp3.*;

import java.io.File;
import java.util.Map;

/**
 * OkHttp 请求构建工具类
 */
public class OkHttpRequestBuilder {
    public static Request buildRequest(String url, String method, Map<String, String> headers, String body) {
        Request.Builder builder = new Request.Builder().url(url);
        String methodUpper = method.toUpperCase();
        String contentType = null;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if ("Content-Type".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    contentType = entry.getValue();
                    break;
                }
            }
        }
        RequestBody requestBody = null;
        if (!"GET".equals(methodUpper) && !"HEAD".equals(methodUpper)) {
            if (body != null && !body.isEmpty()) {
                if (contentType == null) {
                    contentType = "application/json; charset=utf-8";
                }
                requestBody = RequestBody.create(body, MediaType.parse(contentType));
            } else {
                if (contentType != null) {
                    requestBody = RequestBody.create(new byte[0], MediaType.parse(contentType));
                } else {
                    requestBody = RequestBody.create(new byte[0], null);
                }
            }
        }
        builder.method(methodUpper, requestBody);
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 构建 multipart/form-data 的 OkHttp Request
     */
    public static Request buildMultipartRequest(String url, String method, Map<String, String> headers,
                                                Map<String, String> formData, Map<String, String> formFiles) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (formData != null) {
            for (Map.Entry<String, String> entry : formData.entrySet()) {
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        if (formFiles != null) {
            for (Map.Entry<String, String> entry : formFiles.entrySet()) {
                File file = new File(entry.getValue());
                if (file.exists()) {
                    String mimeType = null;
                    try {
                        mimeType = java.nio.file.Files.probeContentType(file.toPath());
                    } catch (Exception e) {
                        // ignore
                    }
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                    multipartBuilder.addFormDataPart(entry.getKey(), file.getName(),
                            RequestBody.create(file, MediaType.parse(mimeType)));
                }
            }
        }
        Request.Builder builder = new Request.Builder().url(url).method(method, multipartBuilder.build());
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }


    public static Request buildFormRequest(String url, String method, Map<String, String> headers,
                                           Map<String, String> urlencoded) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (urlencoded != null) {
            for (Map.Entry<String, String> entry : urlencoded.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody requestBody = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(url).method(method, requestBody);
        boolean hasContentType = false;
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
                if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                    hasContentType = true;
                }
            }
        }
        if (!hasContentType) {
            builder.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        }
        return builder.build();
    }
}