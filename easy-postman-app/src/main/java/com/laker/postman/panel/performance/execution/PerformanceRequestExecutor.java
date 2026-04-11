package com.laker.postman.panel.performance.execution;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.panel.performance.model.ApiMetadata;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.http.HttpUtil;
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

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@Slf4j
public class PerformanceRequestExecutor {

    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<EventSource> activeSseSources;
    private final Set<WebSocket> activeWebSockets;

    public PerformanceRequestExecutor(BooleanSupplier runningSupplier,
                                      Predicate<Throwable> cancelledChecker,
                                      Set<EventSource> activeSseSources,
                                      Set<WebSocket> activeWebSockets) {
        this.runningSupplier = runningSupplier;
        this.cancelledChecker = cancelledChecker;
        this.activeSseSources = activeSseSources;
        this.activeWebSockets = activeWebSockets;
    }

    public PerformanceRequestExecutionResult execute(DefaultMutableTreeNode requestNode,
                                                     JMeterTreeNode requestData,
                                                     ExecutionVariableContext iterationContext) {
        String apiId = requestData.httpRequestItem.getId();
        String apiName = requestData.httpRequestItem.getName();
        boolean webSocketRequest = isWebSocketRequest(requestData.httpRequestItem);

        ApiMetadata.register(apiId, apiName);

        PreparedRequest req = PreparedRequestBuilder.build(requestData.httpRequestItem);
        ScriptExecutionPipeline pipeline = ScriptExecutionPipeline.forRequestExecution(
                requestData.httpRequestItem,
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
        boolean sseRequest = isSseRequest(requestData.httpRequestItem);

        if (preOk && runningSupplier.getAsBoolean()) {
            try {
                configurePreparedRequest(req);
                sseRequest = isSseRequest(requestData.httpRequestItem, req);
                webSocketRequest = isWebSocketRequest(requestData.httpRequestItem);
                ProtocolExecutionResult protocolResult = executeTransport(req, requestNode, requestData, sseRequest, webSocketRequest);
                resp = protocolResult.response;
                errorMsg = CharSequenceUtil.blankToDefault(protocolResult.errorMsg, errorMsg);
                executionFailed = protocolResult.executionFailed;
                interrupted = protocolResult.interrupted;
                if (!protocolResult.testResults.isEmpty()) {
                    testResults.addAll(protocolResult.testResults);
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

            List<DefaultMutableTreeNode> assertionNodes = PerformanceAssertionRunner.collectAssertionNodes(requestNode, sseRequest, webSocketRequest);
            if (resp != null && runningSupplier.getAsBoolean() && !assertionNodes.isEmpty()) {
                AtomicReference<String> assertionErrorRef = new AtomicReference<>(errorMsg);
                PerformanceAssertionRunner.runAssertionNodes(assertionNodes, resp, testResults, assertionErrorRef);
                errorMsg = assertionErrorRef.get();
            }
            if (resp != null && runningSupplier.getAsBoolean()) {
                ScriptExecutionResult postResult = pipeline.executePostScript(resp);
                if (postResult.hasTestResults()) {
                    testResults.addAll(postResult.getTestResults());
                    if (!postResult.allTestsPassed()) {
                        errorMsg = postResult.getTestResults().stream()
                                .filter(test -> !test.passed)
                                .map(test -> test.name)
                                .findFirst()
                                .orElse("pm.test assertion failed");
                    }
                }
                if (!postResult.isSuccess()) {
                    log.error("后置脚本执行失败: {}", postResult.getErrorMessage());
                    errorMsg = postResult.getErrorMessage();
                    executionFailed = true;
                }
            }
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
                webSocketRequest,
                requestStartTime,
                costMs
        );
    }

    private void configurePreparedRequest(PreparedRequest req) {
        req.collectBasicInfo = true;
        req.collectEventInfo = SettingManager.isPerformanceEventLoggingEnabled();
        req.enableNetworkLog = false;
    }

    private ProtocolExecutionResult executeTransport(PreparedRequest req,
                                                     DefaultMutableTreeNode requestNode,
                                                     JMeterTreeNode requestData,
                                                     boolean sseRequest,
                                                     boolean webSocketRequest) throws Exception {
        if (webSocketRequest) {
            WebSocketScenarioExecutor.Result result = new WebSocketScenarioExecutor(
                    runningSupplier,
                    cancelledChecker,
                    activeWebSockets
            ).execute(req, requestNode, requestData.webSocketPerformanceData);
            return new ProtocolExecutionResult(result.response, result.errorMsg, result.executionFailed, result.interrupted, result.testResults);
        }
        if (sseRequest) {
            SseSampleExecutor.Result result = new SseSampleExecutor(
                    runningSupplier,
                    cancelledChecker,
                    activeSseSources
            ).execute(req, requestData.ssePerformanceData);
            return new ProtocolExecutionResult(result.response, result.errorMsg, result.executionFailed, result.interrupted, new ArrayList<>());
        }
        return new ProtocolExecutionResult(HttpSingleRequestExecutor.executeHttp(req), "", false, false, new ArrayList<>());
    }

    private boolean isSseRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item));
    }

    private boolean isSseRequest(HttpRequestItem item, PreparedRequest req) {
        RequestItemProtocolEnum protocol = resolveProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(req));
    }

    private boolean isWebSocketRequest(HttpRequestItem item) {
        return resolveProtocol(item).isWebSocketProtocol();
    }

    private RequestItemProtocolEnum resolveProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    private static final class ProtocolExecutionResult {
        private final HttpResponse response;
        private final String errorMsg;
        private final boolean executionFailed;
        private final boolean interrupted;
        private final List<TestResult> testResults;

        private ProtocolExecutionResult(HttpResponse response,
                                        String errorMsg,
                                        boolean executionFailed,
                                        boolean interrupted,
                                        List<TestResult> testResults) {
            this.response = response;
            this.errorMsg = errorMsg;
            this.executionFailed = executionFailed;
            this.interrupted = interrupted;
            this.testResults = testResults;
        }
    }
}
