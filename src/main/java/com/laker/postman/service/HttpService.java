package com.laker.postman.service;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 请求服务类，负责发送 HTTP 请求并返回响应
 */
@Slf4j
public class HttpService {

    // OkHttpClient 单例，配置连接池、超时、重试等参数
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(50, 5, TimeUnit.MINUTES))
            .build();

    // OkHttp 连接信息线程安全存储
    private static final ThreadLocal<String> lastConnectionInfo = new ThreadLocal<>();

    /**
     * 发送 HTTP 请求，支持环境变量替换
     *
     * @param urlString 请求地址
     * @param method    请求方法（GET、POST 等）
     * @param headers   请求头
     * @param body      请求体
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    public static HttpResponse sendRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects) throws Exception {

        return doSendRequest(urlString, method, headers, body, followRedirects);
    }

    /**
     * 处理请求头中的环境变量
     */
    public static Map<String, String> processHeaders(Map<String, String> headers) {
        if (headers == null) return null;

        Map<String, String> processedHeaders = new HashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 处理请求头的键和值中的环境变量
            String processedKey = EnvironmentService.replaceVariables(key);
            String processedValue = EnvironmentService.replaceVariables(value);

            processedHeaders.put(processedKey, processedValue);
        }

        return processedHeaders;
    }

    /**
     * 实际发送 HTTP 请求（使用 OkHttp 实现）
     */
    private static HttpResponse doSendRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects) throws Exception {
        OkHttpClient client = getOkHttpClient(followRedirects);
        Request.Builder builder = new Request.Builder().url(urlString);
        RequestBody requestBody = null;
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
        // 只为非GET/HEAD方法设置请求体，GET/HEAD不允许有body
        if (!"GET".equals(methodUpper) && !"HEAD".equals(methodUpper)) {
            if (body != null && !body.isEmpty()) {
                if (contentType == null) {
                    contentType = "application/json; charset=utf-8";
                }
                requestBody = RequestBody.create(body, MediaType.parse(contentType));
            } else {
                // OkHttp 要求 POST/PUT/PATCH/DELETE 必须有 requestBody
                if (contentType != null) {
                    requestBody = RequestBody.create(new byte[0], MediaType.parse(contentType));
                } else {
                    requestBody = RequestBody.create(new byte[0], null);
                }
            }
        }
        builder.method(methodUpper, requestBody);
        return callWithRequest(headers, client, builder);
    }

    @NotNull
    private static HttpResponse callWithRequest(Map<String, String> headers, OkHttpClient client, Request.Builder builder) throws IOException {
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }
        Call call = client.newCall(builder.build());
        Response okResponse = call.execute();
        HttpResponse response = new HttpResponse();
        response.code = okResponse.code();
        response.headers = new HashMap<>();
        for (String name : okResponse.headers().names()) {
            String value = okResponse.header(name);
            if (value != null) {
                response.headers.put(name, List.of(value));
            }
        }
        response.connectionInfo = lastConnectionInfo.get();
        lastConnectionInfo.remove();
        response.threadName = Thread.currentThread().getName();
        // 检查是否为二进制内容
        String contentType = okResponse.header("Content-Type", "");
        if (contentType != null && (contentType.toLowerCase().contains("application/octet-stream")
                || contentType.toLowerCase().contains("application/pdf")
                || contentType.toLowerCase().contains("image/")
                || contentType.toLowerCase().contains("audio/")
                || contentType.toLowerCase().contains("video/"))) {
            // 保存为临时文件
            InputStream is = okResponse.body() != null ? okResponse.body().byteStream() : null;
            if (is != null) {
                File tempFile = File.createTempFile("download_", null);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                }
                response.filePath = tempFile.getAbsolutePath();
                response.body = "[二进制内容，已保存为临时文件]";
            } else {
                response.body = "[无响应体]";
            }
        } else {
            response.body = okResponse.body() != null ? okResponse.body().string() : "";
            response.filePath = null;
        }
        return response;
    }

    /**
     * 发送 multipart/form-data 请求，支持文本字段和文件字段（OkHttp 实现）
     */
    public static HttpResponse sendRequestWithMultipart(String urlString, String method, Map<String, String> headers,
                                                        Map<String, String> formData, Map<String, String> formFiles, boolean followRedirects) throws Exception {
        OkHttpClient client = getOkHttpClient(followRedirects);
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
                        mimeType = Files.probeContentType(file.toPath());
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                    if (mimeType == null) {
                        mimeType = "application/octet-stream";
                    }
                    multipartBuilder.addFormDataPart(entry.getKey(), file.getName(),
                            RequestBody.create(file, MediaType.parse(mimeType)));
                }
            }
        }
        Request.Builder builder = new Request.Builder().url(urlString).method(method, multipartBuilder.build());
        return callWithRequest(headers, client, builder);
    }

    private static OkHttpClient getOkHttpClient(boolean followRedirects) {
        return okHttpClient.newBuilder()
                .followRedirects(followRedirects)
                .eventListener(new EventListener() {
                    @Override
                    public void connectionAcquired(@NotNull Call call, @NotNull Connection connection) {
                        try {
                            Socket socket = connection.socket();
                            String local = socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort();
                            String remote = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
                            lastConnectionInfo.set(local + " -> " + remote);
                        } catch (Exception e) {
                            log.error("获取连接信息失败", e);
                            lastConnectionInfo.set("无法获取连接信息");
                        }
                    }
                })
                .build();
    }

    public static HttpRequestItem createDefaultRequest() {
        // 创建一个测试请求
        HttpRequestItem testItem = new HttpRequestItem();
        testItem.setId(IdUtil.simpleUUID());
        testItem.setName("测试请求");
        testItem.setUrl("https://httpbin.org/get");
        testItem.setMethod("GET");

        // 添加一些默认的请求头
        testItem.getHeaders().put("User-Agent", "EasyPostman HTTP Client");
        testItem.getHeaders().put("Accept", "*/*");
        return testItem;
    }
}