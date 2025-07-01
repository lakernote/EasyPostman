package com.laker.postman.service.http;

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

import static com.laker.postman.service.http.HttpRequestUtil.extractBaseUri;

/**
 * HTTP 请求服务类，负责发送 HTTP 请求并返回响应
 */
@Slf4j
public class HttpService {

    /**
     * 发送 HTTP 请求，支持环境变量替换
     *
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    public static HttpResponse sendRequest(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, req);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        return callWithRequest(client, request);
    }

    /**
     * 发送 form-urlencoded 请求
     */
    public static HttpResponse sendRequestWithForm(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, req);
        Request request = OkHttpRequestBuilder.buildFormRequest(req);
        return callWithRequest(client, request);
    }

    /**
     * 发送 multipart请求，支持文本字段和文件字段（OkHttp 实现）
     */
    public static HttpResponse sendRequestWithMultipart(PreparedRequest req) throws Exception {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, req);
        Request request = OkHttpRequestBuilder.buildMultipartRequest(req);
        return callWithRequest(client, request);
    }

    /**
     * 发送 SSE 请求，使用 OkHttp SSE
     */
    public static EventSource sendSseRequest(PreparedRequest req, EventSourceListener listener) {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, req);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        return EventSources.createFactory(client).newEventSource(request, new LogEventSourceListener(listener));
    }


    /**
     * 发送 WebSocket 请求，使用 OkHttp WebSocket
     */
    public static WebSocket sendWebSocket(PreparedRequest req, WebSocketListener listener) {
        String baseUri = extractBaseUri(req.url);
        OkHttpClient client = OkHttpClientManager.getClient(baseUri, req);
        Request request = OkHttpRequestBuilder.buildRequest(req);
        return client.newWebSocket(request, new LogWebSocketListener(listener));
    }


    private static HttpResponse callWithRequest(OkHttpClient client, Request request) throws IOException {
        long startTime = System.currentTimeMillis();
        HttpResponse httpResponse = new HttpResponse();
        Call call = client.newCall(request);
        Response okResponse = null;
        // 记录连接池状态
        ConnectionPool pool = client.connectionPool();
        httpResponse.idleConnectionCount = pool.idleConnectionCount();
        httpResponse.connectionCount = pool.connectionCount();
        try {
            okResponse = call.execute();
        } finally {
            fillHttpEventInfo(httpResponse, startTime);
        }
        return OkHttpResponseHandler.handleResponse(okResponse, httpResponse);
    }


    public static void fillHttpEventInfo(HttpResponse httpResponse, long startTime) {
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