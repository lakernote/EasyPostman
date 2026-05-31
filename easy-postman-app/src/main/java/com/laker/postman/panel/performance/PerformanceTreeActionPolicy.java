package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;


import com.laker.postman.performance.model.PerformanceTreeNode;
import lombok.RequiredArgsConstructor;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.EnumSet;
import java.util.List;

@RequiredArgsConstructor
final class PerformanceTreeActionPolicy {

    private final PerformanceTreeSupport treeSupport;

    EnumSet<PerformanceTreeAction> actionsForSingleSelection(DefaultMutableTreeNode node,
                                                             List<DefaultMutableTreeNode> copiedNodes) {
        EnumSet<PerformanceTreeAction> actions = EnumSet.noneOf(PerformanceTreeAction.class);
        PerformanceTreeNode nodeData = treeNodeData(node);
        if (nodeData == null) {
            return actions;
        }

        if (nodeData.type == NodeType.ROOT) {
            actions.add(PerformanceTreeAction.ADD_THREAD_GROUP);
            addPasteAction(actions, node, copiedNodes);
            return actions;
        }

        boolean requestContainerLoop = treeSupport.isRequestContainerLoop(node);
        boolean canAddCsvDataSet = treeSupport.resolveCsvDataSetParent(node) != null;
        boolean canManageSseStages = treeSupport.resolveSseStageParent(node) != null;
        boolean canManageWsConnect = treeSupport.resolveWebSocketConnectParent(node) != null;
        boolean canManageWsSteps = treeSupport.resolveWebSocketStepParent(node) != null;

        if (canAddCsvDataSet) {
            actions.add(PerformanceTreeAction.ADD_CSV_DATA_SET);
        }
        if (nodeData.type == NodeType.THREAD_GROUP || requestContainerLoop) {
            actions.add(PerformanceTreeAction.ADD_REQUEST);
        }
        if (nodeData.type == NodeType.THREAD_GROUP || requestContainerLoop || canManageWsSteps) {
            actions.add(PerformanceTreeAction.ADD_LOOP);
        }
        if (canManageSseStages) {
            actions.add(PerformanceTreeAction.ADD_SSE_CONNECT);
            actions.add(PerformanceTreeAction.ADD_SSE_READ);
        }
        if (canManageWsConnect) {
            actions.add(PerformanceTreeAction.ADD_WS_CONNECT);
        }
        if (canManageWsSteps) {
            actions.add(PerformanceTreeAction.ADD_WS_SEND);
            actions.add(PerformanceTreeAction.ADD_WS_READ);
            actions.add(PerformanceTreeAction.ADD_WS_CLOSE);
        }
        if (canAddAssertion(nodeData)) {
            actions.add(PerformanceTreeAction.ADD_ASSERTION);
        }
        if (canAddExtractor(nodeData)) {
            actions.add(PerformanceTreeAction.ADD_EXTRACTOR);
        }
        if (nodeData.type == NodeType.REQUEST || requestContainerLoop || canManageWsSteps) {
            actions.add(PerformanceTreeAction.ADD_TIMER);
        }
        if (treeSupport.hasCopyableNodes(singlePath(node))) {
            actions.add(PerformanceTreeAction.COPY);
        }
        addPasteAction(actions, node, copiedNodes);
        if (canRename(node)) {
            actions.add(PerformanceTreeAction.RENAME);
        }
        if (treeSupport.hasDeletableNodes(singlePath(node))) {
            actions.add(PerformanceTreeAction.DELETE);
        }
        if (canSetEnabled(node, true)) {
            actions.add(PerformanceTreeAction.ENABLE);
        }
        if (canSetEnabled(node, false)) {
            actions.add(PerformanceTreeAction.DISABLE);
        }
        return actions;
    }

    EnumSet<PerformanceTreeAction> actionsForMultiSelection(TreePath[] selectedPaths) {
        EnumSet<PerformanceTreeAction> actions = EnumSet.noneOf(PerformanceTreeAction.class);
        if (treeSupport.hasCopyableNodes(selectedPaths)) {
            actions.add(PerformanceTreeAction.COPY);
        }
        if (treeSupport.hasDeletableNodes(selectedPaths)) {
            actions.add(PerformanceTreeAction.DELETE);
        }

        boolean hasDisabled = false;
        boolean hasEnabled = false;
        if (selectedPaths != null) {
            for (TreePath path : selectedPaths) {
                if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) {
                    continue;
                }
                PerformanceTreeNode nodeData = treeNodeData(node);
                if (nodeData == null || nodeData.type == NodeType.ROOT) {
                    continue;
                }
                if (nodeData.enabled) {
                    hasEnabled = true;
                } else {
                    hasDisabled = true;
                }
            }
        }
        if (hasDisabled) {
            actions.add(PerformanceTreeAction.ENABLE);
        }
        if (hasEnabled) {
            actions.add(PerformanceTreeAction.DISABLE);
        }
        return actions;
    }

    boolean canRename(DefaultMutableTreeNode node) {
        PerformanceTreeNode nodeData = treeNodeData(node);
        return nodeData != null
                && nodeData.type != NodeType.ROOT
                && !isFixedNameNode(nodeData.type)
                && nodeData.type != NodeType.LOOP
                && nodeData.type != NodeType.EXTRACTOR
                && !treeSupport.isWebSocketStepNode(nodeData.type);
    }

    boolean canSetEnabled(DefaultMutableTreeNode node, boolean enabled) {
        PerformanceTreeNode nodeData = treeNodeData(node);
        return nodeData != null
                && nodeData.type != NodeType.ROOT
                && nodeData.enabled != enabled;
    }

    private void addPasteAction(EnumSet<PerformanceTreeAction> actions,
                                DefaultMutableTreeNode node,
                                List<DefaultMutableTreeNode> copiedNodes) {
        if (treeSupport.canPasteNodes(node, copiedNodes)) {
            actions.add(PerformanceTreeAction.PASTE);
        }
    }

    private boolean canAddAssertion(PerformanceTreeNode nodeData) {
        return canAddRequestPostProcessor(nodeData);
    }

    private boolean canAddExtractor(PerformanceTreeNode nodeData) {
        return canAddRequestPostProcessor(nodeData);
    }

    private boolean canAddRequestPostProcessor(PerformanceTreeNode nodeData) {
        boolean sseRequestNode = nodeData.type == NodeType.REQUEST && treeSupport.isSsePerfRequest(nodeData.httpRequestItem);
        boolean webSocketRequestNode = nodeData.type == NodeType.REQUEST && treeSupport.isWebSocketPerfRequest(nodeData.httpRequestItem);
        return (nodeData.type == NodeType.REQUEST && !sseRequestNode && !webSocketRequestNode)
                || nodeData.type == NodeType.SSE_READ
                || nodeData.type == NodeType.WS_READ;
    }

    private boolean isFixedNameNode(NodeType type) {
        return type == NodeType.SSE_CONNECT
                || type == NodeType.SSE_READ
                || type == NodeType.WS_CONNECT;
    }

    private TreePath[] singlePath(DefaultMutableTreeNode node) {
        return new TreePath[]{new TreePath(node.getPath())};
    }

    private PerformanceTreeNode treeNodeData(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return null;
        }
        return nodeData;
    }
}
