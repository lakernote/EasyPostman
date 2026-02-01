package com.laker.postman.service.http;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.okhttp.*;
import com.laker.postman.service.http.sse.SseResEventListener;
import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;
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
@UtilityClass
public class HttpService {

    /**
     * 构建支持动态 eventListenerFactory 和超时的 OkHttpClient
     * <p>
     * 拦截器执行顺序说明：
     * 1. addNetworkInterceptor(CompressionSizeInterceptor)：网络层，最先执行，记录压缩前体积。
     * 2. addInterceptor(BrotliInterceptor.INSTANCE)：应用层，自动解压 br 响应。
     * 3. addInterceptor(DeflateDecompressInterceptor)：应用层，自动解压 deflate 响应。
     * 4. BridgeInterceptor（OkHttp 内部）：自动解压 gzip 响应。
     * 5. 其他自定义拦截器（如日志、超时等）。
     * <p>
     * 推荐顺序保证：
     * - 压缩前体积先被记录。
     * - br/deflate/gzip 都能自动解压。
     * - 业务逻辑拦截器可安全处理解压后的响应体。
     * <p>
     * EasyConsoleEventListener 精细化控制说明：
     * - collectBasicInfo: 收集基本信息（headers、body），用于历史记录、HTML 渲染
     * - collectEventInfo: 收集完整事件信息（DNS、连接、SSL等），用于性能分析
     * - enableNetworkLog: 输出到 NetworkLogPanel，用于调试
     * <p>
     * 优化：只有至少一个开关为 true 时才创建 EventListener
     * - 如果三个开关都为 false，则不创建 EventListener（最小性能开销）
     * <p>
     * 各场景配置：
     * - Collection: collectBasicInfo=true, collectEventInfo=true, enableNetworkLog=true (全功能)
     * - Functional: collectBasicInfo=true, collectEventInfo=true, enableNetworkLog=false (不输出UI日志)
     * - Performance: collectBasicInfo=true, collectEventInfo=可选, enableNetworkLog=false (根据设置)
     */
    private static OkHttpClient buildDynamicClient(OkHttpClient baseClient, PreparedRequest preparedRequest, int timeoutMs) {
        OkHttpClient.Builder builder = baseClient.newBuilder();
        // 添加自动解压拦截器
        builder.addNetworkInterceptor(new CompressionDecompressNetworkInterceptor());

        // 只有至少需要一种信息收集时才创建 EventListener
        // 如果三个开关都是 false，则不创建（最小性能开销）
        boolean needEventListener = preparedRequest.collectBasicInfo
                || preparedRequest.collectEventInfo
                || preparedRequest.enableNetworkLog;

        if (needEventListener) {
            builder.eventListenerFactory(call -> new EasyConsoleEventListener(preparedRequest));
        }

        if (timeoutMs > 0) {
            builder.connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .callTimeout(timeoutMs * 3L, TimeUnit.MILLISECONDS); // 整个请求的总超时，包括重定向和重试
        }
        return builder.build();
    }

    /**
     * 发送 HTTP 请求，支持环境变量替换
     *
     * @return HttpResponse 响应对象
     * @throws Exception 发送请求异常
     */
    public static HttpResponse sendRequest(PreparedRequest req, SseResEventListener callback) throws Exception {
        Request request = buildRequestByType(req);
        return executeRequest(req, request, callback);
    }

    /**
     * 根据请求类型构建 Request
     */
    private static Request buildRequestByType(PreparedRequest req) {
        if (req.isMultipart) {
            return OkHttpRequestBuilder.buildMultipartRequest(req);
        } else if (req.urlencodedList != null && !req.urlencodedList.isEmpty()) {
            return OkHttpRequestBuilder.buildFormRequest(req);
        } else {
            return OkHttpRequestBuilder.buildRequest(req);
        }
    }

    /**
     * 构建自定义 OkHttpClient
     */
    private static OkHttpClient buildCustomClient(PreparedRequest req) {
        String baseUri = extractBaseUri(req.url);
        int timeoutMs = SettingManager.getRequestTimeout();
        OkHttpClient baseClient = OkHttpClientManager.getClient(baseUri, req.followRedirects);
        return buildDynamicClient(baseClient, req, timeoutMs);
    }

    /**
     * 执行 HTTP 请求的通用方法
     */
    private static HttpResponse executeRequest(PreparedRequest req, Request request, SseResEventListener callback) throws Exception {
        OkHttpClient client = buildCustomClient(req);
        Call call = client.newCall(request);
        return callWithRequest(call, client, callback);
    }

    /**
     * 发送 SSE 请求，支持动态 eventListenerFactory 和超时配置
     */
    public static EventSource sendSseRequest(PreparedRequest req, EventSourceListener listener) {
        OkHttpClient customClient = buildCustomClient(req);
        Request request = buildRequestByType(req);
        return EventSources.createFactory(customClient).newEventSource(request, listener);
    }

    /**
     * 发送 WebSocket 请求，支持动态 eventListenerFactory 和超时配置
     */
    public static void sendWebSocket(PreparedRequest req, WebSocketListener listener) {
        OkHttpClient customClient = buildCustomClient(req);
        Request request = buildRequestByType(req);
        customClient.newWebSocket(request, new LogWebSocketListener(listener));
    }


    private static HttpResponse callWithRequest(Call call, OkHttpClient client, SseResEventListener callback) throws IOException {
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
        OkHttpResponseHandler.handleResponse(okResponse, httpResponse, callback);
        httpResponse.endTime = System.currentTimeMillis();
        httpResponse.costMs = httpResponse.endTime - startTime;
        // 响应后主动通知Cookie变化，刷新CookieTablePanel
        CookieService.notifyCookieChanged();
        return httpResponse;
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
    }
}