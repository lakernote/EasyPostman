package com.laker.postman.http.runtime.transport;

public interface RealtimeConnectionHandle {

    void cancel();

    default boolean close(int code, String reason) {
        cancel();
        return true;
    }

    default Object metricKey() {
        return this;
    }
}
