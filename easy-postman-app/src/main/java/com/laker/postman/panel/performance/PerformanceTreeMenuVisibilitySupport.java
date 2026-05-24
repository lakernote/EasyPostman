package com.laker.postman.panel.performance;

import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import lombok.RequiredArgsConstructor;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
final class PerformanceTreeMenuVisibilitySupport {

    private final PerformanceTreeSupport treeSupport;
    private final Supplier<List<DefaultMutableTreeNode>> copiedNodesSupplier;

    void configureMultiSelectionMenu(TreePath[] selectedPaths, PerformanceTreeMenuItems items) {
        hideAddItems(items);
        items.copyNode().setVisible(treeSupport.hasCopyableNodes(selectedPaths));
        items.pasteNode().setVisible(false);
        items.renameNode().setVisible(false);
        items.deleteNode().setVisible(treeSupport.hasDeletableNodes(selectedPaths));

        boolean hasDisabled = false;
        boolean hasEnabled = false;
        for (TreePath path : selectedPaths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            Object userObj = node.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type != NodeType.ROOT) {
                if (jtNode.enabled) {
                    hasEnabled = true;
                } else {
                    hasDisabled = true;
                }
            }
        }
        items.enableNode().setVisible(hasDisabled);
        items.disableNode().setVisible(hasEnabled);
    }

    void configureSingleSelectionMenu(DefaultMutableTreeNode node, PerformanceTreeMenuItems items) {
        Object userObj = node.getUserObject();
        if (!(userObj instanceof JMeterTreeNode jtNode)) {
            hideAddItems(items);
            hideToggleClipboardAndEditItems(items);
            return;
        }

        if (jtNode.type == NodeType.ROOT) {
            configureRootMenu(node, items);
            return;
        }

        items.addThreadGroup().setVisible(false);
        boolean requestContainerLoop = treeSupport.isRequestContainerLoop(node);
        boolean canManageSseStages = treeSupport.resolveSseStageParent(node) != null;
        boolean canManageWsConnect = treeSupport.resolveWebSocketConnectParent(node) != null;
        boolean canManageWsSteps = treeSupport.resolveWebSocketStepParent(node) != null;
        items.addRequest().setVisible(jtNode.type == NodeType.THREAD_GROUP || requestContainerLoop);
        items.addLoop().setVisible(jtNode.type == NodeType.THREAD_GROUP || requestContainerLoop || canManageWsSteps);
        items.addSseConnect().setVisible(canManageSseStages);
        items.addSseAwait().setVisible(canManageSseStages);
        items.addWsConnect().setVisible(canManageWsConnect);
        items.addWsSend().setVisible(canManageWsSteps);
        items.addWsAwait().setVisible(canManageWsSteps);
        items.addWsClose().setVisible(canManageWsSteps);
        items.addAssertion().setVisible(canAddAssertion(jtNode));
        items.addTimer().setVisible(jtNode.type == NodeType.REQUEST || requestContainerLoop || canManageWsSteps);
        items.copyNode().setVisible(treeSupport.hasCopyableNodes(new TreePath[]{new TreePath(node.getPath())}));
        items.pasteNode().setVisible(treeSupport.canPasteNodes(node, copiedNodesSupplier.get()));
        boolean structuralNode = isStructuralNode(jtNode.type);
        items.renameNode().setVisible(!structuralNode
                && jtNode.type != NodeType.LOOP
                && !treeSupport.isWebSocketStepNode(jtNode.type));
        items.deleteNode().setVisible(treeSupport.hasDeletableNodes(new TreePath[]{new TreePath(node.getPath())}));
        items.enableNode().setVisible(!structuralNode && !jtNode.enabled);
        items.disableNode().setVisible(!structuralNode && jtNode.enabled);
    }

    private void configureRootMenu(DefaultMutableTreeNode node, PerformanceTreeMenuItems items) {
        items.addThreadGroup().setVisible(true);
        items.addRequest().setVisible(false);
        items.addLoop().setVisible(false);
        items.addSseConnect().setVisible(false);
        items.addSseAwait().setVisible(false);
        items.addWsConnect().setVisible(false);
        items.addWsSend().setVisible(false);
        items.addWsAwait().setVisible(false);
        items.addWsClose().setVisible(false);
        items.addAssertion().setVisible(false);
        items.addTimer().setVisible(false);
        items.copyNode().setVisible(false);
        items.pasteNode().setVisible(treeSupport.canPasteNodes(node, copiedNodesSupplier.get()));
        items.renameNode().setVisible(false);
        items.deleteNode().setVisible(false);
        items.enableNode().setVisible(false);
        items.disableNode().setVisible(false);
    }

    private boolean canAddAssertion(JMeterTreeNode jtNode) {
        boolean sseRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isSsePerfRequest(jtNode.httpRequestItem);
        boolean webSocketRequestNode = jtNode.type == NodeType.REQUEST && treeSupport.isWebSocketPerfRequest(jtNode.httpRequestItem);
        return (jtNode.type == NodeType.REQUEST && !sseRequestNode && !webSocketRequestNode)
                || jtNode.type == NodeType.SSE_AWAIT
                || jtNode.type == NodeType.WS_AWAIT;
    }

    private boolean isStructuralNode(NodeType type) {
        return type == NodeType.SSE_CONNECT
                || type == NodeType.SSE_AWAIT
                || type == NodeType.WS_CONNECT;
    }

    private void hideAddItems(PerformanceTreeMenuItems items) {
        items.addThreadGroup().setVisible(false);
        items.addRequest().setVisible(false);
        items.addLoop().setVisible(false);
        items.addSseConnect().setVisible(false);
        items.addSseAwait().setVisible(false);
        items.addWsConnect().setVisible(false);
        items.addWsSend().setVisible(false);
        items.addWsAwait().setVisible(false);
        items.addWsClose().setVisible(false);
        items.addAssertion().setVisible(false);
        items.addTimer().setVisible(false);
    }

    private void hideToggleClipboardAndEditItems(PerformanceTreeMenuItems items) {
        items.enableNode().setVisible(false);
        items.disableNode().setVisible(false);
        items.copyNode().setVisible(false);
        items.pasteNode().setVisible(false);
        items.renameNode().setVisible(false);
        items.deleteNode().setVisible(false);
    }
}
