package com.laker.postman.panel.performance.plan;

import com.laker.postman.panel.performance.controller.LoopData;
import com.laker.postman.panel.performance.model.JMeterTreeNode;
import com.laker.postman.panel.performance.model.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PerformanceLoopController implements PerformancePlanElement {
    private final String name;
    private final LoopData loopData;
    private final List<PerformancePlanElement> elements;

    public PerformanceLoopController(String name, LoopData loopData, List<PerformancePlanElement> elements) {
        this.name = name;
        this.loopData = PerformancePlanNodeCopies.copyLoopData(loopData);
        if (this.loopData != null) {
            this.loopData.normalize();
        }
        this.elements = Collections.unmodifiableList(new ArrayList<>(elements == null ? List.of() : elements));
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public NodeType getType() {
        return NodeType.LOOP;
    }

    public LoopData getLoopData() {
        return PerformancePlanNodeCopies.copyLoopData(loopData);
    }

    public List<PerformancePlanElement> getElements() {
        return elements;
    }

    @Override
    public DefaultMutableTreeNode toTreeNode() {
        JMeterTreeNode node = new JMeterTreeNode(name, NodeType.LOOP);
        node.loopData = PerformancePlanNodeCopies.copyLoopData(loopData);
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(node);
        for (PerformancePlanElement element : elements) {
            treeNode.add(element.toTreeNode());
        }
        return treeNode;
    }
}
