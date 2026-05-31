package com.laker.postman.panel.performance;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.HttpRequestItem;


import com.laker.postman.performance.core.model.NodeType;
import com.laker.postman.performance.core.model.PerformanceProtocol;


import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.panel.performance.tree.PerformanceTreeClipboardSupport;
import com.laker.postman.panel.performance.tree.PerformanceTreeNodeFactory;
import com.laker.postman.panel.performance.tree.PerformanceTreeStructureSupport;
import com.laker.postman.http.request.HttpRequestProtocol;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.List;

final class PerformanceTreeSupport {

    private final PerformanceTreeStructureSupport structureSupport;
    private final PerformanceTreeClipboardSupport clipboardSupport;

    PerformanceTreeSupport(DefaultTreeModel treeModel) {
        this.structureSupport = new PerformanceTreeStructureSupport(treeModel);
        this.clipboardSupport = new PerformanceTreeClipboardSupport(treeModel, structureSupport);
    }

    RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return PerformanceTreeRules.resolveRequestProtocol(item);
    }

    boolean isSsePerfRequest(HttpRequestItem item) {
        return PerformanceTreeRules.isSsePerfRequest(item);
    }

    boolean isSsePerfRequest(HttpRequestItem item, PreparedRequest req) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpRequestProtocol.isSse(req));
    }

    boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return PerformanceTreeRules.isWebSocketPerfRequest(item);
    }

    PerformanceProtocol resolvePerformanceProtocol(HttpRequestItem item) {
        if (isWebSocketPerfRequest(item)) {
            return PerformanceProtocol.WEBSOCKET;
        }
        if (isSsePerfRequest(item)) {
            return PerformanceProtocol.SSE;
        }
        return PerformanceProtocol.HTTP;
    }

    DefaultMutableTreeNode getParentRequestNode(DefaultMutableTreeNode node) {
        return PerformanceTreeRules.getParentRequestNode(node);
    }

    void syncRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        structureSupport.syncRequestStructure(requestNode, requestData);
    }

    void ensureRequestStructure(DefaultMutableTreeNode requestNode, PerformanceTreeNode requestData) {
        structureSupport.ensureRequestStructure(requestNode, requestData);
    }

    void syncAllRequestStructures(DefaultMutableTreeNode node) {
        structureSupport.syncAllRequestStructures(node);
    }

    void addWebSocketStepNode(JTree performanceTree, NodeType type, Runnable saveConfigAction) {
        structureSupport.addWebSocketStepNode(performanceTree, type, saveConfigAction);
    }

    void addSseStageNode(JTree performanceTree, NodeType type, Runnable saveConfigAction) {
        structureSupport.addSseStageNode(performanceTree, type, saveConfigAction);
    }

    void addLoopNode(JTree performanceTree, Runnable saveConfigAction) {
        structureSupport.addLoopNode(performanceTree, saveConfigAction);
    }

    void addCsvDataSetNode(JTree performanceTree, Runnable saveConfigAction) {
        structureSupport.addCsvDataSetNode(performanceTree, saveConfigAction);
    }

    void addTimerNode(JTree performanceTree, Runnable saveConfigAction) {
        structureSupport.addTimerNode(performanceTree, saveConfigAction);
    }

    boolean isWebSocketStepNode(NodeType type) {
        return structureSupport.isWebSocketStepNode(type);
    }

    boolean isRequestContainerLoop(DefaultMutableTreeNode node) {
        return structureSupport.isRequestContainerLoop(node);
    }

    boolean hasCopyableNodes(TreePath[] selectedPaths) {
        return clipboardSupport.hasCopyableNodes(selectedPaths);
    }

    boolean hasDeletableNodes(TreePath[] selectedPaths) {
        return clipboardSupport.hasDeletableNodes(selectedPaths);
    }

    List<DefaultMutableTreeNode> copyNodes(TreePath[] selectedPaths) {
        return clipboardSupport.copyNodes(selectedPaths);
    }

    List<DefaultMutableTreeNode> deleteNodes(TreePath[] selectedPaths) {
        return clipboardSupport.deleteNodes(selectedPaths);
    }

    boolean canPasteNodes(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> copiedNodes) {
        return clipboardSupport.canPasteNodes(targetNode, copiedNodes);
    }

    List<DefaultMutableTreeNode> pasteNodes(JTree performanceTree,
                                            DefaultMutableTreeNode targetNode,
                                            List<DefaultMutableTreeNode> copiedNodes) {
        return clipboardSupport.pasteNodes(performanceTree, targetNode, copiedNodes);
    }

    DefaultMutableTreeNode resolveWebSocketConnectParent(DefaultMutableTreeNode selectedNode) {
        return structureSupport.resolveWebSocketConnectParent(selectedNode);
    }

    DefaultMutableTreeNode resolveCsvDataSetParent(DefaultMutableTreeNode selectedNode) {
        return structureSupport.resolveCsvDataSetParent(selectedNode);
    }

    DefaultMutableTreeNode resolveWebSocketStepParent(DefaultMutableTreeNode selectedNode) {
        return structureSupport.resolveWebSocketStepParent(selectedNode);
    }

    DefaultMutableTreeNode resolveSseStageParent(DefaultMutableTreeNode selectedNode) {
        return structureSupport.resolveSseStageParent(selectedNode);
    }

    static void createDefaultRequest(DefaultMutableTreeNode root) {
        PerformanceTreeNodeFactory.addDefaultRequest(root);
    }
}
