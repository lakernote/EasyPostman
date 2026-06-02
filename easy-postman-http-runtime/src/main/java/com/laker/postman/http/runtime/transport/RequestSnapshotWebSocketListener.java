package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSink;
import com.laker.postman.http.runtime.okhttp.OkHttpRequestSnapshotCapture;
import com.laker.postman.request.model.HttpHeader;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

final class RequestSnapshotWebSocketListener extends WebSocketListener {
    private final WebSocketListener delegate;
    private final PreparedRequest preparedRequest;
    private final NetworkLogSink networkLogSink;

    RequestSnapshotWebSocketListener(WebSocketListener delegate, PreparedRequest preparedRequest) {
        this.delegate = delegate == null ? new WebSocketListener() {
        } : delegate;
        this.preparedRequest = preparedRequest;
        this.networkLogSink = preparedRequest == null || preparedRequest.networkLogSink == null
                ? NetworkLogSink.noop()
                : preparedRequest.networkLogSink;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        captureHandshake(response);
        delegate.onOpen(webSocket, response);
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        delegate.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        delegate.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        delegate.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        log(NetworkLogEventStage.CALL_END, "code=" + code + ", reason=" + reason);
        delegate.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        captureHandshake(response);
        log(NetworkLogEventStage.CALL_FAILED, t != null ? t.getMessage() : "");
        delegate.onFailure(webSocket, t, response);
    }

    private void captureHandshake(Response response) {
        if (response == null || response.request() == null) {
            return;
        }
        OkHttpRequestSnapshotCapture.capture(preparedRequest, response.request(), false);
        log(NetworkLogEventStage.REQUEST_HEADERS_END, formatRequestHeaders());
        log(NetworkLogEventStage.RESPONSE_HEADERS_END, formatResponseHeaders(response));
    }

    private String formatRequestHeaders() {
        if (preparedRequest == null || preparedRequest.sentHeadersList == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n");
        for (HttpHeader header : preparedRequest.sentHeadersList) {
            sb.append(header.getKey()).append(": ").append(header.getValue()).append("\n");
        }
        return sb.toString();
    }

    private String formatResponseHeaders(Response response) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("Status: ").append(response.code()).append("\n");
        for (int i = 0; i < response.headers().size(); i++) {
            sb.append(response.headers().name(i))
                    .append(": ")
                    .append(response.headers().value(i))
                    .append("\n");
        }
        return sb.toString();
    }

    private void log(NetworkLogEventStage stage, String message) {
        if (preparedRequest == null || !preparedRequest.enableNetworkLog) {
            return;
        }
        try {
            networkLogSink.append(stage, message, null);
        } catch (Throwable ignored) {
            // Logging must not affect WebSocket callbacks.
        }
    }
}
