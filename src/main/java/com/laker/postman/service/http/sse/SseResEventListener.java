package com.laker.postman.service.http.sse;

import com.laker.postman.model.HttpResponse;
import okhttp3.internal.sse.ServerSentEventReader;

public interface SseResEventListener extends ServerSentEventReader.Callback {
    void onOpen(HttpResponse response);
}
