package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
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
        JMeterTreeNode jtNode = treeNodeData(node);
        if (jtNode == null) {
            return actions;
        }

        if (jtNode.type == NodeType.ROOT) {
            actions.add(PerformanceTreeAction.ADD_THREAD_GROUP);
            addPasteAction(actions, node, copiedNodes);
            return actions;
        }

        boolean requestContainerLoop = treeSupport.isRequestContainerLoop(node);
        boolean canManageSseStages = treeSupport.resolveSseStageParent(node) != null;
        boolean canManageWsConnect = treeSupport.resolveWebSocketConnectParent(node) != null;
        boolean canManageWsSteps = treeSupport.resolveWebSocketStepParent(node) != null;

        if (jtNode.type == NodeType.THREAD_GROUP || requestContainerLoop) {
            actions.add(PerformanceTreeAction.ADD_REQUEST);
        }
        if (jtNode.type == NodeType.THREAD_GROUP || requestContainerLoop || canManageWsSteps) {
            actions.add(PerformanceTreeAction.ADD_LOOP);
        }
        if (canManageSseStages) {
            actions.add(PerformanceTreeAction.ADD_SSE_CONNECT);
            actions.add(PerformanceTreeAction.ADD_SSE_AWAIT);
        }
        if (canManageWsConnect) {
            actions.add(PerformanceTreeAction.ADD_WS_CONNECT);
        }
        if (canManageWsSteps) {
            actions.add(PerformanceTreeAction.ADD_WS_SEND);
            actions.add(PerformanceTreeAction.ADD_WS_AWAIT);
            actions.add(PerformanceTreeAction.ADD_WS_CLOSE);
        }
        if (canAddAssertion(jtNode)) {
            actions.add(PerformanceTreeAction.ADD_ASSERTION);
        }
        if (jtNode.type == NodeType.REQUEST || requestContainerLoop || canManageWsSteps) {
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
                JMeterTreeNode jtNode = treeNodeData(node);
                if (jtNode == null || jtNode.type == NodeType.ROOT) {
                    continue;
                }
                if (jtNode.enabled) {
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
        JMeterTreeNode jtNode = treeNodeData(node);
        return jtNode != null
                && jtNode.type != NodeType.ROOT
                && !isFixedNameNode(jtNode.type)
                && jtNode.type != NodeType.LOOP
                && !treeSupport.isWebSocketStepNode(jtNode.type);
    }

    boolean canSetEnabled(DefaultMutableTreeNode node, boolean enabled) {
        JMeterTreeNode jtNode = treeNodeData(node);
        return jtNode != null
                && jtNode.type != NodeType.ROOT
                && jtNode.enabled != enabled;
    }

    private void addPasteAction(EnumSet<PerformanceTreeAction> actions,
                                DefaultMutableTreeNode node,
                                List<DefaultMutableTreeNode> copiedNodes) {
        if (treeSupport.canPasteNodes(node, copiedNodes)) {
            actions.add(PerformanceTreeAction.PASTE);
        }
    }

    private boolean canAddAssertion(JMeterTreeNode jtNode) {
        boolean sseRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isSsePerfRequest(jtNode.httpRequestItem);
        boolean webSocketRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isWebSocketPerfRequest(jtNode.httpRequestItem);
        return (jtNode.type == NodeType.REQUEST && !sseRequestNode && !webSocketRequestNode)
                || jtNode.type == NodeType.SSE_AWAIT
                || jtNode.type == NodeType.WS_AWAIT;
    }

    private boolean isFixedNameNode(NodeType type) {
        return type == NodeType.SSE_CONNECT
                || type == NodeType.SSE_AWAIT
                || type == NodeType.WS_CONNECT;
    }

    private TreePath[] singlePath(DefaultMutableTreeNode node) {
        return new TreePath[]{new TreePath(node.getPath())};
    }

    private JMeterTreeNode treeNodeData(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        return jtNode;
    }
}
