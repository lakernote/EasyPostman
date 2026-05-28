package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.assertion.AssertionData;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.performance.core.controller.LoopData;
import com.laker.postman.performance.core.extractor.ExtractorData;
import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.SsePerformanceData;
import com.laker.postman.performance.core.model.WebSocketPerformanceData;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import com.laker.postman.performance.core.threadgroup.ThreadGroupData;
import com.laker.postman.performance.core.timer.TimerData;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Value
public class PerformancePlanNode {
    String name;
    NodeType type;
    boolean enabled;
    ThreadGroupData threadGroupData;
    CsvDataSetData csvDataSetData;
    LoopData loopData;
    PerformanceRequestSnapshot requestSnapshot;
    AssertionData assertionData;
    ExtractorData extractorData;
    TimerData timerData;
    SsePerformanceData ssePerformanceData;
    WebSocketPerformanceData webSocketPerformanceData;
    RequestExecutionScope requestExecutionScope;
    boolean requestInheritanceSnapshot;
    List<PerformancePlanNode> children;

    @Builder
    public PerformancePlanNode(String name,
                               NodeType type,
                               Boolean enabled,
                               ThreadGroupData threadGroupData,
                               CsvDataSetData csvDataSetData,
                               LoopData loopData,
                               HttpRequestItem httpRequestItem,
                               PerformanceRequestSnapshot requestSnapshot,
                               AssertionData assertionData,
                               ExtractorData extractorData,
                               TimerData timerData,
                               SsePerformanceData ssePerformanceData,
                               WebSocketPerformanceData webSocketPerformanceData,
                               RequestExecutionScope requestExecutionScope,
                               Boolean requestInheritanceSnapshot,
                               List<PerformancePlanNode> children) {
        this.name = name;
        this.type = type;
        this.enabled = enabled == null || enabled;
        this.threadGroupData = PerformancePlanDataCopies.copyThreadGroupData(threadGroupData);
        this.csvDataSetData = PerformancePlanDataCopies.copyCsvDataSetData(csvDataSetData);
        this.loopData = PerformancePlanDataCopies.copyLoopData(loopData);
        PerformanceRequestSnapshot canonicalSnapshot = canonicalRequestSnapshot(type, requestSnapshot, httpRequestItem, requestExecutionScope);
        this.requestSnapshot = PerformanceRequestSnapshotMapper.copyRequestSnapshot(canonicalSnapshot);
        this.assertionData = PerformancePlanDataCopies.copyAssertionData(assertionData);
        this.extractorData = PerformancePlanDataCopies.copyExtractorData(extractorData);
        this.timerData = PerformancePlanDataCopies.copyTimerData(timerData);
        this.ssePerformanceData = PerformancePlanDataCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanDataCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.requestExecutionScope = PerformancePlanDataCopies.copyRequestExecutionScope(canonicalRequestExecutionScope(type, requestExecutionScope, canonicalSnapshot));
        this.requestInheritanceSnapshot = requestInheritanceSnapshot != null && requestInheritanceSnapshot;
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    public HttpRequestItem getHttpRequestItem() {
        return PerformancePlanDataCopies.copyHttpRequestItem(
                PerformanceRequestSnapshotMapper.toHttpRequestItem(requestSnapshot)
        );
    }

    public PerformanceRequestSnapshot getRequestSnapshot() {
        return PerformanceRequestSnapshotMapper.copyRequestSnapshot(requestSnapshot);
    }

    public RequestExecutionScope getRequestExecutionScope() {
        return PerformancePlanDataCopies.copyRequestExecutionScope(requestExecutionScope);
    }

    private static PerformanceRequestSnapshot canonicalRequestSnapshot(NodeType type,
                                                                       PerformanceRequestSnapshot requestSnapshot,
                                                                       HttpRequestItem httpRequestItem,
                                                                       RequestExecutionScope requestExecutionScope) {
        if (type != NodeType.REQUEST) {
            return null;
        }
        if (requestSnapshot != null) {
            return requestSnapshot;
        }
        return PerformanceRequestSnapshotMapper.fromHttpRequestItem(httpRequestItem, requestExecutionScope);
    }

    private static RequestExecutionScope canonicalRequestExecutionScope(NodeType type,
                                                                        RequestExecutionScope requestExecutionScope,
                                                                        PerformanceRequestSnapshot requestSnapshot) {
        if (type != NodeType.REQUEST) {
            return requestExecutionScope;
        }
        if (requestExecutionScope != null) {
            return requestExecutionScope;
        }
        return PerformanceRequestSnapshotMapper.toRequestExecutionScope(requestSnapshot);
    }
}
