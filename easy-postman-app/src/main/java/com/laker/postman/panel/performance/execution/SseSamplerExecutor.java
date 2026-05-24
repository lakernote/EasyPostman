package com.laker.postman.panel.performance.execution;

import com.laker.postman.panel.performance.model.PerformanceProtocol;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
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

    @Override
    public ProtocolExecutionResult execute(PerformanceProtocolSamplerContext context) {
        ProtocolExecutionResult invalidStageResult = validateProtocolStages(
                context.getRequestSampler(),
                PerformanceProtocol.SSE
        );
        if (invalidStageResult != null) {
            return invalidStageResult;
        }
        SsePerformanceData ssePerformanceData = context.getRequestSampler().getSsePerformanceData();
        if (ssePerformanceData == null) {
            ssePerformanceData = new SsePerformanceData();
        }
        SseSampleExecutor.Result result = new SseSampleExecutor(
                runningSupplier,
                cancelledChecker,
                activeSseSources,
                realtimeMetrics,
                responseBodyPreviewLimitBytes()
        ).execute(
                context.getRequest(),
                ssePerformanceData,
                context.getRequestItem().getId(),
                context.getRequestItem().getName()
        );
        return new ProtocolExecutionResult(
                result.response,
                result.errorMsg,
                result.executionFailed,
                result.interrupted,
                List.of()
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
