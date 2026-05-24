package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.plan.PerformanceAssertionElement;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import com.laker.postman.service.js.ScriptExecutionResult;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.service.variable.ExecutionVariableContext;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@Slf4j
public class PerformanceRequestExecutor {

    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<EventSource> activeSseSources;
    private final Set<WebSocket> activeWebSockets;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final BooleanSupplier efficientModeSupplier;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;
    private final PerformanceRequestTransportExecutor transportExecutor;
    private final PerformanceRequestPostProcessor postProcessor;

    public PerformanceRequestExecutor(BooleanSupplier runningSupplier,
                                      Predicate<Throwable> cancelledChecker,
                                      Set<EventSource> activeSseSources,
                                      Set<WebSocket> activeWebSockets) {
        this(runningSupplier, cancelledChecker, activeSseSources, activeWebSockets, new PerformanceRealtimeMetrics());
    }

    public PerformanceRequestExecutor(BooleanSupplier runningSupplier,
                                      Predicate<Throwable> cancelledChecker,
                                      Set<EventSource> activeSseSources,
                                      Set<WebSocket> activeWebSockets,
                                      PerformanceRealtimeMetrics realtimeMetrics) {
        this(runningSupplier, cancelledChecker, activeSseSources, activeWebSockets, realtimeMetrics, () -> false,
                () -> SettingManager.DEFAULT_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB);
    }

    public PerformanceRequestExecutor(BooleanSupplier runningSupplier,
                                      Predicate<Throwable> cancelledChecker,
                                      Set<EventSource> activeSseSources,
                                      Set<WebSocket> activeWebSockets,
                                      PerformanceRealtimeMetrics realtimeMetrics,
                                      BooleanSupplier efficientModeSupplier,
                                      IntSupplier responseBodyPreviewLimitKbSupplier) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeSseSources = activeSseSources;
        this.activeWebSockets = activeWebSockets;
        this.realtimeMetrics = realtimeMetrics == null ? new PerformanceRealtimeMetrics() : realtimeMetrics;
        this.efficientModeSupplier = efficientModeSupplier == null ? () -> false : efficientModeSupplier;
        this.responseBodyPreviewLimitKbSupplier = responseBodyPreviewLimitKbSupplier == null
                ? () -> SettingManager.DEFAULT_PERFORMANCE_RESPONSE_BODY_PREVIEW_LIMIT_KB
                : responseBodyPreviewLimitKbSupplier;
        this.transportExecutor = new PerformanceRequestTransportExecutor(
                this.runningSupplier,
                this.cancelledChecker,
                this.activeSseSources,
                this.activeWebSockets,
                this.realtimeMetrics,
                this.responseBodyPreviewLimitKbSupplier
        );
        this.postProcessor = new PerformanceRequestPostProcessor(this.runningSupplier);
    }

    public PerformanceRequestExecutionResult execute(PerformanceRequestSampler requestSampler,
                                                     ExecutionVariableContext iterationContext) {
        HttpRequestItem requestItem = requestSampler.getHttpRequestItem();
        if (requestItem == null) {
            return null;
        }
        String apiId = requestItem.getId();
        String apiName = requestItem.getName();
        boolean webSocketRequest = PerformanceRequestProtocolResolver.isWebSocketRequest(requestItem);

        ApiMetadata.register(apiId, apiName);

        PreparedRequest req = PreparedRequestBuilder.build(requestItem);
        String requestBodyTemplate = req.body;
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.forRequestExecution(
                requestItem,
                req,
                iterationContext,
                true
        );

        String errorMsg = "";
        List<TestResult> testResults = new ArrayList<>();
        boolean executionFailed = false;
        ScriptExecutionResult preResult = pipeline.executePreScript();
        boolean preOk = preResult.isSuccess();
        if (!preOk) {
            log.error("前置脚本: {}", preResult.getErrorMessage());
            errorMsg = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_PRE_SCRIPT_FAILED, preResult.getErrorMessage());
            executionFailed = true;
        }
        if (!runningSupplier.getAsBoolean()) {
            return null;
        }
        if (preOk) {
            pipeline.finalizeRequest();
        }

        long requestStartTime = System.currentTimeMillis();
        long costMs = 0L;
        boolean interrupted = false;
        HttpResponse resp = null;
        boolean sseRequest = PerformanceRequestProtocolResolver.isSseRequest(requestItem);
        PerformanceProtocol protocol = PerformanceRequestProtocolResolver.resolvePerformanceProtocol(webSocketRequest, sseRequest);

        if (preOk && runningSupplier.getAsBoolean()) {
            try {
                PerformanceRequestPreparationSupport.configurePreparedRequest(req);
                sseRequest = PerformanceRequestProtocolResolver.isSseRequest(requestItem, req);
                webSocketRequest = PerformanceRequestProtocolResolver.isWebSocketRequest(requestItem);
                protocol = PerformanceRequestProtocolResolver.resolvePerformanceProtocol(webSocketRequest, sseRequest);
                boolean transportSseRequest = sseRequest;
                boolean transportWebSocketRequest = webSocketRequest;
                if (!transportSseRequest && !transportWebSocketRequest) {
                    List<PerformanceAssertionElement> assertionNodes =
                            PerformanceAssertionRunner.collectAssertionElements(requestSampler, false, false);
                    req.responseBodyMode = resolveHttpResponseBodyModeForAssertionElements(
                            efficientModeSupplier.getAsBoolean(),
                            assertionNodes,
                            req.postscript
                    );
                    req.responseBodyPreviewLimitBytes = resolveResponseBodyPreviewLimitBytes(
                            responseBodyPreviewLimitKbSupplier.getAsInt()
                    );
                }
                ProtocolExecutionResult protocolResult = pipeline.withExecutionContextThrowing(() ->
                        transportExecutor.execute(req, requestSampler, requestItem, transportSseRequest, transportWebSocketRequest,
                                requestBodyTemplate, pipeline)
                );
                resp = protocolResult.response();
                errorMsg = CharSequenceUtil.blankToDefault(protocolResult.errorMsg(), errorMsg);
                executionFailed = protocolResult.executionFailed();
                interrupted = protocolResult.interrupted();
                if (!protocolResult.testResults().isEmpty()) {
                    testResults.addAll(protocolResult.testResults());
                }
            } catch (Exception ex) {
                if (cancelledChecker.test(ex)) {
                    log.debug("请求被取消/中断（压测已停止）: {}", ex.getMessage());
                    interrupted = true;
                } else {
                    log.error("请求执行失败: {}", ex.getMessage(), ex);
                    errorMsg = I18nUtil.getMessage(MessageKeys.PERFORMANCE_MSG_REQUEST_FAILED, ex.getMessage());
                    executionFailed = true;
                }
            } finally {
                costMs = System.currentTimeMillis() - requestStartTime;
            }

            PerformanceRequestPostProcessResult postProcessResult = postProcessor.process(
                    requestSampler,
                    resp,
                    sseRequest,
                    webSocketRequest,
                    pipeline,
                    errorMsg,
                    executionFailed,
                    testResults
            );
            errorMsg = postProcessResult.errorMsg();
            executionFailed = postProcessResult.executionFailed();
        } else {
            costMs = System.currentTimeMillis() - requestStartTime;
        }

        return new PerformanceRequestExecutionResult(
                apiId,
                apiName,
                req,
                resp,
                errorMsg,
                testResults,
                executionFailed,
                interrupted,
                protocol,
                requestStartTime,
                costMs
        );
    }

    static PreparedRequest.ResponseBodyMode resolveHttpResponseBodyModeForAssertionElements(
            boolean efficientMode,
            List<PerformanceAssertionElement> assertionNodes,
            String postscript) {
        return PerformanceRequestPreparationSupport.resolveHttpResponseBodyModeForAssertionElements(
                efficientMode,
                assertionNodes,
                postscript
        );
    }

    static int resolveResponseBodyPreviewLimitBytes(int previewLimitKb) {
        return PerformanceRequestPreparationSupport.resolveResponseBodyPreviewLimitBytes(previewLimitKb);
    }

}
