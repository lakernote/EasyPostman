package com.laker.postman.service.http;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.service.http.sse.SseResEventListener;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

@Slf4j
@UtilityClass
public class HttpSingleRequestExecutor {

    public static HttpResponse executeHttp(PreparedRequest req) throws Exception {
        return HttpService.sendRequest(req, null);
    }

    public static HttpResponse executeHttp(PreparedRequest req, SseResEventListener callback) throws Exception {
        return HttpService.sendRequest(req, callback);
    }

    public static HttpResponse executeHttp(PreparedRequest req,
                                           SseResEventListener callback,
                                           HttpCallTracker callTracker) throws Exception {
        return HttpService.sendRequest(req, callback, callTracker);
    }

    public static HttpResponse executeHttp(PreparedRequest req,
                                           SseResEventListener callback,
                                           HttpCallTracker callTracker,
                                           HttpBaseClientProvider baseClientProvider) throws Exception {
        return HttpService.sendRequest(req, callback, callTracker, baseClientProvider);
    }

    public static EventSource executeSSE(PreparedRequest req, EventSourceListener listener) {
        return HttpService.sendSseRequest(req, listener);
    }

    public static EventSource executeSSE(PreparedRequest req,
                                         EventSourceListener listener,
                                         HttpBaseClientProvider baseClientProvider) {
        return HttpService.sendSseRequest(req, listener, baseClientProvider);
    }

    public static WebSocket executeWebSocket(PreparedRequest req, WebSocketListener listener) {
        return HttpService.sendWebSocket(req, listener);
    }

    public static WebSocket executeWebSocket(PreparedRequest req,
                                             WebSocketListener listener,
                                             HttpBaseClientProvider baseClientProvider) {
        return HttpService.sendWebSocket(req, listener, baseClientProvider);
    }

    public static WebSocket executeWebSocket(PreparedRequest req,
                                             WebSocketListener listener,
                                             HttpBaseClientProvider baseClientProvider,
                                             boolean lifecycleLoggingEnabled) {
        return HttpService.sendWebSocket(req, listener, baseClientProvider, lifecycleLoggingEnabled);
    }
}
