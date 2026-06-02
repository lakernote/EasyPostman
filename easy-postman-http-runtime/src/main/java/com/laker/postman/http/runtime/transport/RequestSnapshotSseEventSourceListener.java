package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSink;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

final class RequestSnapshotSseEventSourceListener extends EventSourceListener {
    private final EventSourceListener delegate;
    private final PreparedRequest preparedRequest;
    private final NetworkLogSink networkLogSink;

    RequestSnapshotSseEventSourceListener(EventSourceListener delegate, PreparedRequest preparedRequest) {
        this.delegate = delegate == null ? new EventSourceListener() {
        } : delegate;
        this.preparedRequest = preparedRequest;
        this.networkLogSink = preparedRequest == null || preparedRequest.networkLogSink == null
                ? NetworkLogSink.noop()
                : preparedRequest.networkLogSink;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        logResponseHeaders(response);
        log(NetworkLogEventStage.RESPONSE_BODY_START, "SSE stream opened");
        delegate.onOpen(eventSource, response);
    }

    @Override
    public void onEvent(EventSource eventSource, String id, String type, String data) {
        delegate.onEvent(eventSource, id, type, data);
    }

    @Override
    public void onClosed(EventSource eventSource) {
        log(NetworkLogEventStage.CALL_END, "SSE stream closed");
        delegate.onClosed(eventSource);
    }

    @Override
    public void onFailure(EventSource eventSource, Throwable t, Response response) {
        logResponseHeaders(response);
        if (t != null) {
            log(NetworkLogEventStage.CALL_FAILED, t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
        }
        delegate.onFailure(eventSource, t, response);
    }

    private void logResponseHeaders(Response response) {
        if (response == null) {
            return;
        }
        log(NetworkLogEventStage.RESPONSE_HEADERS_END, formatResponseHeaders(response));
    }

    private String formatResponseHeaders(Response response) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("Status: ").append(response.code()).append(" ").append(response.message()).append("\n");
        sb.append("Protocol: ").append(response.protocol()).append("\n");
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
            // Logging must not affect SSE callbacks.
        }
    }
}
