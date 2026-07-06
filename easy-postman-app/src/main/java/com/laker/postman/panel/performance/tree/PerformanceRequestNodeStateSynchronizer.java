package com.laker.postman.panel.performance.tree;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.plan.PerformancePlanDataCopies;
import com.laker.postman.performance.plan.PerformanceRequestSnapshotMapper;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.collections.CollectionRequestItemResolver;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class PerformanceRequestNodeStateSynchronizer {

    public RequestExecutionScope replaceRequestItem(PerformanceTreeNode nodeData, HttpRequestItem item) {
        if (!isRequestNode(nodeData)) {
            return RequestExecutionScope.empty();
        }
        nodeData.httpRequestItem = PerformancePlanDataCopies.copyHttpRequestItem(item);
        if (nodeData.httpRequestItem != null) {
            nodeData.name = nodeData.httpRequestItem.getName();
        }
        return syncSnapshotFromCurrentItem(nodeData);
    }

    public Optional<RequestExecutionScope> refreshLatestCollectionScope(PerformanceTreeNode nodeData) {
        Optional<RequestExecutionScope> latestScope = PerformanceRequestScopeResolver.resolveLatestCollectionScope(nodeData);
        latestScope.ifPresent(scope -> syncSnapshot(nodeData, scope));
        return latestScope;
    }

    public Optional<RequestExecutionScope> refreshFromCollection(PerformanceTreeNode nodeData) {
        String requestId = requestId(nodeData);
        if (requestId == null) {
            return Optional.empty();
        }
        return CollectionRequestItemResolver.resolveCurrentRequest(requestId)
                .map(item -> replaceRequestItem(nodeData, item));
    }

    public RequestExecutionScope syncSnapshotFromCurrentItem(PerformanceTreeNode nodeData) {
        if (!isRequestNode(nodeData) || nodeData.httpRequestItem == null) {
            return RequestExecutionScope.empty();
        }
        RequestExecutionScope scope = PerformanceRequestScopeResolver.resolveEffectiveScope(nodeData);
        syncSnapshot(nodeData, scope);
        return scope;
    }

    private void syncSnapshot(PerformanceTreeNode nodeData, RequestExecutionScope scope) {
        if (!isRequestNode(nodeData)) {
            return;
        }
        RequestExecutionScope effectiveScope = scope == null ? RequestExecutionScope.empty() : scope;
        nodeData.requestExecutionScope = effectiveScope;
        if (nodeData.httpRequestItem != null) {
            nodeData.requestSnapshot = PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                    nodeData.httpRequestItem,
                    effectiveScope
            );
            return;
        }
        if (nodeData.requestSnapshot != null) {
            nodeData.requestSnapshot = nodeData.requestSnapshot.toBuilder()
                    .executionScope(PerformanceRequestSnapshotMapper.toScopeSnapshot(effectiveScope))
                    .build();
        }
    }

    private boolean isRequestNode(PerformanceTreeNode nodeData) {
        return nodeData != null && nodeData.type == NodeType.REQUEST;
    }

    private String requestId(PerformanceTreeNode nodeData) {
        if (!isRequestNode(nodeData)) {
            return null;
        }
        HttpRequestItem item = nodeData.httpRequestItem;
        if (item != null && item.getId() != null && !item.getId().trim().isEmpty()) {
            return item.getId();
        }
        if (nodeData.requestSnapshot != null) {
            String snapshotId = nodeData.requestSnapshot.getId();
            if (snapshotId != null && !snapshotId.trim().isEmpty()) {
                return snapshotId;
            }
        }
        return null;
    }
}
