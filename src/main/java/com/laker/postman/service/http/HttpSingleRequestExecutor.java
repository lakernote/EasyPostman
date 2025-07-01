package com.laker.postman.service.http;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocketListener;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

import java.util.List;

@Slf4j
public class HttpSingleRequestExecutor {

    public static HttpResponse execute(PreparedRequest req) throws Exception {
        HttpResponse resp = sendRequestByType(req);
        List<String> setCookieHeaders = HttpRequestUtil.extractSetCookieHeaders(resp);
        CookieService.handleSetCookie(req.url, setCookieHeaders);
        return resp;
    }

    public static EventSource executeSSE(PreparedRequest req, EventSourceListener listener) {
        return HttpService.sendSseRequest(req, listener);
    }

    public static okhttp3.WebSocket executeWebSocket(PreparedRequest req, WebSocketListener listener) {
        return HttpService.sendWebSocket(req, listener);
    }

    private static HttpResponse sendRequestByType(PreparedRequest req) throws Exception {
        if (req.isMultipart) {
            return HttpService.sendRequestWithMultipart(req);
        } else if (req.urlencoded != null && !req.urlencoded.isEmpty()) {
            return HttpService.sendRequestWithForm(req);
        } else {
            return HttpService.sendRequest(req);
        }
    }
}