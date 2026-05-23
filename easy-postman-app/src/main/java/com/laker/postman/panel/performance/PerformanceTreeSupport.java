package com.laker.postman.panel.performance;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Predicate;

final class PerformanceTreeSupport {

    private final DefaultTreeModel treeModel;

    PerformanceTreeSupport(DefaultTreeModel treeModel) {
        this.treeModel = treeModel;
    }

    RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    boolean isSsePerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item));
    }

    boolean isSsePerfRequest(HttpRequestItem item, PreparedRequest req) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(req));
    }

    boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return resolveRequestProtocol(item).isWebSocketProtocol();
    }

    DefaultMutableTreeNode getParentRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode current = node;
        while (current != null) {
            Object userObj = current.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
                return current;
            }
            current = (DefaultMutableTreeNode) current.getParent();
        }
        return null;
    }

    void syncRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        syncRequestStructure(requestNode, requestData, false);
    }

    void ensureRequestStructure(DefaultMutableTreeNode requestNode, JMeterTreeNode requestData) {
        syncRequestStructure(requestNode, requestData, true);
    }

    private void syncRequestStructure(DefaultMutableTreeNode requestNode,
                                      JMeterTreeNode requestData,
                                      boolean ensureMissingStages) {
        if (requestNode == null || requestData == null || requestData.httpRequestItem == null) {
            return;
        }

        boolean isSse = isSsePerfRequest(requestData.httpRequestItem);
        boolean isWebSocket = isWebSocketPerfRequest(requestData.httpRequestItem);

        cleanupSseRequestStructure(requestNode, !isSse);
        cleanupWebSocketRequestStructure(requestNode, !isWebSocket);

        if (isSse) {
            if (requestData.ssePerformanceData == null) {
                requestData.ssePerformanceData = new SsePerformanceData();
            }
            if (ensureMissingStages) {
                ensureFixedChildNode(
                        requestNode,
                        NodeType.SSE_CONNECT,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                        0
                );
                ensureFixedChildNode(
                        requestNode,
                        NodeType.SSE_AWAIT,
                        buildSseAwaitNodeTitle(requestData.ssePerformanceData),
                        1
                );
            }
            DefaultMutableTreeNode awaitNode = findChildNode(requestNode, NodeType.SSE_AWAIT);
            if (awaitNode != null) {
                moveChildrenByType(requestNode, awaitNode, NodeType.ASSERTION);
            }
            refreshSseStageTitles(requestNode, requestData.ssePerformanceData);
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

    void syncAllRequestStructures(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.REQUEST) {
            syncRequestStructure(node, jtNode);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            syncAllRequestStructures((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    void addWebSocketStepNode(JTree jmeterTree, NodeType type, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parentNode = resolveWebSocketStepParent(selectedNode);
        DefaultMutableTreeNode requestNode = getParentWebSocketRequestNode(parentNode);
        if (parentNode == null || requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        WebSocketPerformanceData defaults = requestJtNode.webSocketPerformanceData != null
                ? requestJtNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        DefaultMutableTreeNode newNode = createWebSocketStepNode(type, defaults);
        int insertIndex = parentNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == parentNode) {
            insertIndex = parentNode.getIndex(selectedNode) + 1;
        }
        if (parentNode == requestNode) {
            insertIndex = Math.max(1, insertIndex);
        }
        treeModel.insertNodeInto(newNode, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        refreshWebSocketStepTitles(requestNode);
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
        saveConfigAction.run();
    }

    void addSseStageNode(JTree jmeterTree, NodeType type, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode requestNode = resolveSseStageParent(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        if (requestJtNode.ssePerformanceData == null) {
            requestJtNode.ssePerformanceData = new SsePerformanceData();
        }
        DefaultMutableTreeNode newNode = createSseStageNode(type, requestJtNode.ssePerformanceData);
        int insertIndex = requestNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == requestNode) {
            insertIndex = requestNode.getIndex(selectedNode) + 1;
        }
        treeModel.insertNodeInto(newNode, requestNode, Math.min(insertIndex, requestNode.getChildCount()));
        refreshSseStageTitles(requestNode, requestJtNode.ssePerformanceData);
        jmeterTree.expandPath(new TreePath(requestNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
        saveConfigAction.run();
    }

    void addLoopNode(JTree jmeterTree, Runnable saveConfigAction) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) jmeterTree.getLastSelectedPathComponent();
        DefaultMutableTreeNode parentNode = resolveLoopInsertParent(selectedNode);
        if (parentNode == null) {
            return;
        }
        LoopData data = new LoopData();
        JMeterTreeNode loopData = new JMeterTreeNode(buildLoopNodeTitle(data), NodeType.LOOP, data);
        DefaultMutableTreeNode loopNode = new DefaultMutableTreeNode(loopData);

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

    void addTimerNode(JTree jmeterTree, Runnable saveConfigAction) {
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
        DefaultMutableTreeNode timer = new DefaultMutableTreeNode(new JMeterTreeNode("Timer", NodeType.TIMER));
        treeModel.insertNodeInto(timer, parentNode, Math.min(insertIndex, parentNode.getChildCount()));
        jmeterTree.expandPath(new TreePath(parentNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(timer.getPath()));
        saveConfigAction.run();
    }

    boolean isWebSocketStepNode(NodeType type) {
        return type == NodeType.WS_SEND || type == NodeType.WS_AWAIT || type == NodeType.WS_CLOSE;
    }

    boolean isRequestContainerLoop(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.LOOP && getParentRequestNode(node) == null;
    }

    boolean hasCopyableNodes(TreePath[] selectedPaths) {
        return !copyableTopLevelNodes(selectedPaths).isEmpty();
    }

    boolean hasDeletableNodes(TreePath[] selectedPaths) {
        return !deletableTopLevelNodes(selectedPaths).isEmpty();
    }

    List<DefaultMutableTreeNode> copyNodes(TreePath[] selectedPaths) {
        List<DefaultMutableTreeNode> nodes = copyableTopLevelNodes(selectedPaths);
        List<DefaultMutableTreeNode> copies = new ArrayList<>(nodes.size());
        for (DefaultMutableTreeNode node : nodes) {
            copies.add(copyTreeNode(node));
        }
        return copies;
    }

    List<DefaultMutableTreeNode> deleteNodes(TreePath[] selectedPaths) {
        List<DefaultMutableTreeNode> nodes = deletableTopLevelNodes(selectedPaths);
        for (DefaultMutableTreeNode node : nodes) {
            if (node.getParent() != null) {
                treeModel.removeNodeFromParent(node);
            }
        }
        return nodes;
    }

    boolean canPasteNodes(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> copiedNodes) {
        return resolvePasteLocation(targetNode, copiedNodes) != null;
    }

    List<DefaultMutableTreeNode> pasteNodes(JTree jmeterTree,
                                            DefaultMutableTreeNode targetNode,
                                            List<DefaultMutableTreeNode> copiedNodes) {
        PasteLocation pasteLocation = resolvePasteLocation(targetNode, copiedNodes);
        if (pasteLocation == null) {
            return List.of();
        }

        List<DefaultMutableTreeNode> pastedNodes = new ArrayList<>(copiedNodes.size());
        int insertIndex = pasteLocation.index();
        for (DefaultMutableTreeNode copiedNode : copiedNodes) {
            DefaultMutableTreeNode pastedNode = copyTreeNode(copiedNode);
            treeModel.insertNodeInto(pastedNode, pasteLocation.parent(),
                    Math.min(insertIndex, pasteLocation.parent().getChildCount()));
            pastedNodes.add(pastedNode);
            insertIndex++;
        }

        Object root = treeModel.getRoot();
        if (root instanceof DefaultMutableTreeNode rootNode) {
            syncAllRequestStructures(rootNode);
        }
        if (jmeterTree != null) {
            jmeterTree.expandPath(new TreePath(pasteLocation.parent().getPath()));
            TreePath[] pastedPaths = pastedNodes.stream()
                    .map(node -> new TreePath(node.getPath()))
                    .toArray(TreePath[]::new);
            jmeterTree.setSelectionPaths(pastedPaths);
        }
        return pastedNodes;
    }

    private List<DefaultMutableTreeNode> copyableTopLevelNodes(TreePath[] selectedPaths) {
        return topLevelNodes(selectedPaths, this::isCopyableNode);
    }

    private List<DefaultMutableTreeNode> deletableTopLevelNodes(TreePath[] selectedPaths) {
        return topLevelNodes(selectedPaths, this::isDeletableNode);
    }

    private List<DefaultMutableTreeNode> topLevelNodes(TreePath[] selectedPaths,
                                                       Predicate<DefaultMutableTreeNode> nodeFilter) {
        if (selectedPaths == null || selectedPaths.length == 0) {
            return List.of();
        }
        List<DefaultMutableTreeNode> selectedNodes = new ArrayList<>();
        for (TreePath path : selectedPaths) {
            if (path == null || !(path.getLastPathComponent() instanceof DefaultMutableTreeNode node)) {
                continue;
            }
            if (nodeFilter.test(node)) {
                selectedNodes.add(node);
            }
        }
        selectedNodes.sort(this::compareTreeOrder);

        List<DefaultMutableTreeNode> topLevelNodes = new ArrayList<>();
        for (DefaultMutableTreeNode node : selectedNodes) {
            if (!hasSelectedAncestor(node, selectedNodes)) {
                topLevelNodes.add(node);
            }
        }
        return topLevelNodes;
    }

    private boolean isDeletableNode(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type != NodeType.ROOT;
    }

    private boolean isCopyableNode(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type != NodeType.ROOT;
    }

    private boolean hasSelectedAncestor(DefaultMutableTreeNode node, List<DefaultMutableTreeNode> selectedNodes) {
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        while (parent != null) {
            if (selectedNodes.contains(parent)) {
                return true;
            }
            parent = (DefaultMutableTreeNode) parent.getParent();
        }
        return false;
    }

    private int compareTreeOrder(DefaultMutableTreeNode first, DefaultMutableTreeNode second) {
        TreePath firstPath = new TreePath(first.getPath());
        TreePath secondPath = new TreePath(second.getPath());
        int firstCount = firstPath.getPathCount();
        int secondCount = secondPath.getPathCount();
        int sharedCount = Math.min(firstCount, secondCount);
        for (int i = 0; i < sharedCount; i++) {
            Object firstComponent = firstPath.getPathComponent(i);
            Object secondComponent = secondPath.getPathComponent(i);
            if (firstComponent == secondComponent) {
                continue;
            }
            DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode) firstComponent;
            DefaultMutableTreeNode secondNode = (DefaultMutableTreeNode) secondComponent;
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) firstNode.getParent();
            return Integer.compare(parent.getIndex(firstNode), parent.getIndex(secondNode));
        }
        return Integer.compare(firstCount, secondCount);
    }

    private DefaultMutableTreeNode copyTreeNode(DefaultMutableTreeNode source) {
        JMeterTreeNode sourceData = source.getUserObject() instanceof JMeterTreeNode jtNode ? jtNode : null;
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(copyNodeData(sourceData));
        for (int i = 0; i < source.getChildCount(); i++) {
            copy.add(copyTreeNode((DefaultMutableTreeNode) source.getChildAt(i)));
        }
        return copy;
    }

    private JMeterTreeNode copyNodeData(JMeterTreeNode source) {
        if (source == null) {
            return new JMeterTreeNode("", NodeType.ROOT);
        }
        JMeterTreeNode copy = new JMeterTreeNode(source.name, source.type);
        copy.enabled = source.enabled;
        copy.threadGroupData = JsonUtil.deepCopy(source.threadGroupData, com.laker.postman.panel.performance.threadgroup.ThreadGroupData.class);
        copy.loopData = JsonUtil.deepCopy(source.loopData, LoopData.class);
        copy.httpRequestItem = JsonUtil.deepCopy(source.httpRequestItem, HttpRequestItem.class);
        if (copy.httpRequestItem != null) {
            copy.httpRequestItem.setId(UUID.randomUUID().toString());
        }
        copy.assertionData = JsonUtil.deepCopy(source.assertionData, com.laker.postman.panel.performance.assertion.AssertionData.class);
        copy.timerData = JsonUtil.deepCopy(source.timerData, com.laker.postman.panel.performance.timer.TimerData.class);
        copy.ssePerformanceData = JsonUtil.deepCopy(source.ssePerformanceData, SsePerformanceData.class);
        copy.webSocketPerformanceData = JsonUtil.deepCopy(source.webSocketPerformanceData, WebSocketPerformanceData.class);
        return copy;
    }

    private PasteLocation resolvePasteLocation(DefaultMutableTreeNode targetNode, List<DefaultMutableTreeNode> copiedNodes) {
        if (targetNode == null || copiedNodes == null || copiedNodes.isEmpty()) {
            return null;
        }
        if (canAcceptAll(targetNode, copiedNodes)) {
            return new PasteLocation(targetNode, targetNode.getChildCount());
        }
        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) targetNode.getParent();
        if (parentNode != null && canAcceptAll(parentNode, copiedNodes)) {
            return new PasteLocation(parentNode, parentNode.getIndex(targetNode) + 1);
        }
        return null;
    }

    private boolean canAcceptAll(DefaultMutableTreeNode parentNode, List<DefaultMutableTreeNode> children) {
        for (DefaultMutableTreeNode child : children) {
            if (!canAcceptChild(parentNode, child)) {
                return false;
            }
        }
        return true;
    }

    private boolean canAcceptChild(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode) {
        if (parentNode == null || childNode == null
                || !(childNode.getUserObject() instanceof JMeterTreeNode childData)) {
            return false;
        }
        return switch (childData.type) {
            case THREAD_GROUP -> isNodeType(parentNode, NodeType.ROOT);
            case REQUEST -> isRequestContainerTarget(parentNode);
            case ASSERTION -> isNodeType(parentNode, NodeType.REQUEST)
                    || isNodeType(parentNode, NodeType.SSE_AWAIT)
                    || isNodeType(parentNode, NodeType.WS_AWAIT);
            case TIMER -> isNodeType(parentNode, NodeType.REQUEST)
                    || isRequestContainerLoop(parentNode)
                    || isWebSocketStepContainerTarget(parentNode);
            case SSE_CONNECT, SSE_AWAIT -> isSseStageContainerTarget(parentNode);
            case WS_CONNECT -> isWebSocketStepContainerTarget(parentNode);
            case WS_SEND, WS_AWAIT, WS_CLOSE -> isWebSocketStepContainerTarget(parentNode);
            case LOOP -> canAcceptLoop(parentNode, childNode);
            case ROOT -> false;
        };
    }

    private boolean canAcceptLoop(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode loopNode) {
        boolean hasRequest = containsNodeType(loopNode, NodeType.REQUEST);
        boolean hasWebSocketStep = containsAnyNodeType(
                loopNode,
                NodeType.WS_CONNECT,
                NodeType.WS_SEND,
                NodeType.WS_AWAIT,
                NodeType.WS_CLOSE
        );
        if (hasRequest && hasWebSocketStep) {
            return false;
        }
        if (hasRequest) {
            return isRequestContainerTarget(parentNode);
        }
        if (hasWebSocketStep) {
            return isWebSocketStepContainerTarget(parentNode);
        }
        return isRequestContainerTarget(parentNode) || isWebSocketStepContainerTarget(parentNode);
    }

    private boolean isRequestContainerTarget(DefaultMutableTreeNode node) {
        return isNodeType(node, NodeType.THREAD_GROUP) || isRequestContainerLoop(node);
    }

    private boolean isSseStageContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.REQUEST && isSsePerfRequest(jtNode.httpRequestItem);
    }

    private boolean isWebSocketStepContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        if (jtNode.type == NodeType.REQUEST) {
            return isWebSocketPerfRequest(jtNode.httpRequestItem);
        }
        return jtNode.type == NodeType.LOOP && getParentWebSocketRequestNode(node) != null;
    }

    private boolean isNodeType(DefaultMutableTreeNode node, NodeType type) {
        return node != null
                && node.getUserObject() instanceof JMeterTreeNode jtNode
                && jtNode.type == type;
    }

    private boolean containsAnyNodeType(DefaultMutableTreeNode node, NodeType... types) {
        for (NodeType type : types) {
            if (containsNodeType(node, type)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsNodeType(DefaultMutableTreeNode node, NodeType type) {
        if (node == null) {
            return false;
        }
        if (node.getUserObject() instanceof JMeterTreeNode jtNode && jtNode.type == type) {
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (containsNodeType((DefaultMutableTreeNode) node.getChildAt(i), type)) {
                return true;
            }
        }
        return false;
    }

    private record PasteLocation(DefaultMutableTreeNode parent, int index) {
    }

    DefaultMutableTreeNode resolveWebSocketStepParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && isWebSocketPerfRequest(jtNode.httpRequestItem)) {
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

    DefaultMutableTreeNode resolveSseStageParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && isSsePerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        DefaultMutableTreeNode requestNode = getParentRequestNode(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return null;
        }
        return isSsePerfRequest(requestJtNode.httpRequestItem) ? requestNode : null;
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

    private boolean isWebSocketScenarioLoop(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.LOOP && getParentWebSocketRequestNode(node) != null;
    }

    private DefaultMutableTreeNode getParentWebSocketRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return null;
        }
        return isWebSocketPerfRequest(requestJtNode.httpRequestItem) ? requestNode : null;
    }

    static void createDefaultRequest(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode group = new DefaultMutableTreeNode(
                new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_THREAD_GROUP), NodeType.THREAD_GROUP)
        );
        HttpRequestItem defaultReq = new HttpRequestItem();
        defaultReq.setName(I18nUtil.getMessage(MessageKeys.PERFORMANCE_DEFAULT_REQUEST));
        defaultReq.setMethod("GET");
        defaultReq.setUrl("https://www.baidu.com");
        DefaultMutableTreeNode req = new DefaultMutableTreeNode(
                new JMeterTreeNode(defaultReq.getName(), NodeType.REQUEST, defaultReq)
        );
        group.add(req);
        root.add(group);
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
        DefaultMutableTreeNode awaitNode = findChildNode(requestNode, NodeType.SSE_AWAIT);
        if (removeNodes && awaitNode != null) {
            moveChildrenByType(awaitNode, requestNode, NodeType.ASSERTION);
        }
        if (removeNodes && connectNode != null) {
            treeModel.removeNodeFromParent(connectNode);
        }
        if (removeNodes && awaitNode != null) {
            treeModel.removeNodeFromParent(awaitNode);
        }
    }

    private void cleanupWebSocketRequestStructure(DefaultMutableTreeNode requestNode, boolean removeNodes) {
        DefaultMutableTreeNode connectNode = findChildNode(requestNode, NodeType.WS_CONNECT);
        List<DefaultMutableTreeNode> wsStepNodes = new ArrayList<>();
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                if (removeNodes && isWebSocketScenarioNode(jtNode.type)) {
                    moveAssertionsFromWebSocketScenario(child, requestNode);
                    wsStepNodes.add(child);
                }
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
                || type == NodeType.WS_AWAIT
                || type == NodeType.WS_CLOSE
                || type == NodeType.LOOP;
    }

    private void moveAssertionsFromWebSocketScenario(DefaultMutableTreeNode from, DefaultMutableTreeNode requestNode) {
        if (from == null || requestNode == null) {
            return;
        }
        Object userObj = from.getUserObject();
        if (userObj instanceof JMeterTreeNode jtNode && jtNode.type == NodeType.WS_AWAIT) {
            moveChildrenByType(from, requestNode, NodeType.ASSERTION);
        }
        for (int i = 0; i < from.getChildCount(); i++) {
            moveAssertionsFromWebSocketScenario((DefaultMutableTreeNode) from.getChildAt(i), requestNode);
        }
    }

    private DefaultMutableTreeNode createSseStageNode(NodeType type, SsePerformanceData requestData) {
        JMeterTreeNode nodeData;
        switch (type) {
            case SSE_CONNECT -> nodeData = new JMeterTreeNode(
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                    NodeType.SSE_CONNECT
            );
            case SSE_AWAIT -> nodeData = new JMeterTreeNode(buildSseAwaitNodeTitle(requestData), NodeType.SSE_AWAIT);
            default -> throw new IllegalArgumentException("Unsupported SSE stage type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    private void refreshWebSocketStepTitles(DefaultMutableTreeNode requestNode) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            refreshWebSocketScenarioNodeTitle((DefaultMutableTreeNode) requestNode.getChildAt(i));
        }
    }

    private void refreshSseStageTitles(DefaultMutableTreeNode requestNode, SsePerformanceData data) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (userObj instanceof JMeterTreeNode jtNode) {
                switch (jtNode.type) {
                    case SSE_CONNECT -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT);
                    case SSE_AWAIT -> jtNode.name = buildSseAwaitNodeTitle(data);
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
                case WS_SEND -> jtNode.name = buildWebSocketSendNodeTitle(jtNode.webSocketPerformanceData);
                case WS_AWAIT -> jtNode.name = buildWebSocketAwaitNodeTitle(jtNode.webSocketPerformanceData);
                case WS_CLOSE -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE);
                case LOOP -> jtNode.name = buildLoopNodeTitle(jtNode.loopData);
                default -> {
                }
            }
            treeModel.nodeChanged(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            refreshWebSocketScenarioNodeTitle((DefaultMutableTreeNode) node.getChildAt(i));
        }
    }

    private WebSocketPerformanceData copyWebSocketData(WebSocketPerformanceData source) {
        WebSocketPerformanceData target = new WebSocketPerformanceData();
        if (source == null) {
            return target;
        }
        target.connectTimeoutMs = source.connectTimeoutMs;
        target.sendMode = source.sendMode;
        target.sendContentSource = source.sendContentSource;
        target.customSendBody = source.customSendBody;
        target.sendPreScript = source.sendPreScript;
        target.sendCount = source.sendCount;
        target.sendIntervalMs = source.sendIntervalMs;
        target.completionMode = source.completionMode;
        target.firstMessageTimeoutMs = source.firstMessageTimeoutMs;
        target.holdConnectionMs = source.holdConnectionMs;
        target.targetMessageCount = source.targetMessageCount;
        target.messageFilter = source.messageFilter;
        return target;
    }

    private DefaultMutableTreeNode createWebSocketStepNode(NodeType type, WebSocketPerformanceData requestDefaults) {
        JMeterTreeNode nodeData;
        switch (type) {
            case WS_CONNECT -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT), NodeType.WS_CONNECT);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            case WS_SEND -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND), NodeType.WS_SEND);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.sendMode = WebSocketPerformanceData.SendMode.REQUEST_BODY_ON_CONNECT;
                stepData.sendCount = 1;
                stepData.sendIntervalMs = 1000;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = buildWebSocketSendNodeTitle(stepData);
            }
            case WS_AWAIT -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT), NodeType.WS_AWAIT);
                WebSocketPerformanceData stepData = copyWebSocketData(requestDefaults);
                stepData.completionMode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
                stepData.firstMessageTimeoutMs = Math.max(100, stepData.firstMessageTimeoutMs);
                stepData.targetMessageCount = 1;
                nodeData.webSocketPerformanceData = stepData;
                nodeData.name = buildWebSocketAwaitNodeTitle(stepData);
            }
            case WS_CLOSE -> {
                nodeData = new JMeterTreeNode(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE), NodeType.WS_CLOSE);
                nodeData.webSocketPerformanceData = copyWebSocketData(requestDefaults);
            }
            default -> throw new IllegalArgumentException("Unsupported WebSocket step type: " + type);
        }
        return new DefaultMutableTreeNode(nodeData);
    }

    private String buildSseAwaitNodeTitle(SsePerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_AWAIT) + " [",
                "]"
        );
        joiner.add(getSseCompletionModeLabel(data.completionMode));
        switch (data.completionMode) {
            case FIRST_MESSAGE, MATCHED_MESSAGE -> joiner.add(formatDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION -> joiner.add(formatDuration(data.holdConnectionMs));
        }
        if (data.completionMode == SsePerformanceData.CompletionMode.MATCHED_MESSAGE
                && CharSequenceUtil.isNotBlank(data.messageFilter)) {
            joiner.add("contains=" + data.messageFilter.trim());
        }
        if (SsePerformanceData.usesEventNameFilter(data.completionMode)
                && CharSequenceUtil.isNotBlank(data.eventNameFilter)) {
            joiner.add("event=" + data.eventNameFilter.trim());
        }
        return joiner.toString();
    }

    private String buildLoopNodeTitle(LoopData data) {
        if (data == null) {
            data = new LoopData();
        }
        data.normalize();
        return I18nUtil.getMessage(MessageKeys.PERFORMANCE_LOOP_NODE)
                + " [" + data.iterations + "x]";
    }

    private String getSseCompletionModeLabel(SsePerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_MESSAGE_COUNT);
        };
    }

    private String buildWebSocketSendNodeTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND);
        }
        String modeLabel = switch (data.sendMode) {
            case NONE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_NONE);
            case REQUEST_BODY_ON_CONNECT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY);
            case REQUEST_BODY_REPEAT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_REQUEST_BODY_REPEAT);
        };
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_SEND) + " [",
                "]"
        );
        joiner.add(modeLabel);
        WebSocketPerformanceData.SendContentSource contentSource = data.sendContentSource != null
                ? data.sendContentSource
                : WebSocketPerformanceData.SendContentSource.REQUEST_BODY;
        if (data.sendMode != WebSocketPerformanceData.SendMode.NONE
                && contentSource == WebSocketPerformanceData.SendContentSource.CUSTOM_TEXT) {
            joiner.add(I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_SEND_CONTENT_CUSTOM_TEXT));
        }
        if (data.sendMode == WebSocketPerformanceData.SendMode.REQUEST_BODY_REPEAT) {
            joiner.add(I18nUtil.getMessage(
                    MessageKeys.PERFORMANCE_WS_SEND_PER_LOOP_COUNT,
                    Math.max(1, data.sendCount)
            ));
            joiner.add(formatDuration(Math.max(0, data.sendIntervalMs)));
        }
        return joiner.toString();
    }

    private String buildWebSocketAwaitNodeTitle(WebSocketPerformanceData data) {
        if (data == null) {
            return I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT);
        }
        StringJoiner joiner = new StringJoiner(
                " | ",
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_AWAIT) + " [",
                "]"
        );
        joiner.add(getWebSocketCompletionModeLabel(data.completionMode));
        switch (data.completionMode) {
            case FIRST_MESSAGE, MATCHED_MESSAGE -> joiner.add(formatDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION -> joiner.add(formatDuration(data.holdConnectionMs));
        }
        if (WebSocketPerformanceData.usesMessageFilter(data.completionMode)
                && CharSequenceUtil.isNotBlank(data.messageFilter)) {
            joiner.add("contains=" + data.messageFilter.trim());
        }
        return joiner.toString();
    }

    private String getWebSocketCompletionModeLabel(WebSocketPerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = WebSocketPerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIRST_MESSAGE);
            case MATCHED_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MATCHED_MESSAGE);
            case FIXED_DURATION -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_FIXED_DURATION);
            case MESSAGE_COUNT -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_COMPLETION_MESSAGE_COUNT);
        };
    }

    private String formatDuration(int durationMs) {
        if (durationMs >= 1000 && durationMs % 1000 == 0) {
            return (durationMs / 1000) + "s";
        }
        return durationMs + "ms";
    }
}
