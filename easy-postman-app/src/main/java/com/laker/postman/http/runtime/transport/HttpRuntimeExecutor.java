package com.laker.postman.http.runtime.transport;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.http.runtime.sse.SseResponseCallback;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

@Slf4j
@UtilityClass
public class HttpRuntimeExecutor {

    public static HttpResponse executeHttp(PreparedRequest req) throws Exception {
        return HttpTransportRuntime.executeHttp(req, null);
    }

    public static HttpResponse executeHttp(PreparedRequest req, SseResponseCallback callback) throws Exception {
        return HttpTransportRuntime.executeHttp(req, callback);
    }

    public static HttpResponse executeHttp(PreparedRequest req,
                                           SseResponseCallback callback,
                                           HttpCallTracker callTracker) throws Exception {
        return HttpTransportRuntime.executeHttp(req, callback, callTracker);
    }

    public static HttpResponse executeHttp(PreparedRequest req,
                                           SseResponseCallback callback,
                                           HttpCallTracker callTracker,
                                           HttpBaseClientProvider baseClientProvider) throws Exception {
        return HttpTransportRuntime.executeHttp(req, callback, callTracker, baseClientProvider);
    }

    public static EventSource openSse(PreparedRequest req, EventSourceListener listener) {
        return HttpTransportRuntime.openSse(req, listener);
    }

    public static EventSource openSse(PreparedRequest req,
                                         EventSourceListener listener,
                                         HttpBaseClientProvider baseClientProvider) {
        return HttpTransportRuntime.openSse(req, listener, baseClientProvider);
    }

    public static WebSocket openWebSocket(PreparedRequest req, WebSocketListener listener) {
        return HttpTransportRuntime.openWebSocket(req, listener);
    }

    public static WebSocket openWebSocket(PreparedRequest req,
                                             WebSocketListener listener,
                                             HttpBaseClientProvider baseClientProvider) {
        return HttpTransportRuntime.openWebSocket(req, listener, baseClientProvider);
    }

    public static WebSocket openWebSocket(PreparedRequest req,
                                             WebSocketListener listener,
                                             HttpBaseClientProvider baseClientProvider,
                                             boolean lifecycleLoggingEnabled) {
        return HttpTransportRuntime.openWebSocket(req, listener, baseClientProvider, lifecycleLoggingEnabled);
    }
}
