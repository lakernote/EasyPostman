package com.laker.postman.service.http.okhttp;

import com.laker.postman.model.PreparedRequest;
import okhttp3.*;

import java.io.File;
import java.util.Map;

/**
 * OkHttp 请求构建工具类
 */
public class OkHttpRequestBuilder {
    public static Request buildRequest(PreparedRequest req) {
        Request.Builder builder = new Request.Builder().url(req.url);
        builder.tag(req.id);
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
                builder.addHeader(entry.getKey(), entry.getValue());
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
                multipartBuilder.addFormDataPart(entry.getKey(), entry.getValue());
            }
        }
        if (req.formFiles != null) {
            for (Map.Entry<String, String> entry : req.formFiles.entrySet()) {
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
        Request.Builder builder = new Request.Builder().url(req.url).method(req.method, multipartBuilder.build());
        builder.tag(req.id);
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }


    public static Request buildFormRequest(PreparedRequest req) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (req.urlencoded != null) {
            for (Map.Entry<String, String> entry : req.urlencoded.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody requestBody = formBuilder.build();
        Request.Builder builder = new Request.Builder().url(req.url).method(req.method, requestBody);
        builder.tag(req.id);
        boolean hasContentType = false;
        if (req.headers != null) {
            for (Map.Entry<String, String> entry : req.headers.entrySet()) {
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