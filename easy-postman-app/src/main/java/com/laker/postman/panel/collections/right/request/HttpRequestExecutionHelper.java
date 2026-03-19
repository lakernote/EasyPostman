package com.laker.postman.panel.collections.right.request;

import com.laker.postman.common.exception.DownloadCancelledException;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.MessageType;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.collections.right.request.sub.ResponsePanel;
import com.laker.postman.panel.sidebar.ConsolePanel;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.http.RedirectHandler;
import com.laker.postman.service.http.sse.SseResEventListener;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
final class HttpRequestExecutionHelper {
    private final ResponsePanel responsePanel;
    private final RequestExecutionUiHelper requestExecutionUiHelper;
    private final RequestStreamUiHelper requestStreamUiHelper;
    private final RequestResponseHelper requestResponseHelper;
    private final Runnable convertCurrentRequestToSse;
    private final Consumer<Boolean> httpSseStreamOpenedSetter;
    private final Supplier<Boolean> httpSseStreamOpenedSupplier;
    private final Runnable clearCurrentWorker;
    private final BooleanSupplier disposedSupplier;

    HttpRequestExecutionHelper(ResponsePanel responsePanel,
                               RequestExecutionUiHelper requestExecutionUiHelper,
                               RequestStreamUiHelper requestStreamUiHelper,
                               RequestResponseHelper requestResponseHelper,
                               Runnable convertCurrentRequestToSse,
                               Consumer<Boolean> httpSseStreamOpenedSetter,
                               Supplier<Boolean> httpSseStreamOpenedSupplier,
                               Runnable clearCurrentWorker,
                               BooleanSupplier disposedSupplier) {
        this.responsePanel = responsePanel;
        this.requestExecutionUiHelper = requestExecutionUiHelper;
        this.requestStreamUiHelper = requestStreamUiHelper;
        this.requestResponseHelper = requestResponseHelper;
        this.convertCurrentRequestToSse = convertCurrentRequestToSse;
        this.httpSseStreamOpenedSetter = httpSseStreamOpenedSetter;
        this.httpSseStreamOpenedSupplier = httpSseStreamOpenedSupplier;
        this.clearCurrentWorker = clearCurrentWorker;
        this.disposedSupplier = disposedSupplier;
    }

    SwingWorker<Void, Void> createWorker(PreparedRequest req, ScriptExecutionPipeline pipeline, int maxRedirectCount) {
        return new SwingWorker<>() {
            HttpResponse resp;
            final boolean expectedSse = HttpUtil.isSSERequest(req);
            final StringBuilder sseBodyBuilder = new StringBuilder();
            final long sseStartTime = System.currentTimeMillis();

            @Override
            protected Void doInBackground() {
                try {
                    responsePanel.setResponseTabButtonsEnable(true);
                    responsePanel.switchTabButtonHttpOrSse(expectedSse ? "sse" : "http");
                    resp = RedirectHandler.executeWithRedirects(req, maxRedirectCount, new SseResEventListener() {
                        @Override
                        public void onOpen(HttpResponse response) {
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                httpSseStreamOpenedSetter.accept(true);
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
                        public void onClosed(HttpResponse response) {
                            requestStreamUiHelper.finalizeSseResponse(response, sseBodyBuilder, sseStartTime);
                            SwingUtilities.invokeLater(() -> {
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                httpSseStreamOpenedSetter.accept(false);
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
                                if (disposedSupplier.getAsBoolean()) {
                                    return;
                                }
                                httpSseStreamOpenedSetter.accept(false);
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
                    ConsolePanel.appendLog("[Error] " + ex.getMessage(), ConsolePanel.LogType.ERROR);
                    if (!disposedSupplier.getAsBoolean()) {
                        NotificationUtil.showError(ex.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                if (disposedSupplier.getAsBoolean()) {
                    httpSseStreamOpenedSetter.accept(false);
                    clearCurrentWorker.run();
                    return;
                }
                boolean keepSseView = (resp != null && resp.isSse) || (isCancelled() && httpSseStreamOpenedSupplier.get());
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
                    httpSseStreamOpenedSetter.accept(false);
                }
                requestExecutionUiHelper.resetSendButton();
                clearCurrentWorker.run();
            }
        };
    }
}
