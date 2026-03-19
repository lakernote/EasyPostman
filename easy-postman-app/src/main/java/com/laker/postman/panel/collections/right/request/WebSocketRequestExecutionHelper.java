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
    private final Runnable clearCurrentWorker;
    private final BooleanSupplier disposedSupplier;

    WebSocketRequestExecutionHelper(ResponsePanel responsePanel,
                                    RequestExecutionUiHelper requestExecutionUiHelper,
                                    RequestStreamUiHelper requestStreamUiHelper,
                                    RequestResponseHelper requestResponseHelper,
                                    Consumer<WebSocket> currentWebSocketSetter,
                                    Consumer<String> currentConnectionIdSetter,
                                    Supplier<String> currentConnectionIdSupplier,
                                    Runnable clearCurrentWorker,
                                    BooleanSupplier disposedSupplier) {
        this.responsePanel = responsePanel;
        this.requestExecutionUiHelper = requestExecutionUiHelper;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.requestResponseHelper = requestResponseHelper;
        this.currentWebSocketSetter = currentWebSocketSetter;
        this.currentConnectionIdSetter = currentConnectionIdSetter;
        this.currentConnectionIdSupplier = currentConnectionIdSupplier;
        this.clearCurrentWorker = clearCurrentWorker;
        this.disposedSupplier = disposedSupplier;
    }

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        final String connectionId = UUID.randomUUID().toString();
        currentConnectionIdSetter.accept(connectionId);

        return new SwingWorker<>() {
            final HttpResponse resp = new HttpResponse();
            long startTime;
            volatile boolean closed = false;

            @Override
            protected Void doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    log.debug("Starting WebSocket connection with ID: {}", connectionId);

                    HttpSingleRequestExecutor.executeWebSocket(req, new WebSocketListener() {
                        @Override
                        public void onOpen(WebSocket webSocket, Response response) {
                            if (!isActiveConnection(connectionId)) {
                                log.debug("Ignoring onOpen callback for expired connection ID: {}, current ID: {}",
                                        connectionId, currentConnectionIdSupplier.get());
                                webSocket.close(NORMAL_CLOSURE, "Connection expired");
                                return;
                            }

                            resp.headers = new LinkedHashMap<>();
                            for (String name : response.headers().names()) {
                                resp.addHeader(name, response.headers(name));
                            }
                            resp.code = response.code();
                            resp.protocol = response.protocol().toString();
                            currentWebSocketSetter.accept(webSocket);
                            SwingUtilities.invokeLater(() -> {
                                if (!isActiveConnection(connectionId)) {
                                    return;
                                }
                                requestExecutionUiHelper.updateUIForResponse(resp);
                                requestExecutionUiHelper.activateWebSocketBodyTab();
                                requestExecutionUiHelper.switchSendButtonToClose();
                                requestExecutionUiHelper.setWebSocketConnected(true);
                            });
                            if (isActiveConnection(connectionId)) {
                                requestStreamUiHelper.appendWebSocketMessage(MessageType.CONNECTED, response.message());
                            }
                        }

                        @Override
                        public void onMessage(WebSocket webSocket, String text) {
                            if (!isActiveConnection(connectionId)) {
                                log.debug("Ignoring onMessage callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            requestStreamUiHelper.appendWebSocketMessage(
                                    MessageType.RECEIVED, text, requestResponseHelper.handleStreamMessage(pipeline, text));
                        }

                        @Override
                        public void onMessage(WebSocket webSocket, ByteString bytes) {
                            if (!isActiveConnection(connectionId)) {
                                log.debug("Ignoring onMessage(binary) callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            requestStreamUiHelper.appendWebSocketMessage(MessageType.BINARY, bytes.hex());
                        }

                        @Override
                        public void onClosing(WebSocket webSocket, int code, String reason) {
                            if (isActiveConnection(connectionId)) {
                                log.debug("closing WebSocket: code={}, reason={}", code, reason);
                                handleWebSocketClose();
                            }
                        }

                        @Override
                        public void onClosed(WebSocket webSocket, int code, String reason) {
                            if (isActiveConnection(connectionId)) {
                                log.debug("closed WebSocket: code={}, reason={}", code, reason);
                                requestStreamUiHelper.appendWebSocketMessage(MessageType.CLOSED, code + " " + reason);
                                handleWebSocketClose();
                            }
                        }

                        private void handleWebSocketClose() {
                            if (closed) {
                                return;
                            }
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            currentWebSocketSetter.accept(null);
                            clearCurrentWorker.run();
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                requestExecutionUiHelper.updateUIForResponse(resp);
                                requestExecutionUiHelper.resetSendButton();
                                requestExecutionUiHelper.setWebSocketConnected(false);
                            });
                        }

                        @Override
                        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                            if (!isActiveConnection(connectionId)) {
                                log.debug("Ignoring onFailure callback for expired connection ID: {}", connectionId);
                                return;
                            }
                            log.error("WebSocket error", t);
                            requestStreamUiHelper.appendWebSocketMessage(MessageType.WARNING, t.getMessage());
                            closed = true;
                            resp.costMs = System.currentTimeMillis() - startTime;
                            currentWebSocketSetter.accept(null);
                            clearCurrentWorker.run();
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                String errorMsg = response != null
                                        ? I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage() + " (" + response.code() + ")")
                                        : I18nUtil.getMessage(MessageKeys.WEBSOCKET_FAILED, t.getMessage());
                                NotificationUtil.showError(errorMsg);
                                requestExecutionUiHelper.updateUIForResponse(resp);
                                requestExecutionUiHelper.resetSendButton();
                                requestExecutionUiHelper.setWebSocketConnected(false);
                            });
                        }
                    });
                    SwingUtilities.invokeLater(() -> {
                        if (!isActiveConnection(connectionId)) {
                            return;
                        }
                        responsePanel.setResponseTabButtonsEnable(true);
                    });
                } catch (Exception ex) {
                    log.error("Error executing WebSocket request: {} - {}", req.url, ex.getMessage(), ex);
                    clearCurrentWorker.run();
                    SwingUtilities.invokeLater(() -> {
                        if (disposedSupplier.getAsBoolean()) {
                            return;
                        }
                        requestExecutionUiHelper.updateUIForResponse(null);
                        NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.WEBSOCKET_ERROR, ex.getMessage()));
                        requestExecutionUiHelper.setWebSocketConnected(false);
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                if (!disposedSupplier.getAsBoolean() && connectionId.equals(currentConnectionIdSupplier.get())) {
                    requestResponseHelper.saveHistory(req, resp, "WebSocket request");
                }
            }
        };
    }

    private boolean isActiveConnection(String connectionId) {
        String currentConnectionId = currentConnectionIdSupplier.get();
        return !disposedSupplier.getAsBoolean()
                && currentConnectionId != null
                && !currentConnectionId.isBlank()
                && connectionId.equals(currentConnectionId);
    }
}
