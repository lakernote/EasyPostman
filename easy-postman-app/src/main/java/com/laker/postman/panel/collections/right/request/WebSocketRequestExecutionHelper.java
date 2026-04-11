package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.MessageType;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
final class WebSocketRequestExecutionHelper {
    private static final int NORMAL_CLOSURE = 1000;

    private final ResponsePanel responsePanel;
    private final RequestExecutionUiHelper requestExecutionUiHelper;
    private final RequestStreamUiHelper requestStreamUiHelper;
    private final RequestResponseHelper requestResponseHelper;
    private final Consumer<WebSocket> currentWebSocketSetter;
    private final Consumer<String> currentConnectionIdSetter;
    private final Supplier<String> currentConnectionIdSupplier;
    private final Supplier<SwingWorker<Void, Void>> currentWorkerSupplier;
    private final Runnable clearCurrentWorker;
    private final BooleanSupplier disposedSupplier;

    WebSocketRequestExecutionHelper(ResponsePanel responsePanel,
                                    RequestExecutionUiHelper requestExecutionUiHelper,
                                    RequestStreamUiHelper requestStreamUiHelper,
                                    RequestResponseHelper requestResponseHelper,
                                    Consumer<WebSocket> currentWebSocketSetter,
                                    Consumer<String> currentConnectionIdSetter,
                                    Supplier<String> currentConnectionIdSupplier,
                                    Supplier<SwingWorker<Void, Void>> currentWorkerSupplier,
                                    Runnable clearCurrentWorker,
                                    BooleanSupplier disposedSupplier) {
        this.responsePanel = responsePanel;
        this.requestExecutionUiHelper = requestExecutionUiHelper;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.requestResponseHelper = requestResponseHelper;
        this.currentWebSocketSetter = currentWebSocketSetter;
        this.currentConnectionIdSetter = currentConnectionIdSetter;
        this.currentConnectionIdSupplier = currentConnectionIdSupplier;
        this.currentWorkerSupplier = currentWorkerSupplier;
        this.clearCurrentWorker = clearCurrentWorker;
        this.disposedSupplier = disposedSupplier;
    }

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        WebSocketSession session = new WebSocketSession(req, pipeline);
        currentConnectionIdSetter.accept(session.connectionId());

        class WebSocketWorker extends SwingWorker<Void, Void> implements UserClosableWebSocketWorker {
            @Override
            protected Void doInBackground() {
                try {
                    session.markStarted();
                    log.debug("Starting WebSocket connection with ID: {}", session.connectionId());

                    HttpSingleRequestExecutor.executeWebSocket(req, session.newListener());
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
        private long startTime;

        private WebSocketSession(PreparedRequest req, ScriptExecutionPipeline pipeline) {
            this.req = req;
            this.pipeline = pipeline;
        }

        String connectionId() {
            return connectionId;
        }

        void markStarted() {
            startTime = System.currentTimeMillis();
        }

        WebSocketListener newListener() {
            return new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    if (!shouldHandleActiveCallback("onOpen")) {
                        webSocket.close(NORMAL_CLOSURE, "Connection expired");
                        return;
                    }

                    applyHandshakeResponse(response);
                    currentWebSocketSetter.accept(webSocket);
                    SwingUtilities.invokeLater(() -> {
                        if (!shouldHandleActiveCallback(null)) {
                            return;
                        }
                        requestExecutionUiHelper.updateUIForResponse(WebSocketSession.this.response);
                        requestExecutionUiHelper.activateWebSocketBodyTab();
                        requestExecutionUiHelper.switchSendButtonToClose();
                        requestExecutionUiHelper.setWebSocketConnected(true);
                    });
                    if (shouldHandleActiveCallback(null)) {
                        requestStreamUiHelper.appendWebSocketMessage(MessageType.CONNECTED, response.message());
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (!shouldHandleActiveCallback("onMessage")) {
                        return;
                    }
                    requestStreamUiHelper.appendWebSocketMessage(
                            MessageType.RECEIVED, text, requestResponseHelper.handleStreamMessage(pipeline, text));
                    requestStreamUiHelper.appendWebSocketRawEvent(bodyBuilder, MessageType.RECEIVED, text);
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    if (!shouldHandleActiveCallback("onMessage(binary)")) {
                        return;
                    }
                    String hex = bytes.hex();
                    requestStreamUiHelper.appendWebSocketMessage(MessageType.BINARY, hex);
                    requestStreamUiHelper.appendWebSocketRawEvent(bodyBuilder, MessageType.BINARY, hex);
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
                if (disposedSupplier.getAsBoolean()) {
                    return;
                }
                requestExecutionUiHelper.updateUIForResponse(null);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WEBSOCKET_ERROR, ex.getMessage()));
                requestExecutionUiHelper.setWebSocketConnected(false);
            });
        }

        void saveHistoryIfNeeded() {
            if (executionState.shouldSaveHistory(disposedSupplier.getAsBoolean())) {
                requestResponseHelper.saveHistory(req, response, "WebSocket request");
            }
        }

        void clearWorkerIfCurrent(SwingWorker<Void, Void> worker) {
            if (currentWorkerSupplier.get() == worker) {
                clearCurrentWorker.run();
            }
        }

        void requestUserClose(SwingWorker<Void, Void> worker) {
            if (!executionState.markClosed()) {
                return;
            }
            currentWebSocketSetter.accept(null);
            currentConnectionIdSetter.accept(null);
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
        }

        private void appendTerminalEvent(MessageType type, String text) {
            requestStreamUiHelper.appendWebSocketMessage(type, text);
            requestStreamUiHelper.appendWebSocketRawEvent(bodyBuilder, type, text);
        }

        private void finishTerminalResponse(Runnable afterUiReset) {
            currentWebSocketSetter.accept(null);
            finalizeResponse();
            SwingUtilities.invokeLater(() -> runUiTeardown(afterUiReset));
        }

        private void finishTerminalResponse() {
            finishTerminalResponse(() -> {
            });
        }

        private void finalizeResponse() {
            requestStreamUiHelper.finalizeWebSocketResponse(response, bodyBuilder, startTime);
        }

        private void runUiTeardown(Runnable afterUiReset) {
            if (disposedSupplier.getAsBoolean()) {
                return;
            }
            requestExecutionUiHelper.updateUIForResponse(response);
            responsePanel.setResponseDetails(response);
            requestExecutionUiHelper.resetSendButton();
            requestExecutionUiHelper.setWebSocketConnected(false);
            afterUiReset.run();
        }

        private boolean shouldHandleActiveCallback(String callbackName) {
            boolean active = executionState.shouldHandleActiveCallback(
                    currentConnectionIdSupplier.get(),
                    disposedSupplier.getAsBoolean()
            );
            if (!active && callbackName != null) {
                log.debug("Ignoring {} callback for expired connection ID: {}", callbackName, connectionId);
            }
            return active;
        }

    }
}
