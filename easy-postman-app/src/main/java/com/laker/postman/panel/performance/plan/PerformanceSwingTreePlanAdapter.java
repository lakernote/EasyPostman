package com.laker.postman.panel.performance.plan;

import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.request.PerformanceRequestSnapshot;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.service.collections.ActiveCollectionTreeNodeRepository;
import com.laker.postman.service.collections.GroupInheritanceHelper;
import com.laker.postman.service.variable.RequestExecutionScope;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PerformanceSwingTreePlanAdapter {

    public PerformancePlanDocument toDocument(DefaultMutableTreeNode rootNode) {
        return new PerformancePlanDocument(toDocumentNode(rootNode));
    }

    public DefaultMutableTreeNode toTree(PerformancePlanDocument document, String rootName) {
        DefaultMutableTreeNode treeNode = toTreeNode(document == null ? null : document.getRoot());
        if (treeNode != null && rootName != null && treeNode.getUserObject() instanceof JMeterTreeNode data) {
            data.name = rootName;
        }
        return treeNode;
    }

    public PerformancePlanNode toDocumentNode(DefaultMutableTreeNode treeNode) {
        JMeterTreeNode data = nodeData(treeNode);
        if (data == null) {
            return null;
        }
        return PerformancePlanNode.builder()
                .name(data.name)
                .type(data.type)
                .enabled(data.enabled)
                .threadGroupData(data.threadGroupData)
                .csvDataSetData(data.csvDataSetData)
                .loopData(data.loopData)
                .httpRequestItem(resolveEffectiveRequestItem(data))
                .requestSnapshot(resolveRequestSnapshot(data))
                .assertionData(data.assertionData)
                .extractorData(data.extractorData)
                .timerData(data.timerData)
                .ssePerformanceData(data.ssePerformanceData)
                .webSocketPerformanceData(data.webSocketPerformanceData)
                .requestExecutionScope(resolveRequestExecutionScope(data))
                .requestInheritanceSnapshot(resolveRequestInheritanceSnapshot(data))
                .children(children(treeNode))
                .build();
    }

    public DefaultMutableTreeNode toTreeNode(PerformancePlanNode node) {
        if (node == null) {
            return null;
        }

        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(toJMeterTreeNode(node));
        for (PerformancePlanNode child : node.getChildren()) {
            DefaultMutableTreeNode childTreeNode = toTreeNode(child);
            if (childTreeNode != null) {
                treeNode.add(childTreeNode);
            }
        }
        return treeNode;
    }

    private List<PerformancePlanNode> children(DefaultMutableTreeNode treeNode) {
        List<PerformancePlanNode> children = new ArrayList<>();
        if (treeNode == null) {
            return children;
        }
        for (int i = 0; i < treeNode.getChildCount(); i++) {
            PerformancePlanNode child = toDocumentNode((DefaultMutableTreeNode) treeNode.getChildAt(i));
            if (child != null) {
                children.add(child);
            }
        }
        return children;
    }

    private JMeterTreeNode toJMeterTreeNode(PerformancePlanNode node) {
        JMeterTreeNode data = new JMeterTreeNode(node.getName(), node.getType());
        data.enabled = node.isEnabled();
        data.threadGroupData = PerformancePlanDataCopies.copyThreadGroupData(node.getThreadGroupData());
        data.csvDataSetData = PerformancePlanDataCopies.copyCsvDataSetData(node.getCsvDataSetData());
        data.loopData = PerformancePlanDataCopies.copyLoopData(node.getLoopData());
        data.httpRequestItem = PerformancePlanDataCopies.copyHttpRequestItem(node.getHttpRequestItem());
        data.requestSnapshot = PerformanceRequestSnapshotMapper.copyRequestSnapshot(node.getRequestSnapshot());
        data.assertionData = PerformancePlanDataCopies.copyAssertionData(node.getAssertionData());
        data.extractorData = PerformancePlanDataCopies.copyExtractorData(node.getExtractorData());
        data.timerData = PerformancePlanDataCopies.copyTimerData(node.getTimerData());
        data.ssePerformanceData = PerformancePlanDataCopies.copySsePerformanceData(node.getSsePerformanceData());
        data.webSocketPerformanceData = PerformancePlanDataCopies.copyWebSocketPerformanceData(node.getWebSocketPerformanceData());
        data.requestExecutionScope = PerformancePlanDataCopies.copyRequestExecutionScope(node.getRequestExecutionScope());
        data.requestInheritanceSnapshot = node.isRequestInheritanceSnapshot();
        return data;
    }

    private com.laker.postman.performance.core.request.PerformanceRequestSnapshot resolveRequestSnapshot(JMeterTreeNode data) {
        if (data == null || data.type != NodeType.REQUEST) {
            return null;
        }
        if (data.requestSnapshot != null) {
            return data.requestSnapshot;
        }
        return PerformanceRequestSnapshotMapper.fromHttpRequestItem(
                resolveEffectiveRequestItem(data),
                resolveRequestExecutionScope(data)
        );
    }

    private RequestExecutionScope resolveRequestExecutionScope(JMeterTreeNode data) {
        if (data == null || data.type != NodeType.REQUEST || data.httpRequestItem == null) {
            return null;
        }
        if (data.requestExecutionScope != null) {
            return data.requestExecutionScope;
        }
        String requestId = data.httpRequestItem.getId();
        if (requestId == null || requestId.trim().isEmpty()) {
            return null;
        }
        return new ActiveCollectionTreeNodeRepository()
                .findNodeByRequestId(requestId)
                .map(GroupInheritanceHelper::getMergedGroupVariables)
                .map(RequestExecutionScope::fromVariables)
                .orElse(null);
    }

    private HttpRequestItem resolveEffectiveRequestItem(JMeterTreeNode data) {
        if (data == null || data.type != NodeType.REQUEST || data.httpRequestItem == null) {
            return data == null ? null : data.httpRequestItem;
        }
        if (data.requestInheritanceSnapshot) {
            return data.httpRequestItem;
        }
        String requestId = data.httpRequestItem.getId();
        if (requestId == null || requestId.trim().isEmpty()) {
            return data.httpRequestItem;
        }
        return new ActiveCollectionTreeNodeRepository()
                .findNodeByRequestId(requestId)
                .map(GroupInheritanceHelper::collectGroupChain)
                .map(groupChain -> mergeGroupSettings(data.httpRequestItem, groupChain))
                .orElse(data.httpRequestItem);
    }

    private HttpRequestItem mergeGroupSettings(HttpRequestItem item, List<RequestGroup> groupChain) {
        if (groupChain == null || groupChain.isEmpty()) {
            return item;
        }
        return GroupInheritanceHelper.mergeGroupSettingsWithChain(item, groupChain);
    }

    private boolean resolveRequestInheritanceSnapshot(JMeterTreeNode data) {
        if (data == null || data.type != NodeType.REQUEST || data.httpRequestItem == null) {
            return false;
        }
        if (data.requestInheritanceSnapshot) {
            return true;
        }
        String requestId = data.httpRequestItem.getId();
        if (requestId == null || requestId.trim().isEmpty()) {
            return false;
        }
        return new ActiveCollectionTreeNodeRepository()
                .findNodeByRequestId(requestId)
                .map(GroupInheritanceHelper::collectGroupChain)
                .map(groupChain -> !groupChain.isEmpty())
                .orElse(false);
    }

    private JMeterTreeNode nodeData(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode data)) {
            return null;
        }
        return data;
    }
}
