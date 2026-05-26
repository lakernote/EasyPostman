package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import lombok.RequiredArgsConstructor;
import okhttp3.WebSocket;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@RequiredArgsConstructor
final class WebSocketSamplerExecutor implements PerformanceProtocolSamplerExecutor {
    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<WebSocket> activeWebSockets;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) throws Exception {
        ProtocolExecutionResult invalidStageResult = validateProtocolStages(
                context.getRequestSampler(),
                PerformanceProtocol.WEBSOCKET
        );
        if (invalidStageResult != null) {
            return invalidStageResult;
        }
        WebSocketPerformanceData webSocketPerformanceData = context.getRequestSampler().getWebSocketPerformanceData();
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
                context.getRequest(),
                context.getRequestSampler(),
                webSocketPerformanceData,
                context.getRequestBodyTemplate(),
                context.getPipeline(),
                context.getCapturePlan(),
                context.getRequestItem().getId(),
                context.getRequestItem().getName()
        );
        return new ProtocolExecutionResult(
                result.response,
                result.errorMsg,
                result.executionFailed,
                result.interrupted,
                result.testResults
        );
    }

    private ProtocolExecutionResult validateProtocolStages(PerformanceRequestSampler requestSampler,
                                                           PerformanceProtocol protocol) {
        PerformanceProtocolStageValidator.ValidationResult validation =
                PerformanceProtocolStageValidator.validate(requestSampler, protocol);
        return validation.valid() ? null : failedProtocolResult(validation.message());
    }

    private ProtocolExecutionResult failedProtocolResult(String message) {
        return new ProtocolExecutionResult(null, message, true, false, List.of());
    }

    private int responseBodyPreviewLimitBytes() {
        return PerformanceRequestPreparationSupport.resolveResponseBodyPreviewLimitBytes(
                responseBodyPreviewLimitKbSupplier.getAsInt()
        );
    }
}
