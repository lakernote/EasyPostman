package com.laker.postman.service.http.sse;

import com.laker.postman.model.HttpResponse;

// SSE UI回调接口
public interface SseUiCallback {
    void onOpen(HttpResponse resp, String headersText);

    void onEvent(String id, String type, String data);

    void onClosed(HttpResponse resp);

    void onFailure(String errorMsg, HttpResponse resp);
}
