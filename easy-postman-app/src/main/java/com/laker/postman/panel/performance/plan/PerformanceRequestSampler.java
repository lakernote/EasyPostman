package com.laker.postman.panel.performance.plan;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;
import com.laker.postman.panel.performance.model.SsePerformanceData;
import com.laker.postman.panel.performance.model.WebSocketPerformanceData;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceRequestSampler implements PerformancePlanElement {
    private final String name;
    private final HttpRequestItem httpRequestItem;
    private final SsePerformanceData ssePerformanceData;
    private final WebSocketPerformanceData webSocketPerformanceData;
    private final List<PerformancePlanElement> children;
    private final List<PerformancePlanNodeSnapshot> childSnapshots;
    private final ThreadLocal<DefaultMutableTreeNode> executionTreeNode;

    public PerformanceRequestSampler(String name,
                                     HttpRequestItem httpRequestItem,
                                     SsePerformanceData ssePerformanceData,
                                     WebSocketPerformanceData webSocketPerformanceData,
                                     List<PerformancePlanElement> children,
                                     List<PerformancePlanNodeSnapshot> childSnapshots) {
        this.name = name;
        this.httpRequestItem = PerformancePlanNodeCopies.copyHttpRequestItem(httpRequestItem);
        this.ssePerformanceData = PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
        this.webSocketPerformanceData = PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
        this.childSnapshots = Collections.unmodifiableList(new ArrayList<>(childSnapshots == null ? List.of() : childSnapshots));
        this.executionTreeNode = ThreadLocal.withInitial(this::buildTreeNode);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.REQUEST;
    }

    public HttpRequestItem getHttpRequestItem() {
        return PerformancePlanNodeCopies.copyHttpRequestItem(httpRequestItem);
    }

    public SsePerformanceData getSsePerformanceData() {
        return PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
    }

    public WebSocketPerformanceData getWebSocketPerformanceData() {
        return PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
    }

    public List<PerformancePlanElement> getChildren() {
        return children;
    }

    @Override
    public DefaultMutableTreeNode toTreeNode() {
        return buildTreeNode();
    }

    public DefaultMutableTreeNode executionTreeNode() {
        return executionTreeNode.get();
    }

    private DefaultMutableTreeNode buildTreeNode() {
        JMeterTreeNode node = new JMeterTreeNode(name, NodeType.REQUEST);
        node.httpRequestItem = PerformancePlanNodeCopies.copyHttpRequestItem(httpRequestItem);
        node.ssePerformanceData = PerformancePlanNodeCopies.copySsePerformanceData(ssePerformanceData);
        node.webSocketPerformanceData = PerformancePlanNodeCopies.copyWebSocketPerformanceData(webSocketPerformanceData);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        for (PerformancePlanNodeSnapshot snapshot : childSnapshots) {
            treeNode.add(snapshot.toTreeNode());
        }
        return treeNode;
    }
}

final class PerformancePlanNodeSnapshot {
    private final JMeterTreeNode nodeData;
    private final List<PerformancePlanNodeSnapshot> children;

    PerformancePlanNodeSnapshot(JMeterTreeNode nodeData, List<PerformancePlanNodeSnapshot> children) {
        this.nodeData = PerformancePlanNodeCopies.copyNode(nodeData);
        this.children = Collections.unmodifiableList(new ArrayList<>(children == null ? List.of() : children));
    }

    DefaultMutableTreeNode toTreeNode() {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(PerformancePlanNodeCopies.copyNode(nodeData));
        for (PerformancePlanNodeSnapshot child : children) {
            treeNode.add(child.toTreeNode());
        }
        return treeNode;
    }
}
