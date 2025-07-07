package com.laker.postman.service.http;

import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.okhttp.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.laker.postman.service.http.HttpRequestUtil.extractBaseUri;

/**
 * HTTP 请求服务类，负责发送 HTTP 请求并返回响应
 */
@Slf4j
public class HttpService {

    /**
     * 构建支持动态 eventListenerFactory 和超时的 OkHttpClient
     */
    private static OkHttpClient buildDynamicClient(OkHttpClient baseClient, boolean logEvent, int timeoutMs) {
        OkHttpClient.Builder builder = baseClient.newBuilder();
        if (logEvent) {
            builder.eventListenerFactory(call -> new EasyConsoleEventListener());
        }
        if (timeoutMs > 0) {
            builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS);
        }
        return builder.build();
    }

    /**
     * 发送 HTTP 请求，支持环境变量替换
     *
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    public static HttpResponse sendRequest(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient client = buildDynamicClient(baseClient, req.logEvent, timeoutMs);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        Call call = client.newCall(request);
        return callWithRequest(call, client);
    }

    /**
     * 发送 form-urlencoded 请求
     */
    public static HttpResponse sendRequestWithForm(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient client = buildDynamicClient(baseClient, req.logEvent, timeoutMs);
        Request request = OkHttpRequestBuilder.buildFormRequest(req);
        Call call = client.newCall(request);
        return callWithRequest(call, client);
    }

    /**
     * 发送 multipart请求，支持文本字段和文件字段（OkHttp 实现）
     */
    public static HttpResponse sendRequestWithMultipart(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient client = buildDynamicClient(baseClient, req.logEvent, timeoutMs);
        Request request = OkHttpRequestBuilder.buildMultipartRequest(req);
        Call call = client.newCall(request);
        return callWithRequest(call, client);
    }

    /**
     * 发送 SSE 请求，支持动态 eventListenerFactory 和超时配置
     */
    public static EventSource sendSseRequest(PreparedRequest req, EventSourceListener listener) {
        String baseUri = extractBaseUri(req.url);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        OkHttpClient customClient = buildDynamicClient(baseClient, req.logEvent, timeoutMs);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        return EventSources.createFactory(customClient).newEventSource(request, new LogEventSourceListener(listener));
    }

    /**
     * 发送 WebSocket 请求，支持动态 eventListenerFactory 和超时配置
     */
    public static WebSocket sendWebSocket(PreparedRequest req, WebSocketListener listener) {
        String baseUri = extractBaseUri(req.url);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        OkHttpClient customClient = buildDynamicClient(baseClient, req.logEvent, timeoutMs);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        return customClient.newWebSocket(request, new LogWebSocketListener(listener));
    }


    private static HttpResponse callWithRequest(Call call, OkHttpClient client) throws IOException {
        long startTime = System.currentTimeMillis();
        HttpResponse httpResponse = new HttpResponse();
        ConnectionPool pool = client.connectionPool();
        httpResponse.idleConnectionCount = pool.idleConnectionCount();
        httpResponse.connectionCount = pool.connectionCount();
        Response okResponse;
        try {
            okResponse = call.execute();
        } finally {
            fillHttpEventInfo(httpResponse, startTime);
        }
        // 响应后主动通知Cookie变化，刷新CookieTablePanel
        CookieService.notifyCookieChanged();
        return OkHttpResponseHandler.handleResponse(okResponse, httpResponse);
    }


    private static void fillHttpEventInfo(HttpResponse httpResponse, long startTime) {
        HttpEventInfo httpEventInfo = EasyConsoleEventListener.getAndRemove();
        if (httpEventInfo != null) {
            httpEventInfo.setQueueStart(startTime);
            // 计算排队耗时
            if (httpEventInfo.getCallStart() > 0) {
                httpEventInfo.setQueueingCost(httpEventInfo.getCallStart() - startTime);
            }
            // 计算阻塞耗时
            if (httpEventInfo.getConnectStart() > 0 && httpEventInfo.getCallStart() > 0) {
                httpEventInfo.setStalledCost(httpEventInfo.getConnectStart() - httpEventInfo.getCallStart());
            }
        }
        httpResponse.httpEventInfo = httpEventInfo;
        httpResponse.costMs = System.currentTimeMillis() - startTime;
    }
}