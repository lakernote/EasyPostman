package com.laker.postman.service.http.okhttp;

import com.laker.postman.model.PreparedRequest;
import okhttp3.*;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

/**
 * OkHttp 请求构建工具类
 */
public class OkHttpRequestBuilder {
    public static Request buildRequest(PreparedRequest req) {
        Request.Builder builder = new Request.Builder().url(req.url);
        String methodUpper = req.method.toUpperCase();
        String contentType = null;
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                if ("Content-Type".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
                    contentType = entry.getValue();
                    break;
                }
            }
        }
        RequestBody requestBody = null;
        if (!"GET".equals(methodUpper) && !"HEAD".equals(methodUpper)) {
            if (req.body != null && !req.body.isEmpty()) {
                if (contentType == null) {
                    contentType = "application/json; charset=utf-8";
                }
                requestBody = RequestBody.create(req.body, MediaType.parse(contentType));
            } else {
                if (contentType != null) {
                    requestBody = RequestBody.create(new byte[0], MediaType.parse(contentType));
                } else {
                    requestBody = RequestBody.create(new byte[0], null);
                }
            }
        }
        builder.method(methodUpper, requestBody);
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty()) {
                    builder.addHeader(key, value == null ? "" : value);
                }
            }
        }
        return builder.build();
    }

    /**
     * 构建 multipart/form-data 的 OkHttp Request
     */
    public static Request buildMultipartRequest(PreparedRequest req) {
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        if (req.formData != null) {
            for (Map.Entry<String, String> entry : req.formData.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty()) {
                    multipartBuilder.addFormDataPart(key, value == null ? "" : value);
                }
            }
        }
        if (req.formFiles != null) {
            for (Map.Entry<String, String> entry : req.formFiles.entrySet()) {
                File file = new File(entry.getValue());
                if (file.exists()) {
                    String mimeType = null;
                    try {
                        mimeType = Files.probeContentType(file.toPath());
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
        Request.Builder builder = new Request.Builder().url(req.url).method(req.method, multipartBuilder.build());
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty()) {
                    builder.addHeader(key, value == null ? "" : value);
                }
            }
        }
        return builder.build();
    }


    public static Request buildFormRequest(PreparedRequest req) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (req.urlencoded != null) {
            for (Map.Entry<String, String> entry : req.urlencoded.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty()) {
                    formBuilder.add(key, value == null ? "" : value);
                }
            }
        }
        RequestBody requestBody = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(req.url).method(req.method, requestBody);
        boolean hasContentType = false;
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && !key.isEmpty()) {
                    builder.addHeader(key, value == null ? "" : value);
                }
                if ("Content-Type".equalsIgnoreCase(key)) {
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