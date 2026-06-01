package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class RequestExecutionState {
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;

    private volatile SwingWorker<Void, Void> currentWorker;
    private volatile RealtimeConnectionHandle currentEventSource;
    private volatile RealtimeWebSocketConnection currentWebSocket;
    private volatile String currentWebSocketConnectionId;
    private volatile boolean autoDetectedHttpSseOpen;
    private volatile boolean disposed;
    private final AtomicBoolean currentSseCancelled = new AtomicBoolean(false);

    SwingWorker<Void, Void> currentWorker() {
        return currentWorker;
    }

    RealtimeConnectionHandle currentEventSource() {
        return currentEventSource;
    }

    RealtimeWebSocketConnection currentWebSocket() {
        return currentWebSocket;
    }

    String currentWebSocketConnectionId() {
        return currentWebSocketConnectionId;
    }

    boolean isDisposed() {
        return disposed;
    }

    boolean isAutoDetectedHttpSseOpen() {
        return autoDetectedHttpSseOpen;
    }

    boolean isSseCancelled() {
        return currentSseCancelled.get();
    }

    void startWorker(SwingWorker<Void, Void> worker) {
        currentWorker = worker;
    }

    void clearCurrentWorkerIf(SwingWorker<Void, Void> worker) {
        if (currentWorker == worker) {
            currentWorker = null;
        }
    }

    void clearCurrentWorker() {
        currentWorker = null;
    }

    void startSseConnection(RealtimeConnectionHandle eventSource) {
        currentEventSource = eventSource;
    }

    void markSseCancelled() {
        currentSseCancelled.set(true);
    }

    void resetSseCancelled() {
        currentSseCancelled.set(false);
    }

    void clearCurrentEventSource() {
        currentEventSource = null;
    }

    void beginWebSocketConnection(String connectionId) {
        currentWebSocketConnectionId = connectionId;
    }

    void attachWebSocketConnection(RealtimeWebSocketConnection webSocket) {
        currentWebSocket = webSocket;
    }

    void clearCurrentWebSocket() {
        currentWebSocket = null;
    }

    void clearCurrentWebSocketConnectionId() {
        currentWebSocketConnectionId = null;
    }

    void markAutoDetectedHttpSseOpen() {
        autoDetectedHttpSseOpen = true;
    }

    void clearAutoDetectedHttpSseOpen() {
        autoDetectedHttpSseOpen = false;
    }

    void disposeOpenConnections() {
        // 关闭请求标签时统一释放网络句柄，避免 UI 消失后 SSE/WS/Worker 仍在后台持有状态。
        disposed = true;

        RealtimeConnectionHandle eventSource = currentEventSource;
        if (eventSource != null) {
            markSseCancelled();
            eventSource.cancel();
            currentEventSource = null;
        }

        RealtimeWebSocketConnection webSocket = currentWebSocket;
        if (webSocket != null) {
            webSocket.close(WEBSOCKET_NORMAL_CLOSURE, "Tab closed");
            currentWebSocket = null;
        }

        currentWebSocketConnectionId = null;

        SwingWorker<Void, Void> worker = currentWorker;
        if (worker != null) {
            worker.cancel(true);
            currentWorker = null;
        }

        clearAutoDetectedHttpSseOpen();
    }
}
