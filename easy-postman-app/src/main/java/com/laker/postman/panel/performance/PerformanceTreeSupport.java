package com.laker.postman.panel.performance;

import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
            DefaultMutableTreeNode connectNode = ensureFixedChildNode(
                    requestNode,
                    NodeType.SSE_CONNECT,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_NODE_CONNECT),
                    0
            );
            DefaultMutableTreeNode awaitNode = ensureFixedChildNode(
                    requestNode,
                    NodeType.SSE_AWAIT,
                    buildSseAwaitNodeTitle(requestData.ssePerformanceData),
                    1
            );
            moveChildrenByType(requestNode, awaitNode, NodeType.ASSERTION);
            treeModel.nodeChanged(connectNode);
            treeModel.nodeChanged(awaitNode);
        } else if (isWebSocket) {
            if (requestData.webSocketPerformanceData == null) {
                requestData.webSocketPerformanceData = new WebSocketPerformanceData();
            }
            DefaultMutableTreeNode connectNode = ensureFixedChildNode(
                    requestNode,
                    NodeType.WS_CONNECT,
                    I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CONNECT),
                    0
            );
            refreshWebSocketStepTitles(requestNode);
            treeModel.nodeChanged(connectNode);
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
        DefaultMutableTreeNode requestNode = resolveWebSocketStepParent(selectedNode);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return;
        }
        WebSocketPerformanceData defaults = requestJtNode.webSocketPerformanceData != null
                ? requestJtNode.webSocketPerformanceData
                : new WebSocketPerformanceData();
        DefaultMutableTreeNode newNode = createWebSocketStepNode(type, defaults);
        int insertIndex = requestNode.getChildCount();
        if (selectedNode != null && selectedNode.getParent() == requestNode) {
            insertIndex = requestNode.getIndex(selectedNode) + 1;
        }
        insertIndex = Math.max(1, insertIndex);
        treeModel.insertNodeInto(newNode, requestNode, Math.min(insertIndex, requestNode.getChildCount()));
        refreshWebSocketStepTitles(requestNode);
        jmeterTree.expandPath(new TreePath(requestNode.getPath()));
        jmeterTree.setSelectionPath(new TreePath(newNode.getPath()));
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
            insertIndex = Math.max(1, insertIndex);
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

    DefaultMutableTreeNode resolveWebSocketStepParent(DefaultMutableTreeNode selectedNode) {
        if (selectedNode == null || !(selectedNode.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return null;
        }
        if (jtNode.type == NodeType.REQUEST && isWebSocketPerfRequest(jtNode.httpRequestItem)) {
            return selectedNode;
        }
        if (jtNode.type == NodeType.WS_CONNECT || isWebSocketStepNode(jtNode.type)) {
            return getParentRequestNode(selectedNode);
        }
        return null;
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
                if (jtNode.type == NodeType.WS_AWAIT) {
                    moveChildrenByType(child, requestNode, NodeType.ASSERTION);
                }
                if (jtNode.type == NodeType.WS_SEND
                        || jtNode.type == NodeType.WS_AWAIT
                        || jtNode.type == NodeType.WS_CLOSE) {
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

    private void refreshWebSocketStepTitles(DefaultMutableTreeNode requestNode) {
        for (int i = 0; i < requestNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) requestNode.getChildAt(i);
            Object userObj = child.getUserObject();
            if (!(userObj instanceof JMeterTreeNode jtNode)) {
                continue;
            }
            switch (jtNode.type) {
                case WS_SEND -> jtNode.name = buildWebSocketSendNodeTitle(jtNode.webSocketPerformanceData);
                case WS_AWAIT -> jtNode.name = buildWebSocketAwaitNodeTitle(jtNode.webSocketPerformanceData);
                case WS_CLOSE -> jtNode.name = I18nUtil.getMessage(MessageKeys.PERFORMANCE_WS_NODE_CLOSE);
                default -> {
                }
            }
            treeModel.nodeChanged(child);
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
            case FIRST_MESSAGE -> joiner.add(formatDuration(data.firstMessageTimeoutMs));
            case MESSAGE_COUNT -> {
                joiner.add(String.valueOf(Math.max(1, data.targetMessageCount)));
                joiner.add(formatDuration(data.holdConnectionMs));
            }
            case FIXED_DURATION -> joiner.add(formatDuration(data.holdConnectionMs));
        }
        if (CharSequenceUtil.isNotBlank(data.eventNameFilter)) {
            joiner.add("event=" + data.eventNameFilter.trim());
        }
        return joiner.toString();
    }

    private String getSseCompletionModeLabel(SsePerformanceData.CompletionMode mode) {
        if (mode == null) {
            mode = SsePerformanceData.CompletionMode.FIRST_MESSAGE;
        }
        return switch (mode) {
            case FIRST_MESSAGE -> I18nUtil.getMessage(MessageKeys.PERFORMANCE_SSE_COMPLETION_FIRST_MESSAGE);
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
            joiner.add(Math.max(1, data.sendCount) + "x");
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
        if (CharSequenceUtil.isNotBlank(data.messageFilter)) {
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
