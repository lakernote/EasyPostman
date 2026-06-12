package com.laker.postman.performance.core.plan;

import com.laker.postman.performance.core.request.PerformanceRequestFormDataPart;
import com.laker.postman.performance.core.request.PerformanceRequestKeyValue;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PerformanceCorePlanDocumentSanitizer {

    public PerformanceCorePlanDocument enabledOnly(PerformanceCorePlanDocument document) {
        PerformanceCorePlanNode root = sanitizeNode(document == null ? null : document.getRoot(), true);
        return new PerformanceCorePlanDocument(root);
    }

    private PerformanceCorePlanNode sanitizeNode(PerformanceCorePlanNode node, boolean rootNode) {
        if (node == null || (!rootNode && !node.isEnabled())) {
            return null;
        }
        return PerformanceCorePlanNode.builder()
                .name(node.getName())
                .type(node.getType())
                .enabled(true)
                .threadGroupData(node.getThreadGroupData())
                .csvDataSetData(node.getCsvDataSetData())
                .loopData(node.getLoopData())
                .conditionData(node.getConditionData())
                .whileData(node.getWhileData())
                .requestSnapshot(sanitizeRequest(node.getRequestSnapshot()))
                .assertionData(node.getAssertionData())
                .extractorData(node.getExtractorData())
                .timerData(node.getTimerData())
                .ssePerformanceData(node.getSsePerformanceData())
                .webSocketPerformanceData(node.getWebSocketPerformanceData())
                .requestInheritanceSnapshot(node.isRequestInheritanceSnapshot())
                .children(sanitizeChildren(node.getChildren()))
                .build();
    }

    private List<PerformanceCorePlanNode> sanitizeChildren(List<PerformanceCorePlanNode> children) {
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        return children.stream()
                .map(child -> sanitizeNode(child, false))
                .filter(child -> child != null)
                .toList();
    }

    private PerformanceRequestSnapshot sanitizeRequest(PerformanceRequestSnapshot request) {
        if (request == null) {
            return null;
        }
        return request.toBuilder()
                .headers(enabledKeyValues(request.getHeaders()))
                .params(enabledKeyValues(request.getParams()))
                .formData(enabledFormData(request.getFormData()))
                .urlencoded(enabledKeyValues(request.getUrlencoded()))
                .build();
    }

    private List<PerformanceRequestKeyValue> enabledKeyValues(List<PerformanceRequestKeyValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value.isEnabled())
                .toList();
    }

    private List<PerformanceRequestFormDataPart> enabledFormData(List<PerformanceRequestFormDataPart> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && value.isEnabled())
                .toList();
    }
}
