package com.laker.postman.panel.performance;

import com.laker.postman.performance.core.model.NodeType;


import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.performance.model.PerformanceProtocolRules;

import javax.swing.tree.DefaultMutableTreeNode;

public final class PerformanceTreeRules {

    private PerformanceTreeRules() {
    }

    public static RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return PerformanceProtocolRules.resolveRequestProtocol(item);
    }

    public static boolean isSsePerfRequest(HttpRequestItem item) {
        return PerformanceProtocolRules.isSsePerfRequest(item);
    }

    public static boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return PerformanceProtocolRules.isWebSocketPerfRequest(item);
    }

    public static boolean canAcceptChild(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode) {
        if (parentNode == null || childNode == null
                || !(childNode.getUserObject() instanceof PerformanceTreeNode childData)) {
            return false;
        }
        return switch (childData.type) {
            case THREAD_GROUP -> isNodeType(parentNode, NodeType.ROOT);
            case CSV_DATA_SET -> isNodeType(parentNode, NodeType.THREAD_GROUP);
            case REQUEST -> isRequestContainerTarget(parentNode);
            case ASSERTION -> isHttpRequestPostProcessorTarget(parentNode)
                    || isNodeType(parentNode, NodeType.SSE_READ)
                    || isNodeType(parentNode, NodeType.WS_READ);
            case EXTRACTOR -> isHttpRequestPostProcessorTarget(parentNode)
                    || isNodeType(parentNode, NodeType.SSE_READ)
                    || isNodeType(parentNode, NodeType.WS_READ);
            case TIMER -> isNodeType(parentNode, NodeType.REQUEST)
                    || isRequestContainerLoop(parentNode)
                    || isWebSocketStepContainerTarget(parentNode);
            case SSE_CONNECT, SSE_READ -> isSseStageContainerTarget(parentNode);
            case WS_CONNECT -> isWebSocketRequestTarget(parentNode);
            case WS_SEND, WS_READ, WS_CLOSE -> isWebSocketStepContainerTarget(parentNode);
            case LOOP -> canAcceptLoop(parentNode, childNode);
            case ROOT -> false;
        };
    }

    public static boolean canAcceptLoop(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode loopNode) {
        if (containsNodeType(loopNode, NodeType.WS_CONNECT)) {
            return false;
        }
        boolean hasRequest = containsNodeType(loopNode, NodeType.REQUEST);
        boolean hasWebSocketStep = containsAnyNodeType(
                loopNode,
                NodeType.WS_SEND,
                NodeType.WS_READ,
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

    public static boolean isRequestContainerTarget(DefaultMutableTreeNode node) {
        return isNodeType(node, NodeType.THREAD_GROUP) || isRequestContainerLoop(node);
    }

    public static boolean isRequestContainerLoop(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData) || nodeData.type != NodeType.LOOP) {
            return false;
        }
        return getParentRequestNode(node) == null;
    }

    public static boolean isSseStageContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return false;
        }
        return nodeData.type == NodeType.REQUEST && isSsePerfRequest(nodeData.httpRequestItem);
    }

    public static boolean isWebSocketRequestTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return false;
        }
        return nodeData.type == NodeType.REQUEST && isWebSocketPerfRequest(nodeData.httpRequestItem);
    }

    public static boolean isWebSocketStepContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData)) {
            return false;
        }
        if (nodeData.type == NodeType.REQUEST) {
            return isWebSocketRequestTarget(node);
        }
        return nodeData.type == NodeType.LOOP && getParentWebSocketRequestNode(node) != null;
    }

    private static boolean isHttpRequestPostProcessorTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof PerformanceTreeNode nodeData) || nodeData.type != NodeType.REQUEST) {
            return false;
        }
        return !isSsePerfRequest(nodeData.httpRequestItem) && !isWebSocketPerfRequest(nodeData.httpRequestItem);
    }

    public static DefaultMutableTreeNode getParentRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode current = node;
        while (current != null) {
            Object userObj = current.getUserObject();
            if (userObj instanceof PerformanceTreeNode nodeData && nodeData.type == NodeType.REQUEST) {
                return current;
            }
            current = (DefaultMutableTreeNode) current.getParent();
        }
        return null;
    }

    public static DefaultMutableTreeNode getParentWebSocketRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
        if (requestNode == null || !(requestNode.getUserObject() instanceof PerformanceTreeNode requestNodeData)) {
            return null;
        }
        return isWebSocketPerfRequest(requestNodeData.httpRequestItem) ? requestNode : null;
    }

    public static boolean isNodeType(DefaultMutableTreeNode node, NodeType type) {
        return node != null
                && node.getUserObject() instanceof PerformanceTreeNode nodeData
                && nodeData.type == type;
    }

    public static boolean containsAnyNodeType(DefaultMutableTreeNode node, NodeType... types) {
        for (NodeType type : types) {
            if (containsNodeType(node, type)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsNodeType(DefaultMutableTreeNode node, NodeType type) {
        if (node == null) {
            return false;
        }
        if (node.getUserObject() instanceof PerformanceTreeNode nodeData && nodeData.type == type) {
            return true;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (containsNodeType((DefaultMutableTreeNode) node.getChildAt(i), type)) {
                return true;
            }
        }
        return false;
    }
}
