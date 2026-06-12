package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceProtocolStageElement implements PerformanceElementContainer {
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
        this.ssePerformanceData = PerformancePlanCoreDataCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
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
        return PerformancePlanCoreDataCopies.copySsePerformanceData(ssePerformanceData);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    @Override
    public List<PerformancePlanElement> getElements() {
        return elements;
    }
}
