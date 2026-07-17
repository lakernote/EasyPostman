package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpCaptureProfile;
import com.laker.postman.http.runtime.model.HttpCaptureProfiles;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.stream.MessageType;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.http.runtime.transport.DefaultHttpTransport;
import com.laker.postman.http.runtime.transport.HttpTransport;
import com.laker.postman.http.runtime.transport.RealtimeConnectionOptions;
import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.runtime.sse.SseStreamEventListener;
import com.laker.postman.http.runtime.sse.SseStreamCallback;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class SseRequestExecutor {
    private final ResponsePanel responsePanel;
    private final RequestExecutionUiUpdater requestExecutionUiUpdater;
    private final RequestStreamUiAppender requestStreamUiAppender;
    private final RequestResponseHandler requestResponseHandler;
    private final RequestExecutionState executionState;
    private final HttpTransport httpTransport = new DefaultHttpTransport();

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline) {
        HttpCaptureProfiles.apply(req, HttpCaptureProfile.COLLECTION_DIAGNOSTIC);
        return new SwingWorker<>() {
            HttpResponse resp;
            StringBuilder sseBodyBuilder;
            long queueStartMs;

            @Override
            protected Void doInBackground() {
                try {
                    queueStartMs = System.currentTimeMillis();
                    resp = new HttpResponse();
                    sseBodyBuilder = new StringBuilder();
                    SseStreamCallback callback = new SseStreamCallback() {
                        @Override
                        public void onOpen(HttpResponse r, String headersText) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                requestExecutionUiUpdater.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestStreamUiAppender.appendSseMessage(MessageType.CONNECTED, null, "open", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CONNECTED), null);
                            });
                        }

                        @Override
                        public void onEvent(String id, String type, String data) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                List<TestResult> testResults = requestResponseHandler.handleStreamMessage(pipeline, data);
                                requestStreamUiAppender.appendSseMessage(MessageType.RECEIVED, id, type, null, data, testResults);
                            });
                        }

                        @Override
                        public void onRetryChange(long retryMs) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                requestStreamUiAppender.appendSseMessage(MessageType.INFO, null, "retry", retryMs,
                                        I18nUtil.getMessage(MessageKeys.SSE_RETRY_UPDATED, retryMs), null);
                            });
                        }

                        @Override
                        public void onClosed(HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                requestExecutionUiUpdater.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestExecutionUiUpdater.resetSendButton();
                                requestStreamUiAppender.appendSseMessage(MessageType.CLOSED, null, "closed", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CLOSED), null);
                                executionState.clearCurrentEventSource();
                                executionState.clearCurrentWorker();
                            });
                        }

                        @Override
                        public void onFailure(String errorMsg, HttpResponse r) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg));
                                requestExecutionUiUpdater.updateUIForResponse(r);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(r);
                                requestExecutionUiUpdater.resetSendButton();
                                requestStreamUiAppender.appendSseMessage(MessageType.WARNING, null, "failure", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_FAILED, errorMsg), null);
                                executionState.clearCurrentEventSource();
                                executionState.clearCurrentWorker();
                            });
                        }
                    };
                    executionState.resetSseCancelled();
                    executionState.startSseConnection(httpTransport.openSse(
                            req,
                            new SseStreamEventListener(callback, resp, sseBodyBuilder, queueStartMs,
                                    executionState::isSseCancelled, req),
                            RealtimeConnectionOptions.defaults()
                    ));
                    responsePanel.setResponseTabButtonsEnable(true);
                } catch (Exception ex) {
                    String logMessage = NetworkErrorMessageResolver.toLogMessage(ex);
                    if (ex instanceof java.net.UnknownHostException) {
                        log.error("Error executing SSE request: {} - {}", req.url, logMessage);
                    } else {
                        log.error("Error executing SSE request: {} - {}", req.url, logMessage, ex);
                    }
                    String userFriendlyMessage = NetworkErrorMessageResolver.toUserFriendlyMessage(ex);
                    SwingUtilities.invokeLater(() -> {
                        if (executionState.isDisposed()) {
                            return;
                        }
                        responsePanel.setStatus(0);
                        NotificationCenter.showError(I18nUtil.getMessage(MessageKeys.SSE_ERROR, userFriendlyMessage));
                    });
                }
                return null;
            }

            @Override
            protected void done() {
                if (!executionState.isDisposed() && resp != null) {
                    requestResponseHandler.saveHistory(req, resp, "SSE request");
                }
            }
        };
    }
}
