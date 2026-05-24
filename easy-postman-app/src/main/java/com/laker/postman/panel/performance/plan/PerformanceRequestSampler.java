package com.laker.postman.panel.performance.plan;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceRequestSampler implements PerformanceSampler {
    private final String name;
    private final HttpRequestItem httpRequestItem;
    private final SsePerformanceData ssePerformanceData;
    private final WebSocketPerformanceData webSocketPerformanceData;
    private final List<PerformancePlanElement> children;

    public PerformanceRequestSampler(String name,
                                     HttpRequestItem httpRequestItem,
                                     SsePerformanceData ssePerformanceData,
                                     WebSocketPerformanceData webSocketPerformanceData,
                                     List<PerformancePlanElement> children) {
        this.name = name;
        this.httpRequestItem = PerformancePlanNodeCopies.copyHttpRequestItem(httpRequestItem);
        this.ssePerformanceData = PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
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
        return PerformancePlanNodeCopies.copyHttpRequestItem(httpRequestItem);
    }

    public SsePerformanceData getSsePerformanceData() {
        return PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    @Override
    public List<PerformancePlanElement> getChildren() {
        return children;
    }

    @Override
    public boolean executesChildrenInSamplerOrder() {
        return httpRequestItem != null
                && httpRequestItem.getProtocol() != null
                && httpRequestItem.getProtocol().isWebSocketProtocol();
    }
}
