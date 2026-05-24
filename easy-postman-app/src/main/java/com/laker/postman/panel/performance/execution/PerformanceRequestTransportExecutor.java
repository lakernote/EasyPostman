package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.http.HttpSingleRequestExecutor;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import lombok.RequiredArgsConstructor;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@RequiredArgsConstructor
final class PerformanceRequestTransportExecutor {

    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<EventSource> activeSseSources;
    private final Set<WebSocket> activeWebSockets;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;

    ProtocolExecutionResult execute(PreparedRequest request,
                                    PerformanceRequestSampler requestSampler,
                                    HttpRequestItem requestItem,
                                    boolean sseRequest,
                                    boolean webSocketRequest,
                                    String requestBodyTemplate,
                                    ScriptExecutionPipeline pipeline) throws Exception {
        if (webSocketRequest) {
            return executeWebSocket(request, requestSampler, requestItem, requestBodyTemplate, pipeline);
        }
        if (sseRequest) {
            return executeSse(request, requestSampler, requestItem);
        }
        return new ProtocolExecutionResult(
                HttpSingleRequestExecutor.executeHttp(request),
                "",
                false,
                false,
                new ArrayList<>()
        );
    }

    private ProtocolExecutionResult executeWebSocket(PreparedRequest request,
                                                     PerformanceRequestSampler requestSampler,
                                                     HttpRequestItem requestItem,
                                                     String requestBodyTemplate,
                                                     ScriptExecutionPipeline pipeline) throws Exception {
        ProtocolExecutionResult invalidStageResult = validateProtocolStages(requestSampler, PerformanceProtocol.WEBSOCKET);
        if (invalidStageResult != null) {
            return invalidStageResult;
        }
        WebSocketPerformanceData webSocketPerformanceData = requestSampler.getWebSocketPerformanceData();
        if (webSocketPerformanceData == null) {
            webSocketPerformanceData = new WebSocketPerformanceData();
        }
        WebSocketScenarioExecutor.Result result = new WebSocketScenarioExecutor(
                runningSupplier,
                cancelledChecker,
                activeWebSockets,
                realtimeMetrics,
                responseBodyPreviewLimitBytes()
        ).execute(
                request,
                requestSampler,
                webSocketPerformanceData,
                requestBodyTemplate,
                pipeline,
                requestItem.getId(),
                requestItem.getName()
        );
        return new ProtocolExecutionResult(result.response, result.errorMsg, result.executionFailed, result.interrupted, result.testResults);
    }

    private ProtocolExecutionResult executeSse(PreparedRequest request,
                                               PerformanceRequestSampler requestSampler,
                                               HttpRequestItem requestItem) {
        ProtocolExecutionResult invalidStageResult = validateProtocolStages(requestSampler, PerformanceProtocol.SSE);
        if (invalidStageResult != null) {
            return invalidStageResult;
        }
        SsePerformanceData ssePerformanceData = requestSampler.getSsePerformanceData();
        if (ssePerformanceData == null) {
            ssePerformanceData = new SsePerformanceData();
        }
        SseSampleExecutor.Result result = new SseSampleExecutor(
                runningSupplier,
                cancelledChecker,
                activeSseSources,
                realtimeMetrics,
                responseBodyPreviewLimitBytes()
        ).execute(request, ssePerformanceData, requestItem.getId(), requestItem.getName());
        return new ProtocolExecutionResult(result.response, result.errorMsg, result.executionFailed, result.interrupted, new ArrayList<>());
    }

    private ProtocolExecutionResult validateProtocolStages(PerformanceRequestSampler requestSampler, PerformanceProtocol protocol) {
        PerformanceProtocolStageValidator.ValidationResult validation =
                PerformanceProtocolStageValidator.validate(requestSampler, protocol);
        if (!validation.valid()) {
            return failedProtocolResult(validation.message());
        }
        return null;
    }

    private ProtocolExecutionResult failedProtocolResult(String message) {
        return new ProtocolExecutionResult(null, message, true, false, new ArrayList<>());
    }

    private int responseBodyPreviewLimitBytes() {
        return PerformanceRequestPreparationSupport.resolveResponseBodyPreviewLimitBytes(
                responseBodyPreviewLimitKbSupplier.getAsInt()
        );
    }
}
