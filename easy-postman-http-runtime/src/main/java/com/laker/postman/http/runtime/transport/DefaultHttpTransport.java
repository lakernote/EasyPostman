package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;

public final class DefaultHttpTransport implements HttpTransport {
    private final HttpExchangeExecutor exchangeExecutor;
    private final RealtimeConnectionFactory realtimeConnectionFactory;

    public DefaultHttpTransport() {
        this(new HttpClientResolver());
    }

    public DefaultHttpTransport(HttpClientResolver clientResolver) {
        this(
                new HttpExchangeExecutor(clientResolver),
                new RealtimeConnectionFactory(clientResolver)
        );
    }

    public DefaultHttpTransport(HttpExchangeExecutor exchangeExecutor,
                                RealtimeConnectionFactory realtimeConnectionFactory) {
        this.exchangeExecutor = exchangeExecutor == null
                ? new HttpExchangeExecutor(new HttpClientResolver())
                : exchangeExecutor;
        this.realtimeConnectionFactory = realtimeConnectionFactory == null
                ? new RealtimeConnectionFactory(new HttpClientResolver())
                : realtimeConnectionFactory;
    }

    @Override
    public HttpResponse execute(PreparedRequest request, HttpExchangeOptions options) throws Exception {
        return exchangeExecutor.executeHttp(request, options);
    }

    @Override
    public RealtimeConnectionHandle openSse(PreparedRequest request,
                                            EventSourceListener listener,
                                            RealtimeConnectionOptions options) {
        return realtimeConnectionFactory.openSseConnection(request, listener, options);
    }

    @Override
    public RealtimeWebSocketConnection openWebSocket(PreparedRequest request,
                                                    WebSocketListener listener,
                                                    RealtimeConnectionOptions options) {
        return realtimeConnectionFactory.openWebSocketConnection(request, listener, options);
    }
}
