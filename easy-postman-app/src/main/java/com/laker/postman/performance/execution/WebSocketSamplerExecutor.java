package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;


import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.http.runtime.transport.HttpBaseClientProvider;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@RequiredArgsConstructor
final class WebSocketSamplerExecutor implements PerformanceProtocolSamplerExecutor {
    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<RealtimeWebSocketConnection> activeWebSockets;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;
    private final HttpBaseClientProvider baseClientProvider;

    WebSocketSamplerExecutor(BooleanSupplier runningSupplier,
                             Predicate<Throwable> cancelledChecker,
                             Set<RealtimeWebSocketConnection> activeWebSockets,
                             PerformanceRealtimeMetrics realtimeMetrics,
                             IntSupplier responseBodyPreviewLimitKbSupplier) {
        this(runningSupplier, cancelledChecker, activeWebSockets, realtimeMetrics, responseBodyPreviewLimitKbSupplier,
                null);
    }

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
                responseBodyPreviewLimitBytes(),
                baseClientProvider
        ).execute(
                context.getRequest(),
                context.getRequestSampler(),
                webSocketPerformanceData,
                context.getRequestBodyTemplate(),
                context.getScriptRuntime(),
                context.getCapturePlan(),
                context.getRequestId(),
                context.getRequestName()
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
