package com.laker.postman.panel.performance.tree;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.plan.PerformanceRequestSnapshotMapper;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.CollectionRequestExecutionScopeResolver;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class PerformanceRequestScopeResolver {

    public Optional<RequestExecutionScope> resolveLatestCollectionScope(PerformanceTreeNode nodeData) {
        String requestId = requestId(nodeData);
        if (requestId == null || requestId.trim().isEmpty()) {
            return Optional.empty();
        }
        return CollectionRequestExecutionScopeResolver.resolveCurrentScope(requestId);
    }

    public RequestExecutionScope resolveEffectiveScope(PerformanceTreeNode nodeData) {
        if (nodeData == null || nodeData.type != NodeType.REQUEST) {
            return RequestExecutionScope.empty();
        }
        return resolveLatestCollectionScope(nodeData)
                .orElseGet(() -> storedScope(nodeData));
    }

    private RequestExecutionScope storedScope(PerformanceTreeNode nodeData) {
        if (nodeData.requestExecutionScope != null) {
            return nodeData.requestExecutionScope;
        }
        return PerformanceRequestSnapshotMapper.toRequestExecutionScope(nodeData.requestSnapshot);
    }

    private String requestId(PerformanceTreeNode nodeData) {
        if (nodeData == null || nodeData.type != NodeType.REQUEST) {
            return null;
        }
        HttpRequestItem item = nodeData.httpRequestItem;
        if (item != null && item.getId() != null && !item.getId().trim().isEmpty()) {
            return item.getId();
        }
        if (nodeData.requestSnapshot != null) {
            return nodeData.requestSnapshot.getId();
        }
        return null;
    }
}
