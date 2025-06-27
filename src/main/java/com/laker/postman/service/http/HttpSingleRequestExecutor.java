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

    public static EventSource executeSSE(PreparedRequest req, EventSourceListener listener) throws Exception {
        return HttpService.sendSseRequest(req.url, req.method, req.headers, req.body, req.followRedirects, listener);
    }

    public static okhttp3.WebSocket executeWebSocket(PreparedRequest req, WebSocketListener listener) throws Exception {
        return HttpService.sendWebSocket(req.url, req.method, req.headers, req.body, req.followRedirects, listener);
    }

    private static HttpResponse sendRequestByType(PreparedRequest req) throws Exception {
        if (req.isMultipart) {
            return HttpService.sendRequestWithMultipart(req.url, req.method, req.headers, req.formData, req.formFiles, req.followRedirects);
        } else if (req.urlencoded != null && !req.urlencoded.isEmpty()) {
            return HttpService.sendRequestWithForm(req.url, req.method, req.headers, req.urlencoded, req.followRedirects);
        } else {
            return HttpService.sendRequest(req.url, req.method, req.headers, req.body, req.followRedirects);
        }
    }
}