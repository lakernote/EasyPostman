package com.laker.postman.http.runtime.transport;

import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.observation.NetworkLogEventStage;
import com.laker.postman.http.runtime.observation.NetworkLogSupport;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

/**
 * SSE 网络日志装饰器：只记录握手响应和生命周期，不改变业务回调语义。
 */
final class SseNetworkLogEventSourceListener extends EventSourceListener {
    private final EventSourceListener delegate;
    private final PreparedRequest preparedRequest;

    SseNetworkLogEventSourceListener(EventSourceListener delegate, PreparedRequest preparedRequest) {
        this.delegate = delegate == null ? new EventSourceListener() {
        } : delegate;
        this.preparedRequest = preparedRequest;
    }

    @Override
    public void onOpen(EventSource eventSource, Response response) {
        logResponseSnapshot(response);
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
        logResponseSnapshot(response);
        if (t != null) {
            log(NetworkLogEventStage.CALL_FAILED, t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage());
        }
        delegate.onFailure(eventSource, t, response);
    }

    private void logResponseSnapshot(Response response) {
        if (response == null) {
            return;
        }
        log(NetworkLogEventStage.RESPONSE_HEADERS_END,
                RealtimeHandshakeNetworkLogFormatter.formatResponseSnapshot(preparedRequest, response));
    }

    private void log(NetworkLogEventStage stage, String message) {
        NetworkLogSupport.append(preparedRequest, stage, message);
    }
}
