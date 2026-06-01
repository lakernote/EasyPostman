package com.laker.postman.performance.execution;

import com.laker.postman.performance.core.model.PerformanceRealtimeMetrics;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.http.runtime.model.PreparedRequest;
import com.laker.postman.http.runtime.transport.RealtimeConnectionHandle;
import com.laker.postman.http.runtime.transport.RealtimeWebSocketConnection;
import com.laker.postman.performance.plan.PerformanceRequestSampler;

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
                                        Set<RealtimeConnectionHandle> activeSseSources,
                                        Set<RealtimeWebSocketConnection> activeWebSockets,
                                        PerformanceRealtimeMetrics realtimeMetrics,
                                        IntSupplier responseBodyPreviewLimitKbSupplier) {
        this(
                runningSupplier,
                cancelledChecker,
                new DefaultPerformanceNetworkRuntime(activeSseSources, activeWebSockets),
                realtimeMetrics,
                responseBodyPreviewLimitKbSupplier
        );
    }

    PerformanceRequestTransportExecutor(BooleanSupplier runningSupplier,
                                        Predicate<Throwable> cancelledChecker,
                                        PerformanceNetworkRuntime networkRuntime,
                                        PerformanceRealtimeMetrics realtimeMetrics,
                                        IntSupplier responseBodyPreviewLimitKbSupplier) {
        PerformanceNetworkRuntime resolvedRuntime = resolveNetworkRuntime(networkRuntime);
        this.httpSamplerExecutor = new HttpSamplerExecutor(resolvedRuntime);
        this.sseSamplerExecutor = new SseSamplerExecutor(
                runningSupplier,
                cancelledChecker,
                resolvedRuntime.activeSseSources(),
                realtimeMetrics,
                responseBodyPreviewLimitKbSupplier,
                resolvedRuntime
        );
        this.webSocketSamplerExecutor = new WebSocketSamplerExecutor(
                runningSupplier,
                cancelledChecker,
                resolvedRuntime.activeWebSockets(),
                realtimeMetrics,
                responseBodyPreviewLimitKbSupplier,
                resolvedRuntime
        );
    }

    PerformanceRequestTransportExecutor(BooleanSupplier runningSupplier,
                                        Predicate<Throwable> cancelledChecker,
                                        Set<RealtimeConnectionHandle> activeSseSources,
                                        Set<RealtimeWebSocketConnection> activeWebSockets,
                                        PerformanceRealtimeMetrics realtimeMetrics,
                                        IntSupplier responseBodyPreviewLimitKbSupplier,
                                        PerformanceProtocolSamplerExecutor httpSamplerExecutor,
                                        PerformanceProtocolSamplerExecutor sseSamplerExecutor,
                                        PerformanceProtocolSamplerExecutor webSocketSamplerExecutor) {
        this.httpSamplerExecutor = httpSamplerExecutor;
        this.sseSamplerExecutor = sseSamplerExecutor;
        this.webSocketSamplerExecutor = webSocketSamplerExecutor;
    }

    private static PerformanceNetworkRuntime resolveNetworkRuntime(PerformanceNetworkRuntime networkRuntime) {
        return networkRuntime == null ? new DefaultPerformanceNetworkRuntime() : networkRuntime;
    }

    ProtocolExecutionResult execute(PreparedRequest request,
                                    PerformanceRequestSampler requestSampler,
                                    PerformanceRequestSnapshot requestSnapshot,
                                    boolean sseRequest,
                                    boolean webSocketRequest,
                                    String requestBodyTemplate,
                                    PerformanceScriptRuntime scriptRuntime) throws Exception {
        return execute(
                request,
                requestSampler,
                requestSnapshot,
                sseRequest,
                webSocketRequest,
                requestBodyTemplate,
                scriptRuntime,
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
                                    PerformanceRequestSnapshot requestSnapshot,
                                    boolean sseRequest,
                                    boolean webSocketRequest,
                                    String requestBodyTemplate,
                                    PerformanceScriptRuntime scriptRuntime,
                                    PerformanceResponseCapturePlan capturePlan) throws Exception {
        PerformanceProtocolSamplerContext context = new PerformanceProtocolSamplerContext(
                request,
                requestSampler,
                requestSnapshot,
                requestBodyTemplate,
                scriptRuntime,
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
