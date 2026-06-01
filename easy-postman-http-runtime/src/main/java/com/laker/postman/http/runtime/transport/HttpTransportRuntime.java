package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.sse.SseResponseCallback;
import lombok.experimental.UtilityClass;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;

/**
 * Thin static entry point for the host application.
 * Concrete responsibilities live in instance ports so Swing, CLI, performance,
 * and future JavaFX hosts can wire their own transport dependencies.
 */
@UtilityClass
public class HttpTransportRuntime {
    private static final HttpClientResolver CLIENT_RESOLVER = HttpClientResolver.DEFAULT;
    private static final HttpExchangeExecutor EXCHANGE_EXECUTOR = new HttpExchangeExecutor(CLIENT_RESOLVER);
    private static final RealtimeConnectionFactory REALTIME_CONNECTION_FACTORY =
            new RealtimeConnectionFactory(CLIENT_RESOLVER);

    public static HttpResponse executeHttp(PreparedRequest request, SseResponseCallback callback) throws Exception {
        return EXCHANGE_EXECUTOR.executeHttp(request, callback);
    }

    public static HttpResponse executeHttp(PreparedRequest request,
                                           SseResponseCallback callback,
                                           HttpCallTracker callTracker) throws Exception {
        return EXCHANGE_EXECUTOR.executeHttp(request, callback, callTracker);
    }

    public static HttpResponse executeHttp(PreparedRequest request,
                                           SseResponseCallback callback,
                                           HttpCallTracker callTracker,
                                           HttpBaseClientProvider baseClientProvider) throws Exception {
        return EXCHANGE_EXECUTOR.executeHttp(request, callback, callTracker, baseClientProvider);
    }

    public static RealtimeConnectionHandle openSseConnection(PreparedRequest request, EventSourceListener listener) {
        return REALTIME_CONNECTION_FACTORY.openSseConnection(request, listener);
    }

    public static RealtimeConnectionHandle openSseConnection(PreparedRequest request,
                                                             EventSourceListener listener,
                                                             HttpBaseClientProvider baseClientProvider) {
        return REALTIME_CONNECTION_FACTORY.openSseConnection(request, listener, baseClientProvider);
    }

    public static RealtimeWebSocketConnection openWebSocketConnection(PreparedRequest request,
                                                                      WebSocketListener listener) {
        return REALTIME_CONNECTION_FACTORY.openWebSocketConnection(request, listener);
    }

    public static RealtimeWebSocketConnection openWebSocketConnection(PreparedRequest request,
                                                                      WebSocketListener listener,
                                                                      HttpBaseClientProvider baseClientProvider) {
        return REALTIME_CONNECTION_FACTORY.openWebSocketConnection(request, listener, baseClientProvider);
    }

    public static RealtimeWebSocketConnection openWebSocketConnection(PreparedRequest request,
                                                                      WebSocketListener listener,
                                                                      HttpBaseClientProvider baseClientProvider,
                                                                      boolean lifecycleLoggingEnabled) {
        return REALTIME_CONNECTION_FACTORY.openWebSocketConnection(
                request,
                listener,
                baseClientProvider,
                lifecycleLoggingEnabled
        );
    }

}
