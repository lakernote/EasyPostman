package com.laker.postman.service;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.util.ConnectionInfoHolder;
import com.laker.postman.util.HttpEventInfo;
import com.laker.postman.util.OkHttpRequestBuilder;
import com.laker.postman.util.OkHttpResponseHandler;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
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
            .eventListener(new EventListener() {
            })
            .build();

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
        OkHttpClient client = getOkHttpClient(followRedirects);
        Request request = OkHttpRequestBuilder.buildRequest(urlString, method, headers, body);
        return callWithRequest(client, request);
    }

    /**
     * 发送 multipart/form-data 请求，支持文本字段和文件字段（OkHttp 实现）
     */
    public static HttpResponse sendRequestWithMultipart(String urlString, String method, Map<String, String> headers,
                                                        Map<String, String> formData, Map<String, String> formFiles, boolean followRedirects) throws Exception {
        OkHttpClient client = getOkHttpClient(followRedirects);
        Request request = OkHttpRequestBuilder.buildMultipartRequest(urlString, method, headers, formData, formFiles);
        return callWithRequest(client, request);
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

    @NotNull
    private static HttpResponse callWithRequest(OkHttpClient client, Request request) throws IOException {
        long startTime = System.currentTimeMillis();
        HttpResponse httpResponse = new HttpResponse();
        Call call = client.newCall(request);
        Response okResponse = null;
        HttpEventInfo httpEventInfo;
        try {
            okResponse = call.execute();
        } finally {
            httpEventInfo = ConnectionInfoHolder.getAndRemove();
            httpResponse.httpEventInfo = httpEventInfo;
            httpResponse.costMs = System.currentTimeMillis() - startTime;
        }
        return OkHttpResponseHandler.handleResponse(okResponse, httpResponse);
    }

    private static OkHttpClient getOkHttpClient(boolean followRedirects) {
        return okHttpClient.newBuilder()
                .followRedirects(followRedirects)
                .eventListener(ConnectionInfoHolder.getEventListener())
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