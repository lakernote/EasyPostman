package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceCoreRequestSampler implements PerformanceSampler {
    private final String name;
    private final PerformanceRequestSnapshot requestSnapshot;
    private final WebSocketPerformanceData webSocketPerformanceData;
    private final List<PerformancePlanElement> children;

    public PerformanceCoreRequestSampler(String name,
                                         PerformanceRequestSnapshot requestSnapshot,
                                         WebSocketPerformanceData webSocketPerformanceData,
                                         List<PerformancePlanElement> children) {
        this.name = name;
        this.requestSnapshot = copyRequestSnapshot(requestSnapshot);
        this.webSocketPerformanceData = PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
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

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return copyRequestSnapshot(requestSnapshot);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanCoreDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    @Override
    public List<PerformancePlanElement> getChildren() {
        return children;
    }

    @Override
    public boolean executesChildrenInSamplerOrder() {
        return requestSnapshot != null && requestSnapshot.executesChildrenInSamplerOrder();
    }

    private static PerformanceRequestSnapshot copyRequestSnapshot(PerformanceRequestSnapshot source) {
        return source == null ? null : source.toBuilder().build();
    }
}
