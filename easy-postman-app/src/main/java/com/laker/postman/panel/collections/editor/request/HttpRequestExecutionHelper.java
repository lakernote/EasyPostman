package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.error.DownloadCancelledException;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.stream.MessageType;
import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.http.runtime.error.NetworkErrorMessageResolver;
import com.laker.postman.http.request.HttpRequestProtocol;
import com.laker.postman.http.runtime.redirect.HttpRedirectExecutor;
import com.laker.postman.http.runtime.sse.SseResponseCallback;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.InterruptedIOException;
import java.util.List;

@Slf4j
final class HttpRequestExecutionHelper {
    private final ResponsePanel responsePanel;
    private final RequestExecutionUiHelper requestExecutionUiHelper;
    private final RequestStreamUiHelper requestStreamUiHelper;
    private final RequestResponseHelper requestResponseHelper;
    private final HttpRedirectExecutor redirectExecutor;
    private final Runnable convertCurrentRequestToSse;
    private final RequestExecutionState executionState;

    HttpRequestExecutionHelper(ResponsePanel responsePanel,
                               RequestExecutionUiHelper requestExecutionUiHelper,
                               RequestStreamUiHelper requestStreamUiHelper,
                               RequestResponseHelper requestResponseHelper,
                               Runnable convertCurrentRequestToSse,
                               RequestExecutionState executionState) {
        this.responsePanel = responsePanel;
        this.requestExecutionUiHelper = requestExecutionUiHelper;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.requestResponseHelper = requestResponseHelper;
        this.redirectExecutor = new HttpRedirectExecutor();
        this.convertCurrentRequestToSse = convertCurrentRequestToSse;
        this.executionState = executionState;
    }

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline, int maxRedirectCount) {
        return new SwingWorker<>() {
            HttpResponse resp;
            final boolean expectedSse = HttpRequestProtocol.isSse(req);
            final StringBuilder sseBodyBuilder = new StringBuilder();
            final long sseStartTime = System.currentTimeMillis();

            @Override
            protected Void doInBackground() {
                try {
                    responsePanel.setResponseTabButtonsEnable(true);
                    responsePanel.switchTabButtonHttpOrSse(expectedSse ? "sse" : "http");
                    resp = redirectExecutor.executeWithRedirects(req, maxRedirectCount, new SseResponseCallback() {
                        @Override
                        public void onOpen(HttpResponse response) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                executionState.markAutoDetectedHttpSseOpen();
                                convertCurrentRequestToSse.run();
                                responsePanel.switchTabButtonHttpOrSse("sse");
                                requestExecutionUiHelper.updateUIForResponse(response);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(response);
                                requestStreamUiHelper.appendSseMessage(MessageType.CONNECTED, null, "open", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CONNECTED), null);
                            });
                        }

                        @Override
                        public void onEvent(String id, String type, String data) {
                            requestStreamUiHelper.appendSseRawEvent(sseBodyBuilder, id, type, data);
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                List<TestResult> testResults = requestResponseHelper.handleStreamMessage(pipeline, data);
                                requestStreamUiHelper.appendSseMessage(MessageType.RECEIVED, id, type, null, data, testResults);
                            });
                        }

                        @Override
                        public void onRetryChange(long retryMs) {
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                requestStreamUiHelper.appendSseMessage(MessageType.INFO, null, "retry", retryMs,
                                        I18nUtil.getMessage(MessageKeys.SSE_RETRY_UPDATED, retryMs), null);
                            });
                        }

                        @Override
                        public void onClosed(HttpResponse response) {
                            requestStreamUiHelper.finalizeSseResponse(response, sseBodyBuilder, sseStartTime);
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                executionState.clearAutoDetectedHttpSseOpen();
                                requestExecutionUiHelper.updateUIForResponse(response);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(response);
                                requestStreamUiHelper.appendSseMessage(MessageType.CLOSED, null, "closed", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_CLOSED), null);
                            });
                        }

                        @Override
                        public void onFailure(String errorMsg, HttpResponse response) {
                            requestStreamUiHelper.finalizeSseResponse(response, sseBodyBuilder, sseStartTime);
                            SwingUtilities.invokeLater(() -> {
                                if (executionState.isDisposed()) {
                                    return;
                                }
                                executionState.clearAutoDetectedHttpSseOpen();
                                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SSE_FAILED, errorMsg));
                                requestExecutionUiHelper.updateUIForResponse(response);
                                responsePanel.setRequestDetails(req);
                                responsePanel.setResponseDetails(response);
                                requestStreamUiHelper.appendSseMessage(MessageType.WARNING, null, "failure", null,
                                        I18nUtil.getMessage(MessageKeys.SSE_STREAM_FAILED, errorMsg), null);
                            });
                        }
                    });
                } catch (DownloadCancelledException ex) {
                    log.info("User canceled download for request: {} {}", req.method, req.url);
                } catch (InterruptedIOException ex) {
                    log.warn("Request interrupted: {} {} - {}", req.method, req.url, ex.getMessage());
                } catch (Exception ex) {
                    log.error("Error executing HTTP request: {} {} - {}", req.method, req.url, ex.getMessage(), ex);
                    String userFriendlyMessage = NetworkErrorMessageResolver.toUserFriendlyMessage(ex);
                    ConsolePanel.appendLog("[Error] " + userFriendlyMessage, ConsolePanel.LogType.ERROR);
                    if (!executionState.isDisposed()) {
                        NotificationUtil.showError(userFriendlyMessage);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (executionState.isDisposed()) {
                        executionState.clearAutoDetectedHttpSseOpen();
                        return;
                    }
                    boolean keepSseView = (resp != null && resp.isSse)
                            || (isCancelled() && executionState.isAutoDetectedHttpSseOpen());
                    responsePanel.switchTabButtonHttpOrSse(keepSseView ? "sse" : "http");
                    requestExecutionUiHelper.updateUIForResponse(resp);
                    if (resp != null && !resp.isSse) {
                        requestResponseHelper.handleResponse(pipeline, req, resp);
                    } else if (resp != null) {
                        requestResponseHelper.recordExchange(req, resp);
                        responsePanel.setRequestDetails(req);
                        responsePanel.setResponseDetails(resp);
                        requestResponseHelper.saveHistory(req, resp, "SSE request");
                    }
                    if (!keepSseView) {
                        executionState.clearAutoDetectedHttpSseOpen();
                    }
                } finally {
                    requestExecutionUiHelper.resetSendButton();
                    executionState.clearCurrentWorkerIf(this);
                }
            }
        };
    }
}
