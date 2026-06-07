package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.stream.MessageType;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.http.runtime.transport.DefaultHttpTransport;
import com.laker.postman.http.runtime.transport.HttpExchangeTraceSupport;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.transport.RealtimeConnectionOptions;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CancellationException;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class WebSocketRequestExecutor {
    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;

    private final ResponsePanel responsePanel;
    private final RequestExecutionUiUpdater requestExecutionUiUpdater;
    private final RequestStreamUiAppender requestStreamUiAppender;
    private final RequestResponseHandler requestResponseHandler;
    private final RequestExecutionState requestExecutionState;
    private final HttpTransport httpTransport = new DefaultHttpTransport();

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        req.collectBasicInfo = true;
        req.collectMetricsInfo = true;
        req.collectEventInfo = true;
        req.enableNetworkLog = true;
        WebSocketSession session = new WebSocketSession(req, pipeline);
        requestExecutionState.beginWebSocketConnection(session.connectionId());

        class WebSocketWorker extends SwingWorker<Void, Void> implements UserClosableWebSocketWorker {
            @Override
            protected Void doInBackground() {
                try {
                    session.markStarted();
                    log.debug("Starting WebSocket connection with ID: {}", session.connectionId());

                    requestExecutionState.attachWebSocketConnection(httpTransport.openWebSocket(
                            req,
                            session.newListener(),
                            RealtimeConnectionOptions.defaults()
                    ));
                    SwingUtilities.invokeLater(session::enableResponseTabsIfActive);
                    session.awaitCompletion();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    session.handleExecutionException(ex);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (CancellationException ignored) {
                    // User-initiated cancel/dispose should not persist history.
                } catch (Exception ex) {
                    log.debug("WebSocket worker finished with exception", ex);
                }

                session.saveHistoryIfNeeded();
                session.clearWorkerIfCurrent(this);
            }

            @Override
            public void requestUserClose() {
                session.requestUserClose(this);
            }
        }

        return new WebSocketWorker();
    }

    private final class WebSocketSession {
        private final PreparedRequest req;
        private final ScriptExecutionPipeline pipeline;
        private final String connectionId = UUID.randomUUID().toString();
        private final WebSocketExecutionState executionState = new WebSocketExecutionState(connectionId);
        private final HttpResponse response = new HttpResponse();
        private final StringBuilder bodyBuilder = new StringBuilder();
        private long queueStartMs;

        private WebSocketSession(PreparedRequest req, ScriptExecutionPipeline pipeline) {
            this.req = req;
            this.pipeline = pipeline;
        }

        String connectionId() {
            return connectionId;
        }

        void markStarted() {
            queueStartMs = System.currentTimeMillis();
        }

        WebSocketListener newListener() {
            return new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    if (!shouldHandleActiveCallback("onOpen")) {
                        webSocket.close(WEBSOCKET_NORMAL_CLOSURE, "Connection expired");
                        return;
                    }

                    applyHandshakeResponse(response);
                    SwingUtilities.invokeLater(() -> {
                        if (!shouldHandleActiveCallback(null)) {
                            return;
                        }
                        requestExecutionUiUpdater.updateUIForResponse(WebSocketSession.this.response);
                        requestExecutionUiUpdater.activateWebSocketBodyTab();
                        requestExecutionUiUpdater.switchSendButtonToClose();
                        requestExecutionUiUpdater.setWebSocketConnected(true);
                        responsePanel.setRequestDetails(req);
                        responsePanel.setResponseDetails(WebSocketSession.this.response);
                    });
                    if (shouldHandleActiveCallback(null)) {
                        requestStreamUiAppender.appendWebSocketMessage(MessageType.CONNECTED, response.message());
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (!shouldHandleActiveCallback("onMessage")) {
                        return;
                    }
                    requestStreamUiAppender.appendWebSocketMessage(
                            MessageType.RECEIVED, text, requestResponseHandler.handleStreamMessage(pipeline, text));
                    requestStreamUiAppender.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, text);
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    if (!shouldHandleActiveCallback("onMessage(binary)")) {
                        return;
                    }
                    String hex = bytes.hex();
                    requestStreamUiAppender.appendWebSocketMessage(MessageType.BINARY, hex);
                    requestStreamUiAppender.appendWebSocketRawEvent(bodyBuilder, MessageType.BINARY, hex);
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    if (shouldHandleActiveCallback(null)) {
                        log.debug("closing WebSocket: code={}, reason={}", code, reason);
                    }
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    if (!shouldHandleActiveCallback("onClosed") || !executionState.markClosed()) {
                        return;
                    }
                    log.debug("closed WebSocket: code={}, reason={}", code, reason);
                    appendTerminalEvent(MessageType.CLOSED, code + " " + reason);
                    finishTerminalResponse();
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    if (!shouldHandleActiveCallback("onFailure") || !executionState.markFailed()) {
                        return;
                    }
                    log.error("WebSocket error", t);
                    appendTerminalEvent(MessageType.WARNING, t.getMessage());
                    finishTerminalResponse(() -> {
                        String errorMsg = response != null
                                ? I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage() + " (" + response.code() + ")")
                                : I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage());
                        NotificationUtil.showError(errorMsg);
                    });
                }
            };
        }

        void enableResponseTabsIfActive() {
            if (!shouldHandleActiveCallback(null)) {
                return;
            }
            responsePanel.setResponseTabButtonsEnable(true);
        }

        void awaitCompletion() throws InterruptedException {
            executionState.awaitCompletion();
        }

        void handleExecutionException(Exception ex) {
            log.error("Error executing WebSocket request: {} - {}", req.url, ex.getMessage(), ex);
            SwingUtilities.invokeLater(() -> {
                if (requestExecutionState.isDisposed()) {
                    return;
                }
                requestExecutionUiUpdater.updateUIForResponse(null);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WEBSOCKET_ERROR, ex.getMessage()));
                requestExecutionUiUpdater.setWebSocketConnected(false);
            });
        }

        void saveHistoryIfNeeded() {
            if (executionState.shouldSaveHistory(requestExecutionState.isDisposed())) {
                requestResponseHandler.saveHistory(req, response, "WebSocket request");
            }
        }

        void clearWorkerIfCurrent(SwingWorker<Void, Void> worker) {
            requestExecutionState.clearCurrentWorkerIf(worker);
        }

        void requestUserClose(SwingWorker<Void, Void> worker) {
            if (!executionState.markClosed()) {
                return;
            }
            requestExecutionState.clearCurrentWebSocket();
            requestExecutionState.clearCurrentWebSocketConnectionId();
            appendTerminalEvent(MessageType.WARNING, "User canceled");
            finalizeResponse();
            runUiTeardown(() -> {
            });
            clearWorkerIfCurrent(worker);
        }

        private void applyHandshakeResponse(Response response) {
            this.response.headers = new LinkedHashMap<>();
            for (String name : response.headers().names()) {
                this.response.addHeader(name, response.headers(name));
            }
            this.response.code = response.code();
            this.response.protocol = response.protocol().toString();
            HttpExchangeTraceSupport.attachToResponse(this.response, queueStartMs, req);
        }

        private void appendTerminalEvent(MessageType type, String text) {
            requestStreamUiAppender.appendWebSocketMessage(type, text);
            requestStreamUiAppender.appendWebSocketRawEvent(bodyBuilder, type, text);
        }

        private void finishTerminalResponse(Runnable afterUiReset) {
            requestExecutionState.clearCurrentWebSocket();
            finalizeResponse();
            SwingUtilities.invokeLater(() -> runUiTeardown(afterUiReset));
        }

        private void finishTerminalResponse() {
            finishTerminalResponse(() -> {
            });
        }

        private void finalizeResponse() {
            requestStreamUiAppender.finalizeWebSocketResponse(response, bodyBuilder, queueStartMs);
        }

        private void runUiTeardown(Runnable afterUiReset) {
            if (requestExecutionState.isDisposed()) {
                return;
            }
            requestExecutionUiUpdater.updateUIForResponse(response);
            responsePanel.setRequestDetails(req);
            responsePanel.setResponseDetails(response);
            requestExecutionUiUpdater.resetSendButton();
            requestExecutionUiUpdater.setWebSocketConnected(false);
            afterUiReset.run();
        }

        private boolean shouldHandleActiveCallback(String callbackName) {
            boolean active = executionState.shouldHandleActiveCallback(
                    requestExecutionState.currentWebSocketConnectionId(),
                    requestExecutionState.isDisposed()
            );
            if (!active && callbackName != null) {
                log.debug("Ignoring {} callback for expired connection ID: {}", callbackName, connectionId);
            }
            return active;
        }

    }
}
