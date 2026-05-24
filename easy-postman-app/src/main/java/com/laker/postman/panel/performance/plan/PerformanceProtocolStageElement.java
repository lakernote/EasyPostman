package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceProtocolStageElement implements PerformancePlanElement {
    private final String name;
    private final NodeType type;
    private final SsePerformanceData ssePerformanceData;
    private final WebSocketPerformanceData webSocketPerformanceData;
    private final List<PerformancePlanElement> elements;

    public PerformanceProtocolStageElement(String name,
                                           NodeType type,
                                           SsePerformanceData ssePerformanceData,
                                           WebSocketPerformanceData webSocketPerformanceData,
                                           List<PerformancePlanElement> elements) {
        this.name = name;
        this.type = type;
        this.ssePerformanceData = PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return type;
    }

    public SsePerformanceData getSsePerformanceData() {
        return PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
