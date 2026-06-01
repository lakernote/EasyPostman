package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.HttpLifecycleLogSink;
import com.laker.postman.http.runtime.okhttp.WebSocketLifecycleLogListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;

public final class RealtimeConnectionFactory {
    private final HttpClientResolver clientResolver;

    public RealtimeConnectionFactory(HttpClientResolver clientResolver) {
        this.clientResolver = clientResolver == null ? HttpClientResolver.DEFAULT : clientResolver;
    }

    public RealtimeConnectionHandle openSseConnection(PreparedRequest request,
                                                      EventSourceListener listener,
                                                      RealtimeConnectionOptions options) {
        RealtimeConnectionOptions resolvedOptions = options == null ? RealtimeConnectionOptions.defaults() : options;
        return OkHttpRealtimeConnectionHandles.sse(openSse(
                request,
                listener,
                resolvedOptions.getBaseClientProvider()
        ));
    }

    private EventSource openSse(PreparedRequest request,
                                EventSourceListener listener,
                                HttpBaseClientProvider baseClientProvider) {
        OkHttpClient customClient = clientResolver.resolveClient(request, baseClientProvider);
        Request okRequest = PreparedOkHttpRequestFactory.build(request);
        return EventSources.createFactory(customClient).newEventSource(okRequest, listener);
    }

    public RealtimeWebSocketConnection openWebSocketConnection(PreparedRequest request,
                                                              WebSocketListener listener,
                                                              RealtimeConnectionOptions options) {
        RealtimeConnectionOptions resolvedOptions = options == null ? RealtimeConnectionOptions.defaults() : options;
        return OkHttpRealtimeConnectionHandles.webSocket(openWebSocket(
                request,
                listener,
                resolvedOptions.getBaseClientProvider(),
                resolvedOptions.isLifecycleLoggingEnabled()
        ));
    }

    private WebSocket openWebSocket(PreparedRequest request,
                                    WebSocketListener listener,
                                    HttpBaseClientProvider baseClientProvider,
                                    boolean lifecycleLoggingEnabled) {
        OkHttpClient customClient = clientResolver.resolveClient(request, baseClientProvider);
        Request okRequest = PreparedOkHttpRequestFactory.build(request);
        return customClient.newWebSocket(okRequest, wrapWebSocketListener(
                listener,
                lifecycleLoggingEnabled,
                request != null ? request.lifecycleLogSink : HttpLifecycleLogSink.noop()
        ));
    }

    WebSocketListener wrapWebSocketListener(WebSocketListener listener, boolean lifecycleLoggingEnabled) {
        return wrapWebSocketListener(listener, lifecycleLoggingEnabled, HttpLifecycleLogSink.noop());
    }

    WebSocketListener wrapWebSocketListener(WebSocketListener listener,
                                            boolean lifecycleLoggingEnabled,
                                            HttpLifecycleLogSink lifecycleLogSink) {
        WebSocketListener resolvedListener = listener == null ? new WebSocketListener() {
        } : listener;
        return lifecycleLoggingEnabled
                ? new WebSocketLifecycleLogListener(resolvedListener, lifecycleLogSink)
                : resolvedListener;
    }
}
