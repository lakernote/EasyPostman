package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.service.http.HttpUtil;

import javax.swing.tree.DefaultMutableTreeNode;

public final class PerformanceTreeRules {

    private PerformanceTreeRules() {
    }

    public static RequestItemProtocolEnum resolveRequestProtocol(HttpRequestItem item) {
        return item != null && item.getProtocol() != null ? item.getProtocol() : RequestItemProtocolEnum.HTTP;
    }

    public static boolean isSsePerfRequest(HttpRequestItem item) {
        RequestItemProtocolEnum protocol = resolveRequestProtocol(item);
        return protocol.isSseProtocol() || (protocol.isHttpProtocol() && HttpUtil.isSSERequest(item));
    }

    public static boolean isWebSocketPerfRequest(HttpRequestItem item) {
        return resolveRequestProtocol(item).isWebSocketProtocol();
    }

    public static boolean canAcceptChild(DefaultMutableTreeNode parentNode, DefaultMutableTreeNode childNode) {
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
            case WS_CONNECT -> isWebSocketRequestTarget(parentNode);
            case WS_SEND, WS_AWAIT, WS_CLOSE -> isWebSocketStepContainerTarget(parentNode);
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

    public static boolean isRequestContainerTarget(DefaultMutableTreeNode node) {
        return isNodeType(node, NodeType.THREAD_GROUP) || isRequestContainerLoop(node);
    }

    public static boolean isRequestContainerLoop(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode) || jtNode.type != NodeType.LOOP) {
            return false;
        }
        return getParentRequestNode(node) == null;
    }

    public static boolean isSseStageContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.REQUEST && isSsePerfRequest(jtNode.httpRequestItem);
    }

    public static boolean isWebSocketRequestTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        return jtNode.type == NodeType.REQUEST && isWebSocketPerfRequest(jtNode.httpRequestItem);
    }

    public static boolean isWebSocketStepContainerTarget(DefaultMutableTreeNode node) {
        if (node == null || !(node.getUserObject() instanceof JMeterTreeNode jtNode)) {
            return false;
        }
        if (jtNode.type == NodeType.REQUEST) {
            return isWebSocketRequestTarget(node);
        }
        return jtNode.type == NodeType.LOOP && getParentWebSocketRequestNode(node) != null;
    }

    public static DefaultMutableTreeNode getParentRequestNode(DefaultMutableTreeNode node) {
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

    public static DefaultMutableTreeNode getParentWebSocketRequestNode(DefaultMutableTreeNode node) {
        DefaultMutableTreeNode requestNode = getParentRequestNode(node);
        if (requestNode == null || !(requestNode.getUserObject() instanceof JMeterTreeNode requestJtNode)) {
            return null;
        }
        return isWebSocketPerfRequest(requestJtNode.httpRequestItem) ? requestNode : null;
    }

    public static boolean isNodeType(DefaultMutableTreeNode node, NodeType type) {
        return node != null
                && node.getUserObject() instanceof JMeterTreeNode jtNode
                && jtNode.type == type;
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
}
