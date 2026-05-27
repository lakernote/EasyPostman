package com.laker.postman.panel.performance.tree;

import com.laker.postman.panel.performance.PerformanceTreeRules;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.RequiredArgsConstructor;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class PerformanceTreeStructureSupport {

    private final DefaultTreeModel treeModel;

    public void syncRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        syncRequestStructure(requestNode, requestData, false);
    }

    public void ensureRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        syncRequestStructure(requestNode, requestData, true);
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode,
                                      JMeterTreeNode requestData,
                                      boolean ensureMissingStages) {
        if (requestNode == null || requestData == null || requestData.httpRequestItem == null) {
            return;
        }

        boolean isSse = PerformanceTreeRules.isSsePerfRequest(requestData.httpRequestItem);
        boolean isWebSocket = PerformanceTreeRules.isWebSocketPerfRequest(requestData.httpRequestItem);

        cleanupSseRequestStructure(requestNode, !isSse);
        cleanupWebSocketRequestStructure(requestNode, !isWebSocket);

        if (isSse) {
            if (ensureMissingStages) {
                DefaultMutableTreeNode connectNode = ensureFixedChildNode(
                        requestNode,
                        NodeType.SSE_CONNECT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                        0
                );
                DefaultMutableTreeNode readNode = ensureFixedChildNode(
                        requestNode,
                        NodeType.SSE_READ,
                        PerformanceTreeNodeTitleFormatter.sseReadTitle(new SsePerformanceData()),
                        1
                );
                ensureSseStageData(connectNode);
                ensureSseStageData(readNode);
            }
            ensureSseStageData(requestNode);
            DefaultMutableTreeNode readNode = findChildNode(requestNode, NodeType.SSE_READ);
            if (readNode != null) {
                moveChildrenByType(requestNode, readNode, NodeType.ASSERTION);
                moveChildrenByType(requestNode, readNode, NodeType.EXTRACTOR);
            }
            refreshSseStageTitles(requestNode);
        } else if (isWebSocket) {
            if (requestData.webSocketPerformanceData == null) {
                requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            }
            if (ensureMissingStages) {
                ensureFixedChildNode(
                        requestNode,
                        NodeType.WS_CONNECT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT),
                        0
                );
            }
            refreshWebSocketStepTitles(requestNode);
        }
    }

    public void syncAllRequestStructures(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
            syncRequestStructure(node, jtNode);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncAllRequestStructures((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    public void addWebSocketStepNode(JTree jmeterTree, NodeType type, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parentNode = type == NodeType.WS_CONNECT
                ? resolveWebSocketConnectParent(selectedNode)
                : resolveWebSocketStepParent(selectedNode);
        DefaultMutableTreeNode requestNode = type == NodeType.WS_CONNECT
                ? parentNode
                : getParentWebSocketRequestNode(parentNode);
        if (parentNode == null || requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        WebSocketPerformanceData defaults = requestJtNode.webSocketPerformanceData != null
                ? requestJtNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        DefaultMutableTreeNode newNode = PerformanceTreeNodeFactory.webSocketStepNode(type, defaults);
        int insertIndex;
        if (type == NodeType.WS_CONNECT) {
            insertIndex = resolveWebSocketConnectInsertIndex(requestNode, selectedNode);
        } else {
            insertIndex = parentNode.getChildCount();
            if (selectedNode != null && selectedNode.getParent() == parentNode) {
                insertIndex = parentNode.getIndex(selectedNode) + 1;
            }
            if (parentNode == requestNode) {
                insertIndex = Math.max(1, insertIndex);
            }
        }
        treeModel.insertNodeInto(newNode, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        refreshWebSocketStepTitles(requestNode);
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
        saveConfigAction.run();
    }

    public void addSseStageNode(JTree jmeterTree, NodeType type, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode requestNode = resolveSseStageParent(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode)) {
            return;
        }
        DefaultMutableTreeNode newNode = PerformanceTreeNodeFactory.sseStageNode(type);
        int insertIndex = requestNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == requestNode) {
            insertIndex = requestNode.getIndex(selectedNode) + 1;
        }
        treeModel.insertNodeInto(newNode, requestNode, Math.min(insertIndex, requestNode.getChildCount()));
        refreshSseStageTitles(requestNode);
        jmeterTree.expandPath(new TreePath(requestNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
        saveConfigAction.run();
    }

    public void addLoopNode(JTree jmeterTree, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parentNode = resolveLoopInsertParent(selectedNode);
        if (parentNode == null) {
            return;
        }
        DefaultMutableTreeNode loopNode = PerformanceTreeNodeFactory.loopNode();

        int insertIndex = parentNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == parentNode) {
            insertIndex = parentNode.getIndex(selectedNode) + 1;
        }
        DefaultMutableTreeNode requestNode = getParentWebSocketRequestNode(parentNode);
        if (parentNode == requestNode) {
            insertIndex = Math.max(1, insertIndex);
        }
        treeModel.insertNodeInto(loopNode, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(loopNode.getPath()));
        saveConfigAction.run();
    }

    public void addCsvDataSetNode(JTree jmeterTree, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parentNode = resolveCsvDataSetParent(selectedNode);
        if (parentNode == null) {
            return;
        }
        DefaultMutableTreeNode csvDataSetNode = PerformanceTreeNodeFactory.csvDataSetNode();
        int insertIndex = firstNonCsvChildIndex(parentNode);
        treeModel.insertNodeInto(csvDataSetNode, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(csvDataSetNode.getPath()));
        saveConfigAction.run();
    }

    public void addTimerNode(JTree jmeterTree, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        if (selectedNode == null) {
            return;
        }
        DefaultMutableTreeNode parentNode = selectedNode;
        int insertIndex = parentNode.getChildCount();
        DefaultMutableTreeNode wsParent = resolveWebSocketStepParent(selectedNode);
        if (wsParent != null) {
            parentNode = wsParent;
            insertIndex = selectedNode.getParent() == wsParent
                    ? wsParent.getIndex(selectedNode) + 1
                    : wsParent.getChildCount();
            if (wsParent == getParentWebSocketRequestNode(wsParent)) {
                insertIndex = Math.max(1, insertIndex);
            }
        } else if (isRequestContainerLoop(selectedNode)) {
            parentNode = selectedNode;
            insertIndex = parentNode.getChildCount();
        }
        DefaultMutableTreeNode timer = PerformanceTreeNodeFactory.timerNode();
        treeModel.insertNodeInto(timer, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(timer.getPath()));
        saveConfigAction.run();
    }

    public boolean isWebSocketStepNode(NodeType type) {
        return type == NodeType.WS_SEND || type == NodeType.WS_READ || type == NodeType.WS_CLOSE;
    }

    public boolean isRequestContainerLoop(DefaultMutableTreeNode node) {
        return PerformanceTreeRules.isRequestContainerLoop(node);
    }

    public DefaultMutableTreeNode resolveCsvDataSetParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.THREAD_GROUP) {
            return selectedNode;
        }
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
        if (parent != null && PerformanceTreeRules.isNodeType(parent, NodeType.THREAD_GROUP)) {
            return parent;
        }
        return null;
    }

    public DefaultMutableTreeNode resolveWebSocketConnectParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && PerformanceTreeRules.isWebSocketPerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        return getParentWebSocketRequestNode(selectedNode);
    }

    int resolveWebSocketConnectInsertIndex(DefaultMutableTreeNode requestNode,
                                           DefaultMutableTreeNode selectedNode) {
        if (requestNode == null) {
            return 0;
        }
        if (selectedNode != null
                && selectedNode.getParent() == requestNode
                && PerformanceTreeRules.isNodeType(selectedNode, NodeType.WS_CONNECT)) {
            return requestNode.getIndex(selectedNode) + 1;
        }
        int insertIndex = 0;
        while (insertIndex < requestNode.getChildCount()
                && PerformanceTreeRules.isNodeType((DefaultMutableTreeNode) requestNode.getChildAt(insertIndex), NodeType.WS_CONNECT)) {
            insertIndex++;
        }
        return insertIndex;
    }

    public DefaultMutableTreeNode resolveWebSocketStepParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && PerformanceTreeRules.isWebSocketPerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        if (jtNode.type == NodeType.LOOP && isWebSocketScenarioLoop(selectedNode)) {
            return selectedNode;
        }
        if (jtNode.type == NodeType.WS_CONNECT
                || jtNode.type == NodeType.TIMER
                || isWebSocketStepNode(jtNode.type)) {
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (isWebSocketScenarioLoop(parent)) {
                return parent;
            }
            return getParentWebSocketRequestNode(selectedNode);
        }
        return null;
    }

    public DefaultMutableTreeNode resolveSseStageParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && PerformanceTreeRules.isSsePerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        DefaultMutableTreeNode requestNode = PerformanceTreeRules.getParentRequestNode(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return null;
        }
        return PerformanceTreeRules.isSsePerfRequest(requestJtNode.httpRequestItem) ? requestNode : null;
    }

    private DefaultMutableTreeNode resolveLoopInsertParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.THREAD_GROUP || isRequestContainerLoop(selectedNode)) {
            return selectedNode;
        }
        return resolveWebSocketStepParent(selectedNode);
    }

    private int firstNonCsvChildIndex(DefaultMutableTreeNode parentNode) {
        int insertIndex = 0;
        while (insertIndex < parentNode.getChildCount()
                && PerformanceTreeRules.isNodeType((DefaultMutableTreeNode) parentNode.getChildAt(insertIndex), NodeType.CSV_DATA_SET)) {
            insertIndex++;
        }
        return insertIndex;
    }

    private boolean isWebSocketScenarioLoop(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.LOOP && getParentWebSocketRequestNode(node) != null;
    }

    private DefaultMutableTreeNode getParentWebSocketRequestNode(DefaultMutableTreeNode node) {
        return PerformanceTreeRules.getParentWebSocketRequestNode(node);
    }

    private DefaultMutableTreeNode findChildNode(DefaultMutableTreeNode parent, NodeType type) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == type) {
                return child;
            }
        }
        return null;
    }

    private DefaultMutableTreeNode ensureFixedChildNode(DefaultMutableTreeNode parent, NodeType type, String name, int index) {
        DefaultMutableTreeNode existing = findChildNode(parent, type);
        if (existing != null) {
            Object userObj = existing.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                jtNode.name = name;
            }
            if (parent.getIndex(existing) != index) {
                treeModel.removeNodeFromParent(existing);
                treeModel.insertNodeInto(existing, parent, Math.min(index, parent.getChildCount()));
            } else {
                treeModel.nodeChanged(existing);
            }
            return existing;
        }
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(new JMeterTreeNode(name, type));
        treeModel.insertNodeInto(child, parent, Math.min(index, parent.getChildCount()));
        return child;
    }

    private void moveChildrenByType(DefaultMutableTreeNode from, DefaultMutableTreeNode to, NodeType type) {
        if (from == null || to == null) {
            return;
        }
        List<DefaultMutableTreeNode> toMove = new ArrayList<>();
        for (int i = 0; i < from.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) from.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == type) {
                toMove.add(child);
            }
        }
        for (DefaultMutableTreeNode child : toMove) {
            treeModel.removeNodeFromParent(child);
            treeModel.insertNodeInto(child, to, to.getChildCount());
        }
    }

    private void cleanupSseRequestStructure(DefaultMutableTreeNode requestNode, boolean removeNodes) {
        DefaultMutableTreeNode connectNode = findChildNode(requestNode, NodeType.SSE_CONNECT);
        DefaultMutableTreeNode readNode = findChildNode(requestNode, NodeType.SSE_READ);
        if (removeNodes && readNode != null) {
            moveChildrenByType(readNode, requestNode, NodeType.ASSERTION);
            moveChildrenByType(readNode, requestNode, NodeType.EXTRACTOR);
        }
        if (removeNodes && connectNode != null) {
            treeModel.removeNodeFromParent(connectNode);
        }
        if (removeNodes && readNode != null) {
            treeModel.removeNodeFromParent(readNode);
        }
    }

    private void cleanupWebSocketRequestStructure(DefaultMutableTreeNode requestNode, boolean removeNodes) {
        DefaultMutableTreeNode connectNode = findChildNode(requestNode, NodeType.WS_CONNECT);
        List<DefaultMutableTreeNode> wsStepNodes = new ArrayList<>();
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && removeNodes && isWebSocketScenarioNode(jtNode.type)) {
                moveAssertionsFromWebSocketScenario(child, requestNode);
                wsStepNodes.add(child);
            }
        }
        if (removeNodes && connectNode != null) {
            treeModel.removeNodeFromParent(connectNode);
        }
        if (removeNodes) {
            for (DefaultMutableTreeNode wsStepNode : wsStepNodes) {
                treeModel.removeNodeFromParent(wsStepNode);
            }
        }
    }

    private boolean isWebSocketScenarioNode(NodeType type) {
        return type == NodeType.WS_SEND
                || type == NodeType.WS_READ
                || type == NodeType.WS_CLOSE
                || type == NodeType.LOOP;
    }

    private void moveAssertionsFromWebSocketScenario(DefaultMutableTreeNode from, DefaultMutableTreeNode requestNode) {
        if (from == null || requestNode == null) {
            return;
        }
        Object userObj = from.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.WS_READ) {
            moveChildrenByType(from, requestNode, NodeType.ASSERTION);
            moveChildrenByType(from, requestNode, NodeType.EXTRACTOR);
        }
        for (int i = 0; i < from.getChildCount(); i++) {
            moveAssertionsFromWebSocketScenario((DefaultMutableTreeNode) from.getChildAt(i), requestNode);
        }
    }

    private void refreshWebSocketStepTitles(DefaultMutableTreeNode requestNode) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            refreshWebSocketScenarioNodeTitle((DefaultMutableTreeNode) requestNode.getChildAt(i));
        }
    }

    private void ensureSseStageData(DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode
                && (jtNode.type == NodeType.SSE_CONNECT || jtNode.type == NodeType.SSE_READ)
                && jtNode.ssePerformanceData == null) {
            jtNode.ssePerformanceData = new SsePerformanceData();
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            ensureSseStageData((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    private void refreshSseStageTitles(DefaultMutableTreeNode requestNode) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                switch (jtNode.type) {
                    case SSE_CONNECT -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT);
                    case SSE_READ -> {
                        if (jtNode.ssePerformanceData == null) {
                            jtNode.ssePerformanceData = new SsePerformanceData();
                        }
                        jtNode.name = PerformanceTreeNodeTitleFormatter.sseReadTitle(jtNode.ssePerformanceData);
                    }
                    default -> {
                    }
                }
                treeModel.nodeChanged(child);
            }
        }
    }

    private void refreshWebSocketScenarioNodeTitle(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode) {
            switch (jtNode.type) {
                case WS_CONNECT -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT);
                case WS_SEND -> jtNode.name = PerformanceTreeNodeTitleFormatter.webSocketSendTitle(jtNode.webSocketPerformanceData);
                case WS_READ -> jtNode.name = PerformanceTreeNodeTitleFormatter.webSocketReadTitle(jtNode.webSocketPerformanceData);
                case WS_CLOSE -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE);
                case LOOP -> jtNode.name = PerformanceTreeNodeTitleFormatter.loopTitle(jtNode.loopData);
                case EXTRACTOR -> jtNode.name = PerformanceTreeNodeTitleFormatter.extractorTitle(jtNode.extractorData);
                default -> {
                }
            }
            treeModel.nodeChanged(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            refreshWebSocketScenarioNodeTitle((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }
}
