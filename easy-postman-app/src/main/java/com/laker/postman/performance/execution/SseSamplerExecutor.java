package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;
import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.SsePerformanceDataSupport;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceProtocolStageElement;


import com.laker.postman.performance.plan.PerformanceRequestSampler;
import com.laker.postman.http.runtime.transport.HttpBaseClientProvider;
import lombok.RequiredArgsConstructor;
import okhttp3.sse.EventSource;

import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@RequiredArgsConstructor
final class SseSamplerExecutor implements PerformanceProtocolSamplerExecutor {
    private final BooleanSupplier runningSupplier;
    private final Predicate<Throwable> cancelledChecker;
    private final Set<EventSource> activeSseSources;
    private final PerformanceRealtimeMetrics realtimeMetrics;
    private final IntSupplier responseBodyPreviewLimitKbSupplier;
    private final HttpBaseClientProvider baseClientProvider;

    SseSamplerExecutor(BooleanSupplier runningSupplier,
                       Predicate<Throwable> cancelledChecker,
                       Set<EventSource> activeSseSources,
                       PerformanceRealtimeMetrics realtimeMetrics,
                       IntSupplier responseBodyPreviewLimitKbSupplier) {
        this(runningSupplier, cancelledChecker, activeSseSources, realtimeMetrics, responseBodyPreviewLimitKbSupplier,
                null);
    }

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) {
        ProtocolExecutionResult invalidStageResult = validateProtocolStages(
                context.getRequestSampler(),
                PerformanceProtocol.SSE
        );
        if (invalidStageResult != null) {
            return invalidStageResult;
        }
        SsePerformanceData ssePerformanceData = resolveSsePerformanceData(context.getRequestSampler());
        SseSampleExecutor.Result result = new SseSampleExecutor(
                runningSupplier,
                cancelledChecker,
                activeSseSources,
                realtimeMetrics,
                responseBodyPreviewLimitBytes(),
                retainResponseBody(context),
                trackResponseBodySize(context),
                baseClientProvider
        ).execute(
                context.getRequest(),
                ssePerformanceData,
                context.getRequestId(),
                context.getRequestName()
        );
        return new ProtocolExecutionResult(
                result.response,
                result.errorMsg,
                result.executionFailed,
                result.interrupted,
                List.of()
        );
    }

    private SsePerformanceData resolveSsePerformanceData(PerformanceRequestSampler requestSampler) {
        SsePerformanceData effective = new SsePerformanceData();
        SsePerformanceData connectData = firstStageData(requestSampler, NodeType.SSE_CONNECT);
        SsePerformanceDataSupport.applyConnectConfig(effective, connectData);
        SsePerformanceData readData = firstStageData(requestSampler, NodeType.SSE_READ);
        SsePerformanceDataSupport.applyReadConfig(effective, readData);
        return effective;
    }

    private SsePerformanceData firstStageData(PerformanceRequestSampler requestSampler, NodeType type) {
        if (requestSampler == null || type == null) {
            return null;
        }
        for (PerformancePlanElement element : requestSampler.getChildren()) {
            if (element instanceof PerformanceProtocolStageElement stage && stage.getType() == type) {
                return stage.getSsePerformanceData();
            }
        }
        return null;
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

    private boolean retainResponseBody(PerformanceProtocolSamplerContext context) {
        PerformanceResponseCapturePlan capturePlan = context.getCapturePlan();
        if (capturePlan != null) {
            return capturePlan.retainStreamResponseBody();
        }
        return false;
    }

    private boolean trackResponseBodySize(PerformanceProtocolSamplerContext context) {
        PerformanceResponseCapturePlan capturePlan = context.getCapturePlan();
        if (capturePlan != null) {
            return capturePlan.trackStreamResponseBodySize();
        }
        return false;
    }

    private int responseBodyPreviewLimitBytes() {
        return PerformanceRequestPreparationSupport.resolveResponseBodyPreviewLimitBytes(
                responseBodyPreviewLimitKbSupplier.getAsInt()
        );
    }
}
