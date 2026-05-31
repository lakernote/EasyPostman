package com.laker.postman.performance.plan;

import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.plan.PerformancePlanElement;
import com.laker.postman.performance.core.plan.PerformanceSampler;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.service.variable.RequestExecutionScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceRequestSampler implements PerformanceSampler {
    private final String name;
    private final PerformanceRequestSnapshot requestSnapshot;
    private final WebSocketPerformanceData webSocketPerformanceData;
    private final RequestExecutionScope requestExecutionScope;
    private final List<PerformancePlanElement> children;

    public PerformanceRequestSampler(String name,
                                     HttpRequestItem httpRequestItem,
                                     WebSocketPerformanceData webSocketPerformanceData,
                                     List<PerformancePlanElement> children) {
        this(name, httpRequestItem, webSocketPerformanceData, children, null);
    }

    public PerformanceRequestSampler(String name,
                                     HttpRequestItem httpRequestItem,
                                     WebSocketPerformanceData webSocketPerformanceData,
                                     List<PerformancePlanElement> children,
                                     RequestExecutionScope requestExecutionScope) {
        this(
                name,
                httpRequestItem,
                PerformanceRequestSnapshotMapper.fromHttpRequestItem(httpRequestItem, requestExecutionScope),
                webSocketPerformanceData,
                children,
                requestExecutionScope
        );
    }

    public PerformanceRequestSampler(String name,
                                     HttpRequestItem httpRequestItem,
                                     PerformanceRequestSnapshot requestSnapshot,
                                     WebSocketPerformanceData webSocketPerformanceData,
                                     List<PerformancePlanElement> children,
                                     RequestExecutionScope requestExecutionScope) {
        this.name = name;
        PerformanceRequestSnapshot resolvedSnapshot = requestSnapshot != null
                ? requestSnapshot
                : PerformanceRequestSnapshotMapper.fromHttpRequestItem(httpRequestItem, requestExecutionScope);
        this.requestSnapshot = PerformanceRequestSnapshotMapper.copyRequestSnapshot(resolvedSnapshot);
        this.webSocketPerformanceData = PerformancePlanDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        RequestExecutionScope resolvedScope = requestExecutionScope != null
                ? requestExecutionScope
                : PerformanceRequestSnapshotMapper.toRequestExecutionScope(this.requestSnapshot);
        this.requestExecutionScope = PerformancePlanDataCopies.copyRequestExecutionScope(resolvedScope);
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.REQUEST;
    }

    public HttpRequestItem getHttpRequestItem() {
        return PerformancePlanDataCopies.copyHttpRequestItem(
                PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot)
        );
    }

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return PerformanceRequestSnapshotMapper.copyRequestSnapshot(requestSnapshot);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    public RequestExecutionScope getRequestExecutionScope() {
        return PerformancePlanDataCopies.copyRequestExecutionScope(requestExecutionScope);
    }

    @Override
    public List<PerformancePlanElement> getChildren() {
        return children;
    }

    @Override
    public boolean executesChildrenInSamplerOrder() {
        return requestSnapshot != null && requestSnapshot.executesChildrenInSamplerOrder();
    }
}
