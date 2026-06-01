package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.http.runtime.model.PreparedRequest;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSourceListener;

/**
 * UI-neutral transport port for one-off HTTP exchanges and realtime connections.
 * Hosts can provide different implementations for Swing, CLI, performance workers,
 * tests, or future JavaFX wiring.
 */
public interface HttpTransport {
    HttpResponse execute(PreparedRequest request, HttpExchangeOptions options) throws Exception;

    RealtimeConnectionHandle openSse(PreparedRequest request,
                                     EventSourceListener listener,
                                     RealtimeConnectionOptions options);

    RealtimeWebSocketConnection openWebSocket(PreparedRequest request,
                                             WebSocketListener listener,
                                             RealtimeConnectionOptions options);
}
