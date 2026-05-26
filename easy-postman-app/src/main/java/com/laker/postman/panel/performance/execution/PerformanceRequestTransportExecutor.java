package com.laker.postman.panel.performance.execution;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.performance.model.PerformanceRealtimeMetrics;
import com.laker.postman.panel.performance.plan.PerformanceRequestSampler;
import com.laker.postman.service.js.ScriptExecutionPipeline;
import okhttp3.WebSocket;
import okhttp3.sse.EventSource;

import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

final class PerformanceRequestTransportExecutor {

    private final PerformanceProtocolSamplerExecutor httpSamplerExecutor;
    private final PerformanceProtocolSamplerExecutor sseSamplerExecutor;
    private final PerformanceProtocolSamplerExecutor webSocketSamplerExecutor;

    PerformanceRequestTransportExecutor(BooleanSupplier runningSupplier,
                                        Predicate<Throwable> cancelledChecker,
                                        Set<EventSource> activeSseSources,
                                        Set<WebSocket> activeWebSockets,
                                        PerformanceRealtimeMetrics realtimeMetrics,
                                        IntSupplier responseBodyPreviewLimitKbSupplier) {
        this(
                runningSupplier,
                cancelledChecker,
                activeSseSources,
                activeWebSockets,
                realtimeMetrics,
                responseBodyPreviewLimitKbSupplier,
                new HttpSamplerExecutor(),
                new SseSamplerExecutor(
                        runningSupplier,
                        cancelledChecker,
                        activeSseSources,
                        realtimeMetrics,
                        responseBodyPreviewLimitKbSupplier
                ),
                new WebSocketSamplerExecutor(
                        runningSupplier,
                        cancelledChecker,
                        activeWebSockets,
                        realtimeMetrics,
                        responseBodyPreviewLimitKbSupplier
                )
        );
    }

    PerformanceRequestTransportExecutor(BooleanSupplier runningSupplier,
                                        Predicate<Throwable> cancelledChecker,
                                        Set<EventSource> activeSseSources,
                                        Set<WebSocket> activeWebSockets,
                                        PerformanceRealtimeMetrics realtimeMetrics,
                                        IntSupplier responseBodyPreviewLimitKbSupplier,
                                        PerformanceProtocolSamplerExecutor httpSamplerExecutor,
                                        PerformanceProtocolSamplerExecutor sseSamplerExecutor,
                                        PerformanceProtocolSamplerExecutor webSocketSamplerExecutor) {
        this.httpSamplerExecutor = httpSamplerExecutor;
        this.sseSamplerExecutor = sseSamplerExecutor;
        this.webSocketSamplerExecutor = webSocketSamplerExecutor;
    }

    ProtocolExecutionResult execute(PreparedRequest request,
                                    PerformanceRequestSampler requestSampler,
                                    HttpRequestItem requestItem,
                                    boolean sseRequest,
                                    boolean webSocketRequest,
                                    String requestBodyTemplate,
                                    ScriptExecutionPipeline pipeline) throws Exception {
        return execute(
                request,
                requestSampler,
                requestItem,
                sseRequest,
                webSocketRequest,
                requestBodyTemplate,
                pipeline,
                PerformanceResponseCapturePlan.resolve(
                        true,
                        requestSampler,
                        sseRequest,
                        webSocketRequest,
                        request == null ? "" : request.postscript
                )
        );
    }

    ProtocolExecutionResult execute(PreparedRequest request,
                                    PerformanceRequestSampler requestSampler,
                                    HttpRequestItem requestItem,
                                    boolean sseRequest,
                                    boolean webSocketRequest,
                                    String requestBodyTemplate,
                                    ScriptExecutionPipeline pipeline,
                                    PerformanceResponseCapturePlan capturePlan) throws Exception {
        PerformanceProtocolSamplerContext context = new PerformanceProtocolSamplerContext(
                request,
                requestSampler,
                requestItem,
                requestBodyTemplate,
                pipeline,
                capturePlan
        );
        if (webSocketRequest) {
            return webSocketSamplerExecutor.execute(context);
        }
        if (sseRequest) {
            return sseSamplerExecutor.execute(context);
        }
        return httpSamplerExecutor.execute(context);
    }
}
