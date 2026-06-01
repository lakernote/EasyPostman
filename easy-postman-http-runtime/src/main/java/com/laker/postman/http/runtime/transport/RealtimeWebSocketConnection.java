package com.laker.postman.http.runtime.transport;

public interface RealtimeWebSocketConnection extends RealtimeConnectionHandle {

    boolean send(String text);

    long queueSize();
}
