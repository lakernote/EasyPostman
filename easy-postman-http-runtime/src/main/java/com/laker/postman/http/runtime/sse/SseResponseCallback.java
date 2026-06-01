package com.laker.postman.http.runtime.sse;

import com.laker.postman.http.runtime.model.HttpResponse;
import okhttp3.internal.sse.ServerSentEventReader;

public interface SseResponseCallback extends ServerSentEventReader.Callback {
    void onOpen(HttpResponse response);

    default void onClosed(HttpResponse response) {
    }

    default void onFailure(String errorMsg, HttpResponse response) {
    }
}
