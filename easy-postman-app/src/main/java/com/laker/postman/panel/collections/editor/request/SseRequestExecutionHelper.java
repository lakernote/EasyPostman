package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.stream.MessageType;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.http.runtime.transport.DefaultHttpTransport;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeConnectionOptions;
import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.runtime.sse.SseStreamEventListener;
import com.laker.postman.http.runtime.sse.SseStreamCallback;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
final class SseRequestExecutionHelper {
    private final ResponsePanel responsePanel;
    private final RequestExecutionUiHelper requestExecutionUiHelper;
    private final RequestStreamUiHelper requestStreamUiHelper;
    private final RequestResponseHelper requestResponseHelper;
    private final AtomicBoolean currentSseCancelled;
    private final Consumer<RealtimeConnectionHandle> currentEventSourceSetter;
    private final Runnable clearCurrentEventSource;
    private final Runnable clearCurrentWorker;
    private final BooleanSupplier disposedSupplier;
    private final HttpTransport httpTransport;

    SseRequestExecutionHelper(ResponsePanel responsePanel,
                              RequestExecutionUiHelper requestExecutionUiHelper,
                              RequestStreamUiHelper requestStreamUiHelper,
                              RequestResponseHelper requestResponseHelper,
                              AtomicBoolean currentSseCancelled,
                              Consumer<RealtimeConnectionHandle> currentEventSourceSetter,
                              Runnable clearCurrentEventSource,
                              Runnable clearCurrentWorker,
                              BooleanSupplier disposedSupplier) {
        this.responsePanel = responsePanel;
        this.requestExecutionUiHelper = requestExecutionUiHelper;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.requestResponseHelper = requestResponseHelper;
        this.currentSseCancelled = currentSseCancelled;
        this.currentEventSourceSetter = currentEventSourceSetter;
        this.clearCurrentEventSource = clearCurrentEventSource;
        this.clearCurrentWorker = clearCurrentWorker;
        this.disposedSupplier = disposedSupplier;
        this.httpTransport = new DefaultHttpTransport();
    }

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        req.collectBasicInfo = true;
        req.collectEventInfo = true;
        req.enableNetworkLog = false;
        return new SwingWorker<>() {
            HttpResponse resp;
            StringBuilder sseBodyBuilder;
            long startTime;

            @Override
            protected Void doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    resp = new HttpResponse();
                    sseBodyBuilder = new StringBuilder();
                    SseStreamCallback callback = new SseStreamCallback() {
                        @Override
                        public void onOpen(HttpResponse r, String headersText) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                requestExecutionUiHelper.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestStreamUiHelper.appendSseMessage(MessageType.CONNECTED, null, "open", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CONNECTED), null);
                            });
                        }

                        @Override
                        public void onEvent(String id, String type, String data) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                List<TestResult> testResults = requestResponseHelper.handleStreamMessage(pipeline, data);
                                requestStreamUiHelper.appendSseMessage(MessageType.RECEIVED, id, type, null, data, testResults);
                            });
                        }

                        @Override
                        public void onRetryChange(long retryMs) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                requestStreamUiHelper.appendSseMessage(MessageType.INFO, null, "retry", retryMs,
                                        I18nUtil.getMessage(MessageKeys.SSE_RETRY_UPDATED, retryMs), null);
                            });
                        }

                        @Override
                        public void onClosed(HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                requestExecutionUiHelper.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestExecutionUiHelper.resetSendButton();
                                requestStreamUiHelper.appendSseMessage(MessageType.CLOSED, null, "closed", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CLOSED), null);
                                clearCurrentEventSource.run();
                                clearCurrentWorker.run();
                            });
                        }

                        @Override
                        public void onFailure(String errorMsg, HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg));
                                requestExecutionUiHelper.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestExecutionUiHelper.resetSendButton();
                                requestStreamUiHelper.appendSseMessage(MessageType.WARNING, null, "failure", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_FAILED, errorMsg), null);
                                clearCurrentEventSource.run();
                                clearCurrentWorker.run();
                            });
                        }
                    };
                    currentSseCancelled.set(false);
                    currentEventSourceSetter.accept(httpTransport.openSse(
                            req,
                            new SseStreamEventListener(callback, resp, sseBodyBuilder, startTime, currentSseCancelled::get),
                            RealtimeConnectionOptions.defaults()
                    ));
                    responsePanel.setResponseTabButtonsEnable(true);
                } catch (Exception ex) {
                    log.error("Error executing SSE request: {} - {}", req.url, ex.getMessage(), ex);
                    String userFriendlyMessage = NetworkErrorMessageResolver.toUserFriendlyMessage(ex);
                    SwingUtilities.invokeLater(() -> {
                        if (disposedSupplier.getAsBoolean()) {
                            return;
                        }
                        responsePanel.setStatus(0);
                        NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SSE_ERROR, userFriendlyMessage));
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                if (!disposedSupplier.getAsBoolean() && resp != null) {
                    requestResponseHelper.saveHistory(req, resp, "SSE request");
                }
            }
        };
    }
}
