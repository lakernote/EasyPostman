package com.laker.postman.service.http;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.service.http.okhttp.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

import java.io.IOException;
import java.util.Map;

import static com.laker.postman.service.http.HttpRequestUtil.extractBaseUri;

/**
 * HTTP 请求服务类，负责发送 HTTP 请求并返回响应
 */
@Slf4j
public class HttpService {

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
    public static HttpResponse sendRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects, boolean logEvent) throws Exception {
        String baseUri = extractBaseUri(urlString);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, followRedirects, logEvent);
        Request request = OkHttpRequestBuilder.buildRequest(urlString, method, headers, body);
        return callWithRequest(client, request);
    }

    /**
     * 发送 form-urlencoded 请求
     */
    public static HttpResponse sendRequestWithForm(String urlString, String method, Map<String, String> headers,
                                                   Map<String, String> urlencoded, boolean followRedirects, boolean logEvent) throws Exception {
        String baseUri = extractBaseUri(urlString);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, followRedirects, logEvent);
        Request request = OkHttpRequestBuilder.buildFormRequest(urlString, method, headers, urlencoded);
        return callWithRequest(client, request);
    }

    /**
     * 发送 multipart请求，支持文本字段和文件字段（OkHttp 实现）
     */
    public static HttpResponse sendRequestWithMultipart(String urlString, String method, Map<String, String> headers,
                                                        Map<String, String> formData, Map<String, String> formFiles, boolean followRedirects, boolean logEvent) throws Exception {
        String baseUri = extractBaseUri(urlString);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, followRedirects, logEvent);
        Request request = OkHttpRequestBuilder.buildMultipartRequest(urlString, method, headers, formData, formFiles);
        return callWithRequest(client, request);
    }

    /**
     * 发送 SSE 请求，使用 OkHttp SSE
     */
    public static EventSource sendSseRequest(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects, EventSourceListener listener) throws Exception {
        String baseUri = extractBaseUri(urlString);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, followRedirects);
        Request request = OkHttpRequestBuilder.buildRequest(urlString, method, headers, body);
        return EventSources.createFactory(client).newEventSource(request, new LogEventSourceListener(listener));
    }


    /**
     * 发送 WebSocket 请求，使用 OkHttp WebSocket
     */
    public static WebSocket sendWebSocket(String urlString, String method, Map<String, String> headers, String body, boolean followRedirects, WebSocketListener listener) throws Exception {
        String baseUri = extractBaseUri(urlString);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, followRedirects);
        Request request = OkHttpRequestBuilder.buildRequest(urlString, method, headers, body);
        return client.newWebSocket(request, new LogWebSocketListener(listener));
    }


    private static HttpResponse callWithRequest(OkHttpClient client, Request request) throws IOException {
        long startTime = System.currentTimeMillis();

        HttpResponse httpResponse = new HttpResponse();
        long queueStart = System.currentTimeMillis(); // 记录newCall前的时间戳
        Call call = client.newCall(request);
        Response okResponse = null;
        // 记录连接池状态
        ConnectionPool pool = client.connectionPool();
        httpResponse.idleConnectionCount = pool.idleConnectionCount();
        httpResponse.connectionCount = pool.connectionCount();
        try {
            okResponse = call.execute();
        } finally {
            fillHttpEventInfo(httpResponse, queueStart, startTime);
        }
        return OkHttpResponseHandler.handleResponse(okResponse, httpResponse);
    }


    public static void fillHttpEventInfo(HttpResponse httpResponse, long queueStart, long startTime) {
        HttpEventInfo httpEventInfo = EasyConsoleEventListener.getAndRemove();
        if (httpEventInfo != null) {
            httpEventInfo.setQueueStart(queueStart);
            // 计算排队耗时
            if (httpEventInfo.getCallStart() > 0) {
                httpEventInfo.setQueueingCost(httpEventInfo.getCallStart() - queueStart);
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