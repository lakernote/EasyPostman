package com.laker.postman.service.http;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

@Slf4j
@UtilityClass
public class HttpSingleRequestExecutor {

    public static HttpResponse executeHttp(PreparedRequest req) throws Exception {
        return HttpService.sendRequest(req);
    }

    public static EventSource executeSSE(PreparedRequest req, EventSourceListener listener) {
        return HttpService.sendSseRequest(req, listener);
    }

    public static void executeWebSocket(PreparedRequest req, WebSocketListener listener) {
        HttpService.sendWebSocket(req, listener);
    }
}